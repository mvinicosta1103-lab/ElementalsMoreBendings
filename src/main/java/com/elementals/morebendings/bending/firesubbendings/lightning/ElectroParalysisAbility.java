package com.elementals.morebendings.bending.firesubbendings.lightning;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "electroParalysis" — nó enxertado no fim do leaf {@code
 * lightningStaticAuraStrengthII}, sub-ramo {@code lightningStaticAura}
 * DENTRO de {@code lightningVoltArc} da árvore REAL de Lightning Bending do
 * mod base (ver {@link LightningMasteryGraft}).
 * <br><br>
 * Canalizada por Shift, mesmo esquema de {@code MetalCoilAbility}: enquanto
 * segura Shift, mantém eletrocutado quem estiver mais alinhado com a mira
 * do caster dentro de {@link #RANGE} -- zera a velocidade do alvo a cada
 * tick, como um choque contínuo travando os músculos. Recalcula o alvo a
 * cada tick (não guarda estado entre casts, já que a MESMA instância de
 * {@code Ability} é compartilhada por todo bender com Lightning -- ver
 * comentário em {@link LightningMasteryGraft}), então a paralisia "solta" e
 * prende outro alvo se o caster mudar a mira. Alvos "condutivos" (ver
 * {@link LightningMasteryGraft#isConductive}) recebem dano leve por tick
 * além da imobilização; alvos secos só ficam imobilizados. Controle de
 * área -- não é dano puro, cobre o tema "imobilizar" que nenhuma
 * habilidade base de Lightning cobre.
 */
public class ElectroParalysisAbility implements Ability {

    private static final double RANGE = 14.0;
    private static final float CAST_CHI_COST = 8.0f;
    private static final float TICK_CHI_COST = 0.4f;
    private static final double AIM_TOLERANCE = 0.95;
    private static final float CONDUCTIVE_TICK_DAMAGE = 0.5f;
    private static final int PARTICLE_REFRESH_TICKS = 10;

    @Override
    public boolean activatesOnPress() {
        return true;
    }

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(LightningMasteryGraft.ELECTRO_PARALYSIS)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!player.isShiftKeyDown()) {
            caster.displayClientMessage(
                    Component.literal("Segure Shift para manter a Eletroparalisia ativa."), true);
            bender.setCurrAbility(null);
            return;
        }

        LivingEntity target = findAimedTarget(caster, level);
        if (target == null) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        shock(level, caster, target);
        bender.setCurrAbility(this); // canalizada -- solta ao soltar Shift, ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !player.isShiftKeyDown()
                || !(player.level() instanceof ServerLevel level)) {
            onRemove(bender);
            return;
        }
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }

        LivingEntity target = findAimedTarget(caster, level);
        if (target == null) {
            // Perdeu a mira -- não desativa a habilidade (pode reencontrar
            // no próximo tick), só não prende ninguém neste tick.
            return;
        }
        shock(level, caster, target);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    private LivingEntity findAimedTarget(ServerPlayer caster, ServerLevel level) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();

        AABB area = caster.getBoundingBox().inflate(RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != caster && e.isAlive());

        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 toCandidate = candidate.position().subtract(eye);
            double distSq = toCandidate.lengthSqr();
            if (distSq < 0.01) {
                continue;
            }
            Vec3 direction = toCandidate.normalize();
            if (direction.dot(look) >= AIM_TOLERANCE && distSq < bestDistSq) {
                best = candidate;
                bestDistSq = distSq;
            }
        }
        return best;
    }

    private void shock(ServerLevel level, ServerPlayer caster, LivingEntity target) {
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.fallDistance = 0.0f;

        if (LightningMasteryGraft.isConductive(target)) {
            target.hurt(level.damageSources().playerAttack(caster), CONDUCTIVE_TICK_DAMAGE);
        }

        if (caster.tickCount % PARTICLE_REFRESH_TICKS == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    6, target.getBbWidth() * 0.4, target.getBbHeight() * 0.4, target.getBbWidth() * 0.4, 0.02);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 0.3f, 1.9f);
        }
    }
}