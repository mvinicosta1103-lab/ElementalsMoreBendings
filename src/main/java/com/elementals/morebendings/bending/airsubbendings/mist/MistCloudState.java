package com.elementals.morebendings.bending.airsubbendings.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Estado de uma única névoa (Heavy Fog) ativa. Fixa no ponto de conjuração
 * (não segue o caster) — mesmo esquema de {@code PressureZoneState}.
 * Dirigida tick a tick por {@link MistCloudManager#onServerTick}.
 * <p>
 * Efeito-base (sempre ativo, independente de especialização): Cegueira +
 * Escuridão em qualquer LivingEntity dentro do raio, exceto o próprio
 * caster, reaplicadas a cada tick enquanto o alvo permanecer dentro.
 * <p>
 * As especializações (no máximo uma comprada, nó "mistSpecialization" é
 * exclusive=true) entram por cima do efeito-base:
 *  - {@link MistChokeAbility}  — dano contínuo por permanência
 *  - {@link MistFreezeAbility} — lentidão pesada extra
 *  - mistVeil/mistVeilDurationI só mudam a duração total da névoa, já
 *    resolvida na criação deste estado (ver {@link MistVeilAbility}).
 * <p>
 * Além do efeito de jogo, este estado também é dono de uma {@link
 * MistFogEntity} -- entidade puramente visual (nuvem de "golfadas"
 * translúcidas renderizadas por {@link MistFogEntityRenderer}) que nasce
 * junto com a névoa e é descartada no mesmo tick em que a zona termina.
 */
public class MistCloudState {

    private static final double HEIGHT = 3.0;
    private static final int BLINDNESS_REFRESH_TICKS = 30;
    private static final int PARTICLE_INTERVAL_TICKS = 5;

    private final ServerLevel level;
    private final ServerPlayer caster;
    private final Vec3 center;
    private final double radius;
    private final int maxDurationTicks;

    /**
     * Corpo visual da névoa (ver {@link MistFogEntity}/{@link
     * MistFogEntityRenderer}) -- entidade puramente decorativa, sincronizada
     * automaticamente pro cliente pelo próprio sistema de entidades do
     * NeoForge assim que é adicionada ao nível. Nasce junto com o estado e
     * morre junto com ele (ver {@link #tick()}); não tem lógica própria de
     * duração pra não duplicar a fonte de verdade que já mora aqui.
     */
    private final MistFogEntity visualEntity;

    private int ticksElapsed = 0;

    public MistCloudState(ServerLevel level, ServerPlayer caster, double radius, int maxDurationTicks) {
        this.level = level;
        this.caster = caster;
        this.center = caster.position();
        this.radius = radius;
        this.maxDurationTicks = maxDurationTicks;

        this.visualEntity = new MistFogEntity(level, center.x, center.y, center.z, radius);
        level.addFreshEntity(this.visualEntity);
    }

    /** @return true enquanto a névoa deve continuar ativa; false quando deve ser encerrada. */
    public boolean tick() {
        ticksElapsed++;

        if (ticksElapsed > maxDurationTicks || visualEntity.isRemoved()) {
            visualEntity.discard();
            return false;
        }

        blindTargetsInside();
        spawnAmbientParticles();
        return true;
    }

    private void blindTargetsInside() {
        AABB area = new AABB(
                center.x - radius, center.y - 1.0, center.z - radius,
                center.x + radius, center.y + HEIGHT, center.z + radius);

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive()
                        && entity.position().distanceToSqr(center) <= radius * radius);

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_REFRESH_TICKS, 0, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, BLINDNESS_REFRESH_TICKS, 0, false, false, true));

            MistChokeAbility.applyTick(level, caster, target, ticksElapsed);
            MistFreezeAbility.applyTick(caster, target);
        }
    }

    private void spawnAmbientParticles() {
        if (ticksElapsed % PARTICLE_INTERVAL_TICKS != 0) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = level.random.nextDouble() * radius;
            double px = center.x + Math.cos(angle) * dist;
            double py = center.y + level.random.nextDouble() * HEIGHT;
            double pz = center.z + Math.sin(angle) * dist;
            level.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0, 0.01, 0, 0.01);
        }
    }
}
