package com.elementals.morebendings.bending.watersubbendings.ice;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * "iceSpike" — primeira habilidade raiz da árvore de Ice (ver {@link
 * IceElement}), uma AoE ofensiva de curto alcance. Mesmo esquema exato de
 * {@code CrystalSpikeAbility}: raycast na direção mirada, e no ponto de
 * impacto um punhado de blocos ao redor vira {@link Blocks#PACKED_ICE},
 * arremessando e CONGELANDO quem estiver em cima (em vez do dano de
 * estilhaço puro do cristal).
 *
 * Os blocos revertem ao original depois de {@link #RETRACT_AFTER_TICKS} --
 * ver {@link IceSpikeManager}, registrado no NeoForge.EVENT_BUS em
 * {@code ElementalsMoreBendingsMod}. Cada posição erguida também spawna um
 * {@link IceSpikeVisualEntity} em cima do bloco, sincronizada pra sumir
 * exatamente quando o bloco reverte.
 *
 * Aceita qualquer chão sólido comum (pedra/terra/areia/água congelável) até
 * {@link #SEARCH_DEPTH} blocos de profundidade, igual {@code
 * CrystalSpikeAbility} -- assim funciona na maioria dos biomas, não só em
 * pedra exposta.
 */
public class IceSpikeAbility implements Ability {

    private static final double RANGE = 8.0;
    /** Raio da área de espinhos (blocos ao redor do centro do impacto). */
    private static final int RADIUS = 2;
    /** Quantos blocos abaixo da superfície procurar caso o topo não seja "congelável". */
    private static final int SEARCH_DEPTH = 4;
    private static final float CHI_COST = 25.0f;
    private static final float DAMAGE = 3.5f;
    private static final double LAUNCH_UP = 0.55;
    private static final int FREEZE_DURATION_TICKS = 60; // 3s de lentidão pesada
    /** Bônus de iceMastery (ver {@link IceElement#hasMastery}). */
    private static final int MASTERY_RADIUS_BONUS = 1;
    private static final float MASTERY_DAMAGE_BONUS = 1.0f;
    /** Quanto tempo (em ticks de servidor) os espinhos ficam de pé antes de desmanchar. 20 ticks = 1s. */
    static final int RETRACT_AFTER_TICKS = 60; // 3s

    /** Terrenos comuns do overworld em que os espinhos conseguem brotar -- mesmo set de {@code CrystalSpikeAbility}. */
    private static final java.util.Set<Block> FREEZABLE = java.util.Set.of(
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.CALCITE,
            Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.BLACKSTONE,
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.PODZOL,
            Blocks.ROOTED_DIRT, Blocks.MYCELIUM, Blocks.MUD,
            Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.GRAVEL,
            Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.PACKED_ICE, Blocks.SNOW
    );

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        boolean mastery = IceElement.hasMastery(bender);
        int radius = RADIUS + (mastery ? MASTERY_RADIUS_BONUS : 0);
        float damage = DAMAGE + (mastery ? MASTERY_DAMAGE_BONUS : 0.0f);

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 hitPos = hit.getLocation();
        BlockPos hitColumn = BlockPos.containing(hitPos.x, hitPos.y, hitPos.z);
        BlockPos center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hitColumn).below();

        List<BlockPos> candidates = findCandidates(level, center, radius);
        if (candidates.isEmpty()) {
            caster.displayClientMessage(
                    Component.literal("Não há chão o suficiente por perto para congelar."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        Map<BlockPos, BlockState> original = new HashMap<>();
        applySpikes(level, candidates, original);
        damageAndFreeze(level, caster, center, radius, damage);
        IceSpikeManager.registerSpikes(level, candidates, original);
        level.playSound(null, center, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.9f, 1.4f);
        level.playSound(null, center, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.5f, 0.9f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    private List<BlockPos> findCandidates(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> candidates = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }

                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = findFreezableGround(level, column);
                if (ground != null) {
                    candidates.add(ground);
                }
            }
        }

        return candidates;
    }

    private BlockPos findFreezableGround(ServerLevel level, BlockPos column) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
        for (int depth = 0; depth <= SEARCH_DEPTH; depth++) {
            BlockPos candidate = surface.below(depth);
            if (FREEZABLE.contains(level.getBlockState(candidate).getBlock())) {
                return candidate;
            }
        }
        return null;
    }

    private void applySpikes(ServerLevel level, List<BlockPos> positions, Map<BlockPos, BlockState> original) {
        for (BlockPos ground : positions) {
            BlockState existing = level.getBlockState(ground);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                    ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.0);
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
            original.put(ground.immutable(), existing);
            level.setBlock(ground, Blocks.PACKED_ICE.defaultBlockState(), 3);
            IceSpikeVisualEntity.spawn(level, ground, RETRACT_AFTER_TICKS);
        }
    }

    /** Dano + arremesso + congelamento (lentidão pesada) em quem estiver na área no momento da erupção. */
    private void damageAndFreeze(ServerLevel level, Player caster, BlockPos center, int radius, float damage) {
        AABB area = new AABB(center).inflate(radius + 0.5, 1.5, radius + 0.5);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity entity : hit) {
            entity.hurt(level.damageSources().freeze(), damage);
            entity.push(0, LAUNCH_UP, 0);
            entity.hurtMarked = true; // garante que o cliente sincronize o impulso vertical
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_DURATION_TICKS, 3));
            entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze() / 2));
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}