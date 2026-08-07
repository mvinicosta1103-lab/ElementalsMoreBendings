package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GasPropulsionAbility implements Ability {

    private static final int COOLDOWN_TICKS = 100; // 5s
    private static final float CHI_COST = 4.5f;
    private static final double BLAST_RADIUS = 3.0;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!GasElement.hasUpgrade(caster, GasElement.GAS_JET)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        if (now - lastUse.getOrDefault(caster.getUUID(), -100000L) < COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        // Multiplicador de Força via Upgrade
        double forceMultiplier = GasElement.hasUpgrade(caster, GasElement.GAS_JET_FORCE_I) ? 2.2 : 1.6;

        // Propulsão do Dobrador
        Vec3 look = caster.getLookAngle();
        caster.setDeltaMovement(look.x * forceMultiplier, look.y * (forceMultiplier * 0.7) + 0.2, look.z * forceMultiplier);
        caster.hurtMarked = true;

        // Efeitos Visuais
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, caster.getX(), caster.getY(), caster.getZ(), 20, 0.4, 0.4, 0.4, 0.05);

        // Repulsão AoE em inimigos no ponto de arranque
        AABB area = caster.getBoundingBox().inflate(BLAST_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != caster && e.isAlive());

        for (LivingEntity entity : nearby) {
            Vec3 knockback = entity.position().subtract(caster.position()).normalize().scale(1.0);
            entity.setDeltaMovement(knockback.x, 0.3, knockback.z);
            entity.hurtMarked = true;
        }

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}