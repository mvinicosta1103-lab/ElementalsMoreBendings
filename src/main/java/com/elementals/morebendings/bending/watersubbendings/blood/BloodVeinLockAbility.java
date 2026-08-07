package com.elementals.morebendings.bending.watersubbendings.blood;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.blood.BloodElement;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "bloodVeinLock" -- nó enxertado no fim de {@code bloodControlPrecisionII},
 * leaf do sub-ramo exclusive {@code bloodControlPrecisionI} da árvore REAL
 * de Blood Bending do mod base (ver {@link BloodMasteryGraft}).
 * <br><br>
 * Diferente de {@code AbilityBloodControl} (que TOMA o corpo inteiro do
 * alvo e o move fisicamente, canalizada, alto custo de chi) -- Vein Lock é
 * instantânea e mais sutil: em vez de mover o alvo, o bender aperta a
 * circulação nas pernas e braços dele à distância, aplicando Lentidão e
 * Fadiga de Mineração por alguns segundos. Não desloca ninguém, não causa
 * dano -- é controle de MOBILIDADE/COMBATE, não de posição, cobrindo um
 * uso que nem {@code AbilityBloodControl} nem {@code AbilityBloodPush}
 * oferecem (imobilizar sem gastar 25 de chi por segundo canalizando).
 * <br><br>
 * Bônus temático de noite/lua cheia (mesmo padrão de
 * {@code AbilityBloodControl}/{@code AbilityBlood4}, via
 * {@link BloodElement#isNight}): à noite, o efeito dura mais tempo e vem
 * com um amplificador extra -- o sangue "obedece" melhor no escuro.
 */
public class BloodVeinLockAbility implements Ability {

    private static final double RANGE = 20.0;
    private static final float CHI_COST = 18.0f;
    private static final int BASE_DURATION_TICKS = 100; // 5s
    private static final int NIGHT_BONUS_DURATION_TICKS = 60; // +3s à noite

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_VEIN_LOCK)) {
            bender.setCurrAbility(null);
            return;
        }

        LivingEntity target = raycastLivingTarget(caster, level);
        if (target == null) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(this, CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        boolean night = BloodElement.isNight(level);
        int duration = BASE_DURATION_TICKS + (night ? NIGHT_BONUS_DURATION_TICKS : 0);
        int amplifier = night ? 3 : 2; // Lentidão IV à noite, III de dia

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 1, false, true, true));

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.6f, 0.6f);
        level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(0x8B0000).toVector3f(), 1.0f),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                14, 0.3, 0.4, 0.3, 0.02);

        bender.setCurrAbility(null);
    }

    private LivingEntity raycastLivingTarget(ServerPlayer caster, ServerLevel level) {
        Vec3 eye = caster.getEyePosition();
        Vec3 reach = eye.add(caster.getLookAngle().scale(RANGE));

        HitResult blockHit = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDistSq = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceToSqr(eye)
                : RANGE * RANGE;

        LivingEntity closest = null;
        double closestDistSq = maxDistSq;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(RANGE), e -> e != caster && e.isAlive())) {
            var clip = candidate.getBoundingBox().inflate(0.3).clip(eye, reach);
            if (clip.isEmpty()) {
                continue;
            }
            double distSq = clip.get().distanceToSqr(eye);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = candidate;
            }
        }
        return closest;
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}