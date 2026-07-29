package com.elementals.morebendings.bending.airsubbendings.atmosphere;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estado de um único campo de pressão ativo. Fixo no ponto de conjuração
 * (não segue o caster) — se o caster sair do próprio raio, a zona colapsa
 * (ver {@link #tick()}). Dirigida tick a tick por
 * {@link PressureZoneManager#onServerTick}.
 * <p>
 * Sem MobEffect customizado: o "rastejar" é só Lentidão em amplificador
 * bem alto (reaplicada a cada tick, igual {@code StaticLegsAbility} faz
 * com STATIONARY/Resistência), e o dano por permanência é contado aqui
 * mesmo, por entidade, em vez de depender de um efeito próprio.
 */
public class PressureZoneState {

    private static final double HEIGHT = 3.0;

    /** Depois de quantos ticks dentro da zona o alvo começa a levar dano. */
    private static final int CRUSH_THRESHOLD_TICKS = 20 * 3; // 3s
    private static final int DAMAGE_INTERVAL_TICKS = 10; // a cada 0.5s depois do threshold
    private static final float DAMAGE_PER_HIT = 1.5f;

    private static final int SLOWNESS_AMPLIFIER = 6; // quase parado, "rastejando"
    private static final int EFFECT_REFRESH_TICKS = 10;

    private final ServerLevel level;
    private final ServerPlayer caster;
    private final Vec3 center;
    private final double radius;
    private final int maxDurationTicks;

    /** Quantos ticks seguidos cada alvo já passou dentro da zona. */
    private final Map<UUID, Integer> timeInZone = new HashMap<>();

    private int ticksElapsed = 0;

    public PressureZoneState(ServerLevel level, ServerPlayer caster, double radius, int maxDurationTicks) {
        this.level = level;
        this.caster = caster;
        this.center = caster.position();
        this.radius = radius;
        this.maxDurationTicks = maxDurationTicks;
    }

    public void begin() {
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.1, center.z, 1, 0, 0, 0, 0);
    }

    /** @return true enquanto a zona deve continuar ativa; false quando deve ser encerrada. */
    public boolean tick() {
        ticksElapsed++;

        if (!caster.isAlive() || caster.isRemoved()) {
            return false;
        }
        if (caster.position().distanceToSqr(center) > radius * radius) {
            return false; // caster saiu do próprio raio -- zona colapsa
        }
        if (ticksElapsed > maxDurationTicks) {
            return false;
        }

        crushTargetsInside();
        spawnAmbientParticles();
        return true;
    }

    private void crushTargetsInside() {
        AABB area = new AABB(
                center.x - radius, center.y - 0.5, center.z - radius,
                center.x + radius, center.y + HEIGHT, center.z + radius);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (target == caster) {
                continue;
            }

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    EFFECT_REFRESH_TICKS, SLOWNESS_AMPLIFIER, false, false, true));

            int overstay = timeInZone.merge(target.getUUID(), 1, Integer::sum);
            if (overstay >= CRUSH_THRESHOLD_TICKS && overstay % DAMAGE_INTERVAL_TICKS == 0) {
                target.hurt(level.damageSources().generic(), DAMAGE_PER_HIT);
            }
        }
    }

    private void spawnAmbientParticles() {
        if (ticksElapsed % 4 != 0) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = level.random.nextDouble() * radius;
            double px = center.x + Math.cos(angle) * dist;
            double pz = center.z + Math.sin(angle) * dist;
            level.sendParticles(ParticleTypes.CRIT, px, center.y + 0.1, pz, 1, 0, 0.01, 0, 0);
        }
    }
}