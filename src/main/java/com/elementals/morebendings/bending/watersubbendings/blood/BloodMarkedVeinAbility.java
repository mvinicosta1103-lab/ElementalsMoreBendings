package com.elementals.morebendings.bending.watersubbendings.blood;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
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
 * "bloodMarkedVein" -- nó enxertado no fim de {@code bloodShotPrecisionI},
 * leaf IRMÃ de {@code bloodShotEfficiencyII} (onde {@link
 * BloodTwinShotAbility} foi enxertada), mesmo ramo {@code bloodShot} da
 * árvore REAL de Blood (ver {@link BloodMasteryGraft}).
 * <br><br>
 * Cobre o tema "Precision" do ramo sem duplicar {@code AbilityBloodShot}
 * (que só compartilha efeitos JÁ ativos no caster): Marked Vein localiza
 * com precisão cirúrgica um vaso sanguíneo específico do alvo à distância,
 * revelando sua posição através de paredes (Brilho) e deixando os músculos
 * daquele ponto fracos (Fraqueza) por alguns segundos -- puro utilitário
 * de reconhecimento/debuff, sem gastar chi em movimento ou dano direto.
 */
public class BloodMarkedVeinAbility implements Ability {

    private static final double RANGE = 25.0;
    private static final float CHI_COST = 12.0f;
    private static final int GLOW_DURATION_TICKS = 140; // 7s
    private static final int WEAKNESS_DURATION_TICKS = 100; // 5s

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_MARKED_VEIN)) {
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

        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_DURATION_TICKS, 1, false, true, true));

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.8f);
        level.sendParticles(ParticleTypes.WITCH,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                8, 0.25, 0.35, 0.25, 0.0);

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