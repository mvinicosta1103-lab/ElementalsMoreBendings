package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "sonicShockwave" — libera um pulso de choque sonoro em área ao redor do
 * bender: empurra e causa dano a todas as entidades vivas próximas.
 * Instantânea, mesmo padrão de {@link EchoingVoiceAbility}/{@link
 * ResonantPulseAbility} (única instância registrada em {@link
 * SoundElement}, cooldown/uso guardado num Map por UUID).
 *
 *  - sonicShockwaveRadiusI    -> +1.5 de raio
 *  - sonicShockwaveCooldownI  -> -1.5s de cooldown
 *  - sonicShockwaveDisorientI -> aplica Náusea em quem for atingido
 */
public class SonicShockwaveAbility implements Ability {

    private static final double BASE_RADIUS = 4.0;
    private static final float DAMAGE = 3.0f;
    private static final double KNOCKBACK_STRENGTH = 1.2;
    private static final int BASE_COOLDOWN_TICKS = 120; // 6s
    private static final int MIN_COOLDOWN_TICKS = 90;   // 4.5s
    private static final float CAST_CHI_COST = 4.0f;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -100000L);
        int cooldown = SoundElement.hasUpgrade(caster, SoundElement.SONIC_SHOCKWAVE_COOLDOWN_I)
                ? MIN_COOLDOWN_TICKS : BASE_COOLDOWN_TICKS;
        if (now - last < cooldown) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        double radius = SoundElement.hasUpgrade(caster, SoundElement.SONIC_SHOCKWAVE_RADIUS_I)
                ? BASE_RADIUS + 1.5 : BASE_RADIUS;
        boolean disorient = SoundElement.hasUpgrade(caster, SoundElement.SONIC_SHOCKWAVE_DISORIENT_I);

        Vec3 center = caster.position();

        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 1.0, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.5, center.z,
                (int) (20 * (radius / BASE_RADIUS)), radius * 0.4, 0.3, radius * 0.4, 0.02);
        level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.4f, 0.8f);

        AABB area = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        DamageSource soundDamage = level.damageSources().indirectMagic(caster, caster);

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(center);
            double distance = toTarget.length();
            if (distance > radius || distance <= 0.0001) {
                continue;
            }
            Vec3 push = toTarget.normalize().scale(KNOCKBACK_STRENGTH);
            target.push(push.x, Math.max(0.15, push.y * 0.3 + 0.2), push.z);
            target.hurtMarked = true;
            target.hurt(soundDamage, DAMAGE);

            if (disorient) {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            }
        }

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
