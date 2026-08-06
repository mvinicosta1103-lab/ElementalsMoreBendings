package com.elementals.morebendings.bending.earthsubbendings.metal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "metalRedirect" — nó enxertado no fim do leaf {@code metalLanceRedirectI},
 * dentro do sub-ramo {@code metalLance} (que já hospeda {@link
 * MetalSlamAbility} em {@code metalLanceDamageII} -- ramo IRMÃO, não
 * conflita) da árvore REAL de Metal Bending do mod base (ver {@link
 * MetalMasteryGraft}). Nome literal do nó ("Redirect") já sugeria essa
 * mecânica antes mesmo de qualquer habilidade existir ali.
 *
 * Instantânea, mesmo esquema de {@code PetrifyingTouchAbility}: OBRIGATÓRIO
 * liberar {@code currAbility} de volta pra {@code null} em todo caminho de
 * saída de {@link #onCall} e em {@link #onRemove}.
 *
 * Agarra magneticamente toda flecha em voo dentro de {@link #RANGE} do
 * caster (não precisa ser metálica -- ferro na ponta conta) e redireciona
 * cada uma na direção da criatura viva mais próxima que não seja o caster,
 * roubando o projétil de volta contra quem atirou (ou contra qualquer outra
 * criatura por perto). Puramente defensivo/utilidade -- não causa dano
 * diretamente, só reaproveita dano que já estava a caminho.
 */
public class MetalRedirectAbility implements Ability {

    private static final double GRAB_RANGE = 10.0;
    private static final double RETARGET_RANGE = 24.0;
    private static final float CHI_COST = 18.0f;
    private static final float REDIRECT_SPEED = 2.4f;
    private static final float REDIRECT_INACCURACY = 0.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(MetalMasteryGraft.METAL_REDIRECT)) {
            bender.setCurrAbility(null);
            return;
        }

        AABB grabArea = caster.getBoundingBox().inflate(GRAB_RANGE);
        List<AbstractArrow> arrows = level.getEntitiesOfClass(AbstractArrow.class, grabArea,
                arrow -> arrow.isAlive() && arrow.getOwner() != caster);

        if (arrows.isEmpty()) {
            // Nenhuma flecha por perto -- não gasta chi à toa (mesmo padrão de MetalSlamAbility).
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        int redirected = 0;
        for (AbstractArrow arrow : arrows) {
            LivingEntity newTarget = findRetargetCandidate(level, caster, arrow);
            if (newTarget == null) {
                continue;
            }
            redirect(arrow, newTarget);
            redirected++;

            level.sendParticles(ParticleTypes.END_ROD,
                    arrow.getX(), arrow.getY(), arrow.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
        }

        if (redirected > 0) {
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 0.7f, 1.5f);
        }

        bender.setCurrAbility(null);
    }

    /**
     * @return a criatura viva mais próxima do PONTO DA FLECHA (não do
     * caster) dentro de {@link #RETARGET_RANGE}, ignorando o caster e quem
     * já era dono da flecha (evita "redirecionar" ela de volta pro próprio
     * atirador na hora). {@code null} se não houver ninguém válido.
     */
    private LivingEntity findRetargetCandidate(ServerLevel level, ServerPlayer caster, AbstractArrow arrow) {
        Entity owner = arrow.getOwner();
        AABB area = arrow.getBoundingBox().inflate(RETARGET_RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != caster && e != owner && e.isAlive());

        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distSq = candidate.position().distanceToSqr(arrow.position());
            if (distSq < closestDistSq) {
                closest = candidate;
                closestDistSq = distSq;
            }
        }
        return closest;
    }

    private void redirect(AbstractArrow arrow, LivingEntity target) {
        Vec3 aimPoint = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 toTarget = aimPoint.subtract(arrow.position());
        double dist = Math.max(toTarget.length(), 0.1);
        Vec3 direction = toTarget.scale(1.0 / dist);

        arrow.shoot(direction.x, direction.y, direction.z, REDIRECT_SPEED, REDIRECT_INACCURACY);
        arrow.setOwner(null);
        arrow.hurtMarked = true;
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}