package com.elementals.morebendings.bending.watersubbendings.healing;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * "witheringTouch" — habilidade aninhada dentro do ramo de {@code
 * healingTouch} na árvore de Healing (ver {@link HealingElement}) --
 * literalmente o oposto dela: em vez de curar, corrompe.
 * <p>
 * Alcance de TOQUE (bem mais curto que as outras habilidades de Healing,
 * ver {@link #RANGE} -- é preciso estar realmente perto do alvo, não só
 * mirando de longe). Raycast igual {@code CurseMinionAbility}, mas em vez
 * de amaldiçoar a IA, registra a vítima em {@link WitheringTouchManager},
 * que reaplica Veneno + Fome indefinidamente e desfere dano periódico até
 * matar -- SEM duração, só termina se:
 * <p>
 * - o caster tocar a MESMA vítima de novo com esta ability (reverte,
 *   ver {@link WitheringTouchManager#cure}) -- é o único jeito de
 *   reverter, como pedido; ou
 * - a vítima morrer.
 * <p>
 * Instantânea: OBRIGATÓRIO liberar {@code currAbility} no final.
 */
public class WitheringTouchAbility implements Ability {

    private static final double RANGE = 4.0; // alcance de toque, não de longa distância como as outras

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false,
                entity -> entity instanceof LivingEntity living && living != player && living.isAlive());

        if (!(hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult eHit)
                || !(eHit.getEntity() instanceof LivingEntity victim)) {
            caster.displayClientMessage(Component.literal("§7Nothing within touch range."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (WitheringTouchManager.isAfflicted(victim)) {
            cure(level, caster, victim);
        } else {
            afflict(level, caster, victim);
        }

        bender.setCurrAbility(null); // libera a trava -- ver MudSurgeAbility para explicação completa
    }

    private void afflict(ServerLevel level, ServerPlayer caster, LivingEntity victim) {
        WitheringTouchManager.afflict(level, victim);

        level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                victim.getZ(), 20, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, victim.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8f, 0.6f);

        String targetName = victim.getName().getString();
        caster.displayClientMessage(Component.literal("§4You afflicted " + targetName + " with a withering touch."), true);
        if (victim instanceof ServerPlayer targetPlayer) {
            targetPlayer.displayClientMessage(Component.literal(
                    "§4" + caster.getName().getString() + "'s touch withers you... only their touch can undo it."), true);
        }
    }

    private void cure(ServerLevel level, ServerPlayer caster, LivingEntity victim) {
        WitheringTouchManager.cure(victim);

        level.sendParticles(ParticleTypes.HEART, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                victim.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, victim.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.4f);

        String targetName = victim.getName().getString();
        caster.displayClientMessage(Component.literal("§bYou lifted the withering curse from " + targetName + "."), true);
        if (victim instanceof ServerPlayer targetPlayer) {
            targetPlayer.displayClientMessage(Component.literal(
                    "§b" + caster.getName().getString() + " lifted the curse from you."), true);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}