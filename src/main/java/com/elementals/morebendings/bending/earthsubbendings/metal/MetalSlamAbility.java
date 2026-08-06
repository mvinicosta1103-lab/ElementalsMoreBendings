package com.elementals.morebendings.bending.earthsubbendings.metal;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "metalSlam" — filho da árvore de {@link MetalElement} (Metal Mastery).
 * Instantânea, mesmo esquema de {@code PetrifyingTouchAbility}: OBRIGATÓRIO
 * liberar {@code currAbility} de volta pra {@code null} em todo caminho de
 * saída de {@link #onCall} e em {@link #onRemove}.
 *
 * Comportamento é o já descrito em en_us.json (chave
 * upgrade.elementals.metalSlam.description): puxa toda criatura viva
 * dentro de {@link #RANGE} blocos que esteja usando armadura de metal DE
 * VERDADE (ver {@link MetalElement#isWearingMetal}) na direção do caster,
 * causa {@link #DAMAGE} de dano e aplica Lentidão breve no impacto -- quem
 * não estiver de metal fica completamente intocado. Combina com {@link
 * MetalSenseAbility}, que revela quem tem metal antes do puxão.
 */
public class MetalSlamAbility implements Ability {

    private static final double RANGE = 12.0;
    private static final float CHI_COST = 25.0f;
    private static final float DAMAGE = 3.0f;
    private static final double PULL_STRENGTH = 1.3;
    private static final double PULL_UPWARD = 0.15;
    private static final int SLOW_DURATION_TICKS = 60; // 3s
    private static final int SLOW_AMPLIFIER = 1;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        AABB area = caster.getBoundingBox().inflate(RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != caster && e.isAlive() && MetalElement.isWearingMetal(e));

        if (targets.isEmpty()) {
            // Ninguém de metal por perto -- não gasta chi à toa.
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        for (LivingEntity target : targets) {
            pull(caster, target);
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SLOW_DURATION_TICKS, SLOW_AMPLIFIER));

            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.02);
        }

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.8f, 0.7f);

        bender.setCurrAbility(null);
    }

    private void pull(ServerPlayer caster, LivingEntity target) {
        Vec3 toCaster = caster.position().subtract(target.position());
        double horizontalDist = Math.max(toCaster.horizontalDistance(), 0.1);
        Vec3 pull = new Vec3(toCaster.x / horizontalDist, PULL_UPWARD, toCaster.z / horizontalDist)
                .scale(PULL_STRENGTH);

        target.setDeltaMovement(pull);
        target.hurtMarked = true; // força sincronizar a nova velocidade pro cliente
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}