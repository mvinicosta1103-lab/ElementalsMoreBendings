package com.elementals.morebendings.bending.avatarstate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Raycast compartilhado por {@link AvatarBendingGrantAbility} e
 * {@link AvatarBendingRemoveAbility} -- acha o {@code ServerPlayer} que o
 * caster está mirando (mesma técnica de {@code BloodPickupAbility}, só que
 * restrita a jogadores: dobra, no sistema base do mod, só existe em cima
 * de {@code ServerPlayer} -- {@code Bender.getBender} não aceita outro
 * tipo de entidade, então mobs/bots nunca podem receber ou perder dobra
 * por aqui).
 */
final class AvatarBendingTargeting {

    static final double RANGE = 20.0;

    private AvatarBendingTargeting() {
    }

    static ServerPlayer raycastPlayerTarget(ServerPlayer caster, ServerLevel level) {
        Vec3 eye = caster.getEyePosition();
        Vec3 reach = eye.add(caster.getLookAngle().scale(RANGE));

        HitResult blockHit = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDistSq = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceToSqr(eye)
                : RANGE * RANGE;

        ServerPlayer closest = null;
        double closestDistSq = maxDistSq;
        for (ServerPlayer candidate : level.getEntitiesOfClass(ServerPlayer.class,
                caster.getBoundingBox().inflate(RANGE), p -> p != caster && p.isAlive())) {
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
}