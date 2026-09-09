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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * "risingTide" — segunda habilidade raiz da árvore de Healing (ver
 * {@link HealingElement}).
 *
 * Mesmo raycast de alvo de {@link HealingTouchAbility} (cai pro próprio
 * caster se nada for mirado), mas em vez de curar na hora aplica
 * Regeneração por {@link #DURATION_TICKS}, pra recuperação gradual --
 * útil antes/depois de um combate, ou pra manter alguém vivo enquanto
 * luta.
 *
 * Instantânea: OBRIGATÓRIO liberar {@code currAbility} no final.
 */
public class RisingTideAbility implements Ability {

    private static final double RANGE = 10.0;
    private static final int DURATION_TICKS = 20 * 8; // 8s
    private static final int AMPLIFIER = 1; // Regeneração II

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        LivingEntity target = resolveTarget(player);
        if (target == null) {
            caster.displayClientMessage(Component.literal("§7Nothing to bless."), true);
            bender.setCurrAbility(null);
            return;
        }

        bless(level, caster, target);
        bender.setCurrAbility(null); // libera a trava -- ver MudSurgeAbility para explicação completa
    }

    private LivingEntity resolveTarget(Player player) {
        HitResult hit = SapsUtils.raycastFull(player, RANGE, false,
                entity -> entity instanceof LivingEntity living && living.isAlive() && !(living instanceof Monster));

        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult eHit
                && eHit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return player;
    }

    private void bless(ServerLevel level, ServerPlayer caster, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, AMPLIFIER));

        level.sendParticles(ParticleTypes.DRIPPING_WATER, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 16, 0.35, 0.5, 0.35, 0.01);
        level.playSound(null, target.blockPosition(), SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.3f);

        String targetName = target == caster ? "yourself" : target.getName().getString();
        caster.displayClientMessage(Component.literal("§bYou blessed " + targetName + " with rising tide."), true);
        if (target instanceof ServerPlayer targetPlayer && targetPlayer != caster) {
            targetPlayer.displayClientMessage(Component.literal(
                    "§b" + caster.getName().getString() + " blessed you with rising tide."), true);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}