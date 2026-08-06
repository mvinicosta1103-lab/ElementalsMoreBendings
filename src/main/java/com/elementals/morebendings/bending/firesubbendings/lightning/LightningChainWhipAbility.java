package com.elementals.morebendings.bending.firesubbendings.lightning;

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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * "lightningChainWhip" — nó enxertado no fim do leaf {@code
 * lightningBoltEfficiencyII}, sub-cadeia {@code lightningBolt} DENTRO do
 * ramo {@code lightningRedirection} da árvore REAL de Lightning Bending do
 * mod base (ver {@link LightningMasteryGraft}). Diferente de {@code
 * AbilityLightningVoltArc} (arco canalizado que o mod base já tem):
 * instantânea, mesmo esquema de {@code PetrifyingTouchAbility} --
 * OBRIGATÓRIO liberar {@code currAbility} de volta pra {@code null} em todo
 * caminho de saída de {@link #onCall} e em {@link #onRemove}.
 * <br><br>
 * Acerta o primeiro alvo na mira dentro de {@link #RANGE}, causando {@link
 * #BASE_DAMAGE}, e a partir dele o choque SALTA até {@link #MAX_JUMPS}
 * vezes para a criatura viva "condutiva" (ver {@link
 * LightningMasteryGraft#isConductive}) mais próxima ainda não atingida
 * dentro de {@link #JUMP_RANGE} -- cada salto perde {@link #DAMAGE_FALLOFF}
 * de dano. Alvos não-condutivos nunca recebem um salto (só podem ser o
 * primeiro impacto). Ofensivo em cadeia, cobre um nicho que nenhuma
 * habilidade base de Lightning cobre (Bolt é single-target reto, VoltArc é
 * canalizado).
 */
public class LightningChainWhipAbility implements Ability {

    private static final double RANGE = 20.0;
    private static final double JUMP_RANGE = 8.0;
    private static final int MAX_JUMPS = 3;
    private static final float CHI_COST = 24.0f;
    private static final float BASE_DAMAGE = 6.0f;
    private static final float DAMAGE_FALLOFF = 1.5f;
    private static final float MIN_DAMAGE = 1.5f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(LightningMasteryGraft.LIGHTNING_CHAIN_WHIP)) {
            bender.setCurrAbility(null);
            return;
        }

        LivingEntity firstTarget = findAimedTarget(caster, level);
        if (firstTarget == null) {
            // Ninguém na mira -- não gasta chi à toa (mesmo padrão de MetalShrapnelAbility).
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        chain(level, caster, firstTarget);

        bender.setCurrAbility(null);
    }

    private LivingEntity findAimedTarget(ServerPlayer caster, ServerLevel level) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 reach = eye.add(look.scale(RANGE));

        HitResult blockHit = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDistSq = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceToSqr(eye)
                : RANGE * RANGE;

        AABB area = caster.getBoundingBox().inflate(RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != caster && e.isAlive());

        LivingEntity closest = null;
        double closestDistSq = maxDistSq;
        for (LivingEntity candidate : candidates) {
            AABB hitBox = candidate.getBoundingBox().inflate(0.3);
            var clip = hitBox.clip(eye, reach);
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

    private void chain(ServerLevel level, ServerPlayer caster, LivingEntity firstTarget) {
        List<LivingEntity> hit = new ArrayList<>();
        LivingEntity current = firstTarget;
        float damage = BASE_DAMAGE;

        while (current != null) {
            zap(level, caster, current, damage);
            hit.add(current);

            if (hit.size() > MAX_JUMPS) {
                break;
            }
            damage = Math.max(MIN_DAMAGE, damage - DAMAGE_FALLOFF);
            current = findNextConductiveJump(level, caster, current, hit);
        }

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    private LivingEntity findNextConductiveJump(ServerLevel level, ServerPlayer caster,
                                                LivingEntity from, List<LivingEntity> alreadyHit) {
        AABB area = from.getBoundingBox().inflate(JUMP_RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != caster && e.isAlive() && !alreadyHit.contains(e)
                        && LightningMasteryGraft.isConductive(e));

        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distSq = candidate.position().distanceToSqr(from.position());
            if (distSq < closestDistSq) {
                closest = candidate;
                closestDistSq = distSq;
            }
        }
        return closest;
    }

    private void zap(ServerLevel level, ServerPlayer caster, LivingEntity target, float damage) {
        target.hurt(level.damageSources().playerAttack(caster), damage);

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                14, target.getBbWidth() * 0.4, target.getBbHeight() * 0.4, target.getBbWidth() * 0.4, 0.05);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4f, 1.8f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}