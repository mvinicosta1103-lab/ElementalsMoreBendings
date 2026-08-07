package com.elementals.morebendings.bending.watersubbendings.blood;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "bloodForcedGrasp" -- nó enxertado no fim de {@code bloodControlPowerI},
 * leaf IRMÃ de {@code bloodControlPrecisionII} (onde {@link
 * BloodVeinLockAbility} foi enxertada), mesmo sub-ramo exclusive de
 * {@code bloodControlPrecisionI} na árvore REAL de Blood (ver {@link
 * BloodMasteryGraft}).
 * <br><br>
 * Cobre um ângulo de "poder" sobre o corpo do alvo que nenhuma habilidade
 * base de Blood ataca: em vez de mover o corpo inteiro (como {@code
 * AbilityBloodControl}/{@code AbilityBloodPush}), Forced Grasp aperta
 * especificamente os tendões da mão do alvo -- os dedos se abrem à força e
 * o item na mão principal cai no chão. Puro desarme, sem dano, sem
 * deslocamento -- uma ferramenta tática de duelo (contra outro bender
 * empunhando arma, por exemplo) que não existe em nenhuma outra habilidade
 * de Blood.
 */
public class BloodForcedGraspAbility implements Ability {

    private static final double RANGE = 20.0;
    private static final float CHI_COST = 16.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_FORCED_GRASP)) {
            bender.setCurrAbility(null);
            return;
        }

        LivingEntity target = raycastLivingTarget(caster, level);
        if (target == null || target.getMainHandItem().isEmpty()) {
            // Sem alvo, ou alvo de mãos vazias -- não gasta chi à toa.
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(this, CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        ItemStack dropped = target.getMainHandItem().copy();
        target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        ItemEntity itemEntity = new ItemEntity(level, target.getX(), target.getY() + 0.5, target.getZ(), dropped);
        itemEntity.setDeltaMovement(
                (level.random.nextDouble() - 0.5) * 0.2,
                0.25,
                (level.random.nextDouble() - 0.5) * 0.2);
        level.addFreshEntity(itemEntity);

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.5f, 0.7f);
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                10, 0.25, 0.25, 0.25, 0.05);

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