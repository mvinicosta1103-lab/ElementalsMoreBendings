package com.elementals.morebendings.bending.earthsubbendings.metal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "metalShrapnel" — nó enxertado no fim de {@code metalBulletDamageI}, um
 * leaf do ramo {@code metalBullet} DIFERENTE do sub-ramo {@code metalLance}
 * onde {@link MetalSlamAbility} já foi enxertada (ver {@link
 * MetalMasteryGraft}). Instantânea, mesmo esquema de
 * {@code PetrifyingTouchAbility}: OBRIGATÓRIO liberar {@code currAbility}
 * de volta pra {@code null} em todo caminho de saída de {@link #onCall} e
 * em {@link #onRemove}.
 *
 * Dispara um estilhaço de metal na mira do caster; ao atingir uma entidade
 * ou um bloco dentro de {@link #RANGE}, explode num raio curto causando
 * {@link #BASE_DAMAGE} em todos os vivos ali -- quem estiver usando
 * armadura de metal DE VERDADE (ver {@link MetalMasteryGraft#isWearingMetal})
 * leva {@link #METAL_BONUS_DAMAGE} extra (estilhaço gruda no metal e
 * concentra o dano). Ferramenta ofensiva em área, cobrindo o tema
 * "Bullet" que {@link MetalSlamAbility} não cobre (que é puxão single/AoE
 * de curto alcance, não estilhaço à distância).
 */
public class MetalShrapnelAbility implements Ability {

    private static final double RANGE = 25.0;
    private static final double BURST_RADIUS = 4.0;
    private static final float CHI_COST = 22.0f;
    private static final float BASE_DAMAGE = 4.0f;
    private static final float METAL_BONUS_DAMAGE = 3.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(MetalMasteryGraft.METAL_SHRAPNEL)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 impact = findImpact(caster, level);
        if (impact == null) {
            // Nada na mira dentro do alcance -- não gasta chi à toa.
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        burst(level, caster, impact);

        bender.setCurrAbility(null);
    }

    /**
     * @return o ponto de impacto do estilhaço -- a posição de uma entidade
     * viva atingida primeiro pelo raycast, ou o ponto de colisão com um
     * bloco, dentro de {@link #RANGE}. {@code null} se nada for atingido.
     */
    private Vec3 findImpact(ServerPlayer caster, ServerLevel level) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 reach = eye.add(look.scale(RANGE));

        HitResult blockHit = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDistSq = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceToSqr(eye)
                : RANGE * RANGE;

        LivingEntity closestEntity = null;
        double closestDistSq = maxDistSq;
        AABB searchArea = caster.getBoundingBox().inflate(RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                e -> e != caster && e.isAlive());

        for (LivingEntity candidate : candidates) {
            AABB hitBox = candidate.getBoundingBox().inflate(0.3);
            var clip = hitBox.clip(eye, reach);
            if (clip.isEmpty()) {
                continue;
            }
            double distSq = clip.get().distanceToSqr(eye);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestEntity = candidate;
            }
        }

        if (closestEntity != null) {
            return closestEntity.position().add(0, closestEntity.getBbHeight() * 0.5, 0);
        }
        if (blockHit instanceof BlockHitResult && blockHit.getType() != HitResult.Type.MISS) {
            return blockHit.getLocation();
        }
        return null;
    }

    private void burst(ServerLevel level, ServerPlayer caster, Vec3 impact) {
        AABB burstArea = new AABB(
                impact.x - BURST_RADIUS, impact.y - BURST_RADIUS, impact.z - BURST_RADIUS,
                impact.x + BURST_RADIUS, impact.y + BURST_RADIUS, impact.z + BURST_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, burstArea,
                e -> e != caster && e.isAlive());

        for (LivingEntity victim : victims) {
            float damage = BASE_DAMAGE + (MetalMasteryGraft.isWearingMetal(victim) ? METAL_BONUS_DAMAGE : 0.0f);
            victim.hurt(level.damageSources().playerAttack(caster), damage);
        }

        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 0.9f, 1.3f);
        level.sendParticles(ParticleTypes.CRIT,
                impact.x, impact.y, impact.z, 25, BURST_RADIUS * 0.4, BURST_RADIUS * 0.4, BURST_RADIUS * 0.4, 0.15);
        level.sendParticles(ParticleTypes.POOF,
                impact.x, impact.y, impact.z, 6, 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}