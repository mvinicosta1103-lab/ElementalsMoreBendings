package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GasIgniteAbility implements Ability {

    private static final int FIRE_SECONDS = 6;
    private static final int FIRE_TICKS = FIRE_SECONDS * 20;
    private static final float GROUND_FIRE_CHANCE = 0.45f;
    private static final int MAX_GROUND_HEIGHT_DIFF = 3;
    private static final double BASE_RADIUS = 3.5;
    private static final int COOLDOWN_TICKS = 160; // 8s
    private static final float CHI_COST = 7.0f;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!GasElement.hasUpgrade(caster, GasElement.GAS_IGNITE)) {
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

        applyIgnite(caster, level, BASE_RADIUS);
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static void applyIgnite(ServerPlayer caster, ServerLevel level, double radius) {
        level.sendParticles(ParticleTypes.FLAME,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                (int) (20 * (radius / 3.0)), radius * 0.4, 0.5, radius * 0.4, 0.02);
        level.sendParticles(ParticleTypes.LAVA,
                caster.getX(), caster.getY() + 0.5, caster.getZ(),
                6, radius * 0.3, 0.2, radius * 0.3, 0.0);

        AABB area = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());
        for (LivingEntity entity : nearby) {
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), FIRE_TICKS));
        }

        igniteGround(caster, level, radius);
    }

    private static void igniteGround(ServerPlayer caster, ServerLevel level, double radius) {
        BlockPos casterPos = caster.blockPosition();
        int r = (int) Math.ceil(radius);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos column = casterPos.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();

                if (Math.abs(ground.getY() - casterPos.getY()) > MAX_GROUND_HEIGHT_DIFF) {
                    continue;
                }
                if (!level.getFluidState(ground).isEmpty()) {
                    continue;
                }
                BlockPos firePos = ground.above();
                if (!level.getBlockState(firePos).isAir()) {
                    continue;
                }
                if (level.random.nextFloat() > GROUND_FIRE_CHANCE) {
                    continue;
                }
                level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }
}