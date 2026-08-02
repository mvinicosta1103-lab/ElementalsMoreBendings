package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "crystalSpike" — segunda habilidade raiz da árvore de Crystal (ver
 * {@link CrystalElement}), uma AoE ofensiva de curto alcance. Mesmo esquema
 * de {@code MudSpikesAbility}: raycast na direção mirada, e no ponto de
 * impacto um punhado de blocos "de pedra" ao redor vira
 * {@link Blocks#AMETHYST_BLOCK} de repente, arremessando quem estiver em
 * cima. Diferente da lama (que é puramente físico), o cristal também deixa
 * as farpas mais "afiadas" -- dano um pouco maior, já que aqui não tem
 * susto de volume, e sim de estilhaço.
 *
 * Os blocos revertem ao original depois de {@link #RETRACT_AFTER_TICKS} --
 * ver {@link CrystalSpikeManager}, registrado no NeoForge.EVENT_BUS em
 * {@code ElementalsMoreBendingsMod}.
 *
 * === MODELO 3D DE VERDADE (antes era só troca de textura) ===
 * Cada posição erguida agora também spawna um {@link CrystalSpikeVisualEntity}
 * em cima do bloco -- uma farpa de ametista de verdade (com animação de
 * crescer/encolher), não mais só o piso virando amethyst_block. O bloco em si
 * continua sendo trocado (mantém a luz/textura/colisão do chão), a farpa
 * visual é só "decoração" por cima, sincronizada pra sumir exatamente
 * quando {@link CrystalSpikeManager} reverte o bloco. Mesmo esquema que
 * {@code MagmaSpikeAbility} já usa pra {@code magmaSpike}.
 *
 * === FIX (espinhos só apareciam em pedra exposta) ===
 * {@link #CRYSTALLIZABLE} originalmente só tinha pedra/variantes. Em
 * qualquer terreno com grama/terra/areia por cima (a maior parte do
 * overworld), {@link #findCrystallizableGround} tinha que descer vários
 * blocos até achar pedra escondida -- e tanto o bloco cristalizado quanto
 * a farpa visual em cima dele nasciam ENTERRADOS sob a camada de terra
 * intacta, invisíveis pro jogador. Só em pedra exposta (onde a superfície
 * já era o próprio bloco cristalizável, depth 0) o efeito aparecia -- daí
 * a impressão de "só funciona em rocha". Ver o comentário em {@link
 * #CRYSTALLIZABLE} pra correção.
 */
public class CrystalSpikeAbility implements Ability {

    private static final double RANGE = 8.0;
    /** Raio da área de espinhos (blocos ao redor do centro do impacto). */
    private static final int RADIUS = 2;
    /** Fallback: quantos blocos abaixo da superfície procurar caso o bloco do topo não seja cristalizável (ex: neve, água rasa). */
    private static final int SEARCH_DEPTH = 4;
    private static final float CHI_COST = 25.0f;
    private static final float DAMAGE = 4.0f;
    private static final double LAUNCH_UP = 0.55;
    /** Bônus de crystalMastery (ver {@link CrystalElement#hasMastery}). */
    private static final int MASTERY_RADIUS_BONUS = 1;
    private static final float MASTERY_DAMAGE_BONUS = 1.5f;
    /** Quanto tempo (em ticks de servidor) os espinhos ficam de pé antes de desmanchar. 20 ticks = 1s. */
    static final int RETRACT_AFTER_TICKS = 60; // 3s

    /**
     * === FIX (espinhos só apareciam em pedra exposta) ===
     * Antes esse set só tinha pedra/variantes. Em qualquer terreno com
     * grama/terra/areia por cima (a maior parte do overworld), {@link
     * #findCrystallizableGround} tinha que descer vários blocos até achar
     * pedra escondida -- e o bloco cristalizado (e a farpa visual em cima
     * dele, ver {@link CrystalSpikeVisualEntity}) ficava ENTERRADO sob a
     * camada de terra intacta, invisível pro jogador. Só em pedra exposta
     * (onde a superfície já era o próprio bloco cristalizável, depth 0) o
     * efeito aparecia -- daí a impressão de "só funciona em rocha".
     * Ampliando o set pra cobrir os chãos comuns do overworld (terra,
     * grama, areia, cascalho, além da pedra), a superfície em si já bate
     * na maioria dos casos (depth 0), então a farpa nasce exatamente onde
     * o jogador está pisando, em qualquer bioma -- igual {@code
     * MagmaSpikeAbility} já faz aceitando qualquer bloco sólido.
     */
    private static final java.util.Set<Block> CRYSTALLIZABLE = java.util.Set.of(
            // Pedra e variantes.
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.CALCITE,
            Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.BLACKSTONE,
            // Terra e variantes.
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.PODZOL,
            Blocks.ROOTED_DIRT, Blocks.MYCELIUM, Blocks.MUD,
            // Areia e cascalho.
            Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.GRAVEL
    );

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        boolean mastery = CrystalElement.hasMastery(bender);
        int radius = RADIUS + (mastery ? MASTERY_RADIUS_BONUS : 0);
        float damage = DAMAGE + (mastery ? MASTERY_DAMAGE_BONUS : 0.0f);

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 hitPos = hit.getLocation();
        BlockPos hitColumn = BlockPos.containing(hitPos.x, hitPos.y, hitPos.z);
        BlockPos center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hitColumn).below();

        List<BlockPos> candidates = findCandidates(level, center, radius);
        if (candidates.isEmpty()) {
            // Feedback claro em vez de simplesmente não fazer nada -- e sem
            // gastar Chi, já que a habilidade não pôde ser executada.
            caster.displayClientMessage(
                    Component.literal("Não há pedra o suficiente por perto para cristalizar."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        Map<BlockPos, BlockState> original = new HashMap<>();
        applySpikes(level, candidates, original);
        damageAndLaunch(level, caster, center, radius, damage);
        CrystalSpikeManager.registerSpikes(level, candidates, original);
        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 0.9f, 0.8f);
        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.3f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    /**
     * Varre o círculo de colunas ao redor de {@code center} e devolve a
     * posição cristalizável de cada uma (ou nenhuma, se a coluna não tiver
     * pedra em até {@link #SEARCH_DEPTH} blocos de profundidade). Não
     * altera o mundo -- só encontra os candidatos, pra poder checar
     * "tem algo pra fazer?" ANTES de cobrar Chi.
     */
    private List<BlockPos> findCandidates(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> candidates = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Só os blocos dentro do círculo (não o quadrado inteiro) -- dá
                // uma forma mais de "cluster de geodo" do que uma plataforma lisa.
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }

                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = findCrystallizableGround(level, column);
                if (ground != null) {
                    candidates.add(ground);
                }
            }
        }

        return candidates;
    }

    /**
     * A partir da superfície da coluna, desce até {@link #SEARCH_DEPTH}
     * blocos procurando o primeiro bloco "cristalizável" -- pega tanto
     * pedra exposta quanto pedra escondida sob uma camada fina de
     * grama/terra (o caso mais comum no overworld).
     */
    private BlockPos findCrystallizableGround(ServerLevel level, BlockPos column) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
        for (int depth = 0; depth <= SEARCH_DEPTH; depth++) {
            BlockPos candidate = surface.below(depth);
            if (CRYSTALLIZABLE.contains(level.getBlockState(candidate).getBlock())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Substitui cada posição candidata por {@code Blocks.AMETHYST_BLOCK}.
     * O {@link BlockState} de cada uma é salvo em {@code original} ANTES da
     * troca, pra {@link CrystalSpikeManager} conseguir devolver o terreno
     * exatamente como estava depois.
     */
    private void applySpikes(ServerLevel level, List<BlockPos> positions, Map<BlockPos, BlockState> original) {
        for (BlockPos ground : positions) {
            BlockState existing = level.getBlockState(ground);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                    ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.0);
            original.put(ground.immutable(), existing);
            level.setBlock(ground, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
            // Farpa de verdade (modelo 3D, ver CrystalSpikeVisualEntity) brotando em
            // cima do bloco, não só a troca de textura -- sincronizada pra sumir
            // exatamente quando o bloco reverte (mesmo RETRACT_AFTER_TICKS que
            // CrystalSpikeManager já usa).
            CrystalSpikeVisualEntity.spawn(level, ground, RETRACT_AFTER_TICKS);
        }
    }

    /** Dano + leve arremesso pra cima em quem estiver na área no momento da erupção. */
    private void damageAndLaunch(ServerLevel level, Player caster, BlockPos center, int radius, float damage) {
        AABB area = new AABB(center).inflate(radius + 0.5, 1.5, radius + 0.5);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity entity : hit) {
            entity.hurt(level.damageSources().playerAttack(caster), damage);
            entity.push(0, LAUNCH_UP, 0);
            entity.hurtMarked = true; // garante que o cliente sincronize o impulso vertical
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}