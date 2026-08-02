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
 * === FIX (a ability "não funcionava") ===
 * Duas causas, as duas nesse método:
 *  1. {@code raiseSpikes} só olhava o bloco EXATAMENTE na superfície da
 *     coluna (heightmap). Em qualquer terreno normal (grama/terra por
 *     cima de pedra, que é a maior parte do overworld) isso não batia com
 *     {@link #CRYSTALLIZABLE} e a lista de espinhos ficava vazia -- ou
 *     seja, testando em pé na grama (o caso mais comum) a ability
 *     simplesmente não fazia nada.
 *  2. O Chi era descontado ANTES de verificar se havia algo pra
 *     cristalizar, então mesmo nesse caso de falha o jogador perdia Chi
 *     sem nenhum feedback -- reforçando a sensação de "travada"/quebrada.
 * Correção: {@link #findCandidates} agora procura até {@link #SEARCH_DEPTH}
 * blocos abaixo da superfície de cada coluna (pega a pedra escondida sob
 * a camada de grama/terra) e o Chi só é descontado depois de confirmar que
 * existe pelo menos um ponto cristalizável -- com uma mensagem clara pro
 * jogador quando não há pedra nenhuma por perto.
 *
 * === MODELO 3D DE VERDADE (antes era só troca de textura) ===
 * Cada posição erguida agora também spawna um {@link CrystalSpikeVisualEntity}
 * em cima do bloco -- uma farpa de ametista de verdade (com animação de
 * crescer/encolher), não mais só o piso virando amethyst_block. O bloco em si
 * continua sendo trocado (mantém a luz/textura/colisão do chão), a farpa
 * visual é só "decoração" por cima, sincronizada pra sumir exatamente
 * quando {@link CrystalSpikeManager} reverte o bloco. Mesmo esquema que
 * {@code MagmaSpikeAbility} já usa pra {@code magmaSpike}.
 */
public class CrystalSpikeAbility implements Ability {

    private static final double RANGE = 8.0;
    /** Raio da área de espinhos (blocos ao redor do centro do impacto). */
    private static final int RADIUS = 2;
    /** Quantos blocos abaixo da superfície de cada coluna procurar por pedra escondida sob grama/terra. */
    private static final int SEARCH_DEPTH = 4;
    private static final float CHI_COST = 25.0f;
    private static final float DAMAGE = 4.0f;
    private static final double LAUNCH_UP = 0.55;
    /** Bônus de crystalMastery (ver {@link CrystalElement#hasMastery}). */
    private static final int MASTERY_RADIUS_BONUS = 1;
    private static final float MASTERY_DAMAGE_BONUS = 1.5f;
    /** Quanto tempo (em ticks de servidor) os espinhos ficam de pé antes de desmanchar. 20 ticks = 1s. */
    static final int RETRACT_AFTER_TICKS = 60; // 3s

    /** Chão "rochoso" que pode ser cristalizado -- pedra e variantes, não terra/areia. */
    private static final java.util.Set<Block> CRYSTALLIZABLE = java.util.Set.of(
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.CALCITE,
            Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.BLACKSTONE
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