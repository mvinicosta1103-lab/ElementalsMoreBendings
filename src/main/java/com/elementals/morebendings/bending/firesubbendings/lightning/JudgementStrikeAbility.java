package com.elementals.morebendings.bending.firesubbendings.lightning;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "judgmentStrike" — nó enxertado no fim do leaf {@code
 * lightningStormDurationI}, ramo {@code lightningStorm} da árvore REAL de
 * Lightning Bending do mod base (ver {@link LightningMasteryGraft}).
 * Diferente de {@code AbilityLightningStorm} (que derruba raios ALEATÓRIOS
 * numa área ao longo do tempo): esta é um golpe único, PRECISO e caro,
 * o "ultimate" da árvore -- a culminação temática de dominar a tempestade
 * inteira o suficiente pra comandar um único raio certeiro.
 * <br><br>
 * Instantânea, mesmo esquema de {@code PetrifyingTouchAbility}: OBRIGATÓRIO
 * liberar {@code currAbility} de volta pra {@code null} em todo caminho de
 * saída de {@link #onCall} e em {@link #onRemove}.
 * <br><br>
 * Faz um raycast na mira do caster até {@link #RANGE}; se acertar uma
 * criatura viva, um {@code LightningBolt} de verdade cai exatamente nela
 * (dano garantido, sem depender da física de queda aleatória do raio
 * vanilla). Se acertar só um bloco, o raio cai ali mesmo, ainda causando
 * dano de área a quem estiver por perto -- sempre atinge algo, nunca
 * desperdiça o cast à toa igual as outras habilidades instantâneas deste
 * addon.
 */
public class JudgementStrikeAbility implements Ability {

    private static final double RANGE = 40.0;
    private static final double SPLASH_RADIUS = 3.0;
    private static final float CHI_COST = 45.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(LightningMasteryGraft.JUDGMENT_STRIKE)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 impact = findImpact(caster, level);
        if (impact == null) {
            // Nada na mira dentro do alcance -- não gasta chi à toa (mesmo padrão de MetalShrapnelAbility).
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        strike(level, caster, impact);

        bender.setCurrAbility(null);
    }

    /**
     * @return o ponto de impacto -- a posição de uma entidade viva atingida
     * primeiro pelo raycast, ou o ponto de colisão com um bloco, dentro de
     * {@link #RANGE}. {@code null} se nada for atingido (mesmo padrão de
     * {@code MetalShrapnelAbility#findImpact}).
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
            return closestEntity.position();
        }
        if (blockHit instanceof BlockHitResult && blockHit.getType() != HitResult.Type.MISS) {
            return blockHit.getLocation();
        }
        return null;
    }

    private void strike(ServerLevel level, ServerPlayer caster, Vec3 impact) {
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(impact.x, impact.y, impact.z);
        level.addFreshEntity(bolt);

        // Dano de respaldo além do próprio LightningBolt vanilla -- garante
        // que quem estiver bem perto do impacto (não só o alvo direto)
        // também sinta o golpe, já que o raio vanilla só acerta quem
        // encostar nele fisicamente.
        AABB splash = new AABB(
                impact.x - SPLASH_RADIUS, impact.y - SPLASH_RADIUS, impact.z - SPLASH_RADIUS,
                impact.x + SPLASH_RADIUS, impact.y + SPLASH_RADIUS, impact.z + SPLASH_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, splash,
                e -> e != caster && e.isAlive());
        for (LivingEntity victim : nearby) {
            victim.hurt(level.damageSources().lightningBolt(), 4.0f);
        }

        level.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.9f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                impact.x, impact.y + 0.5, impact.z, 25, SPLASH_RADIUS * 0.4, 1.0, SPLASH_RADIUS * 0.4, 0.1);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}