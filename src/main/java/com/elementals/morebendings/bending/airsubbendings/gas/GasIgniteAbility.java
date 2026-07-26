package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Ramo de especialização "gasIgnite" (ver {@link GasElement}) — ignita a
 * nuvem de gás, colocando fogo em quem estiver dentro da área (o dobrador
 * que castou NUNCA é afetado, igual às outras especializações).
 *
 * Antes causava uma explosão de verdade (dano + possível destruição de
 * blocos); agora é fogo puro na área/alvo, sem explosão e sem quebrar nada
 * de propósito — só a queimadura em si.
 *
 * É um nó terminal (sem upgrades de melhoria ainda) e o mais caro dos três
 * (3 pontos), justamente por não precisar de investimento extra pra ser forte.
 */
public class GasIgniteAbility {

    private static final int FIRE_SECONDS = 6;
    private static final int FIRE_TICKS = FIRE_SECONDS * 20;

    public static void applyOnCloud(ServerPlayer caster, ServerLevel level, double radius) {
        if (!GasElement.hasUpgrade(caster, GasElement.GAS_IGNITE)) {
            return;
        }

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
    }

    private GasIgniteAbility() {
    }
}