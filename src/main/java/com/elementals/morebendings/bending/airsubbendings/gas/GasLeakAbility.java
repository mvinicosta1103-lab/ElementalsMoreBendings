package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GasLeakAbility implements Ability {

    private static final int BASE_DURATION_TICKS = 200; // 10s
    private static final float RADIUS = 3.5f;
    private static final int COOLDOWN_TICKS = 140; // 7s
    private static final float CHI_COST = 6.0f;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!GasElement.hasUpgrade(caster, GasElement.GAS_LEAK)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -100000L);
        if (now - last < COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        applyLeak(caster, level);
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static void applyLeak(ServerPlayer caster, ServerLevel level) {
        int duration = getDuration(caster);

        AreaEffectCloud cloud = new AreaEffectCloud(level, caster.getX(), caster.getY(), caster.getZ());
        cloud.setOwner(caster);
        cloud.setRadius(RADIUS);
        cloud.setRadiusPerTick((0.0f - cloud.getRadius()) / (float) duration);
        cloud.setDuration(duration);
        cloud.setParticle(ParticleTypes.CLOUD);
        level.addFreshEntity(cloud);

        GasLeakManager.register(level, cloud, caster);
        spoilGround(level, caster);
    }

    private static void spoilGround(ServerLevel level, ServerPlayer caster) {
        BlockPos center = caster.blockPosition();
        int r = (int) Math.ceil(RADIUS);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }
                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
                BlockState state = level.getBlockState(ground);

                if (state.is(Blocks.FARMLAND)) {
                    BlockPos cropPos = ground.above();
                    if (!level.getBlockState(cropPos).isAir()) {
                        level.destroyBlock(cropPos, false);
                    }
                    level.setBlock(ground, Blocks.DIRT.defaultBlockState(), 3);
                } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
                    level.setBlock(ground, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }
    }

    public static int getDuration(ServerPlayer player) {
        int duration = BASE_DURATION_TICKS;
        if (GasElement.hasUpgrade(player, GasElement.GAS_LEAK_DURATION_I)) duration += 100;
        return duration;
    }
}