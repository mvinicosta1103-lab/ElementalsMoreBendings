package com.elementals.morebendings.bending.firesubbendings.combustion;

import dev.saperate.elementals.data.ElementalConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Explosão "manual" (sem {@code Level#explode}, de propósito -- não quebra
 * bloco nenhum, igual o resto do addon evita griefar terreno em MudTrap/
 * PetrifyingTouch/etc.). Dano cai linearmente com a distância até o raio,
 * com knockback radial. Usado em três lugares:
 *
 *  1. {@link CombustionExplosionAbility} -- tiro instantâneo (sem
 *     Guidance) e explosão de impacto do {@link CombustionBoltEntity}
 *     (com Guidance).
 *  2. Backfire (autodano) quando o bender solta cedo demais ou segura
 *     foco demais -- ver {@link #selfBackfire}.
 */
final class CombustionExplosionUtils {

    private CombustionExplosionUtils() {
    }

    /**
     * Explosão num ponto do mundo. Se {@code owner} for passado, ele é
     * EXCLUÍDO do dano por padrão -- a não ser que esteja perto demais do
     * próprio centro da explosão (dentro de {@code radius + selfProximityPad}),
     * caso em que também leva o golpe (risco de "detonar perto de si
     * mesmo", parte do risco pedido pro caster).
     */
    static void explode(ServerLevel level, Vec3 center, ServerPlayer owner,
                        float damage, double radius, double selfProximityPad) {
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.3f, 0.9f + level.random.nextFloat() * 0.2f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z,
                18, radius * 0.3, radius * 0.3, radius * 0.3, 0.04);

        AABB area = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != owner)) {
            applyBlast(level, center, owner, entity, damage, radius);
        }

        if (owner != null) {
            double distToOwner = center.distanceTo(owner.position());
            if (distToOwner <= radius + selfProximityPad) {
                applyBlast(level, center, owner, owner, damage, radius);
            }
        }
    }

    /**
     * Autodano puro -- o bender detona na própria cara/mãos por ter
     * apressado ou estufado demais a concentração. Sempre atinge o caster,
     * raio pequeno (não é pensado pra pegar mais ninguém junto), e deixa
     * o efeito colateral clássico do Combustion Man: visão embaçada e
     * confusão por um instante.
     */
    static void selfBackfire(ServerLevel level, ServerPlayer caster, float damage, double radius,
                             int blindnessTicks, int confusionTicks) {
        Vec3 center = caster.position().add(0, caster.getEyeHeight() * 0.6, 0);

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9f, 1.5f);
        level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 10, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 14, 0.3, 0.3, 0.3, 0.03);

        applyBlast(level, center, caster, caster, damage, radius);

        if (blindnessTicks > 0) {
            caster.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindnessTicks, 0));
        }
        if (confusionTicks > 0) {
            caster.addEffect(new MobEffectInstance(MobEffects.CONFUSION, confusionTicks, 0));
        }
    }

    private static void applyBlast(ServerLevel level, Vec3 center, ServerPlayer owner,
                                   LivingEntity target, float damage, double radius) {
        double dist = center.distanceTo(target.position());
        double falloff = Math.max(0.0, 1.0 - (dist / (radius + 0.001)));
        float finalDamage = (float) (damage * falloff) * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER;
        if (finalDamage < 0.5f) {
            return;
        }

        DamageSource src = owner != null
                ? level.damageSources().playerAttack(owner)
                : level.damageSources().magic();
        target.hurt(src, finalDamage);

        Vec3 push = target.position().subtract(center);
        push = push.length() < 0.01 ? new Vec3(0, 1, 0) : push.normalize();
        double kb = 0.55 * falloff + 0.2;
        target.push(push.x * kb, Math.max(push.y * kb, 0.2), push.z * kb);
        target.hurtMarked = true;
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), (int) (40 * falloff)));
    }
}