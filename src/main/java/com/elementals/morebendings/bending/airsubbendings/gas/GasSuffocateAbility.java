package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GasSuffocateAbility implements Ability {

    private static final float BASE_DAMAGE = 2.0f;
    private static final int COOLDOWN_TICKS = 120; // 6s
    private static final float CHI_COST = 5.0f;
    private static final double BASE_RADIUS = 3.5;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!GasElement.hasUpgrade(caster, GasElement.GAS_SUFFOCATE)) {
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

        applySuffocate(caster, level, BASE_RADIUS);
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static void applySuffocate(ServerPlayer caster, ServerLevel level, double radius) {
        float damage = getDamage(caster);
        DamageSource source = level.damageSources().indirectMagic(caster, caster);

        level.sendParticles(ParticleTypes.SQUID_INK,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                20, radius * 0.4, 0.5, radius * 0.4, 0.02);

        AABB area = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());
        for (LivingEntity entity : nearby) {
            entity.hurt(source, damage);
        }
    }

    public static float getDamage(ServerPlayer player) {
        float damage = BASE_DAMAGE;
        if (GasElement.hasUpgrade(player, GasElement.GAS_SUFFOCATE_DAMAGE_I)) damage += 1.5f;
        if (GasElement.hasUpgrade(player, GasElement.GAS_SUFFOCATE_DAMAGE_II)) damage += 1.5f;
        return damage;
    }
}