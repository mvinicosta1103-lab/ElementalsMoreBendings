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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * "healingTouch" — primeira habilidade raiz da árvore de Healing (ver
 * {@link HealingElement}).
 *
 * Raycast (mesmo esquema de {@code CurseMinionAbility}) numa criatura viva
 * e não-hostil na direção mirada; restaura {@link #HEAL_AMOUNT} de vida
 * nela na hora. Se nada for mirado (ou o raycast acertar um mob hostil),
 * cura o próprio caster em vez disso -- assim a ability nunca "erra à
 * toa" e sempre faz alguma coisa útil.
 *
 * Instantânea, igual {@code PurifyingWaterAbility}/{@code CurseMinionAbility}:
 * OBRIGATÓRIO liberar {@code currAbility} no final.
 */
public class HealingTouchAbility implements Ability {

    private static final double RANGE = 10.0;
    private static final float HEAL_AMOUNT = 8.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        LivingEntity target = resolveTarget(player);
        if (target == null) {
            caster.displayClientMessage(Component.literal("§7Nothing to heal."), true);
            bender.setCurrAbility(null);
            return;
        }

        heal(level, caster, target);
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

    private void heal(ServerLevel level, ServerPlayer caster, LivingEntity target) {
        target.heal(HEAL_AMOUNT);

        level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 12, 0.3, 0.3, 0.3, 0.03);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);

        String targetName = target == caster ? "yourself" : target.getName().getString();
        caster.displayClientMessage(Component.literal("§bYou healed " + targetName + "."), true);
        if (target instanceof ServerPlayer targetPlayer && targetPlayer != caster) {
            targetPlayer.displayClientMessage(Component.literal("§b" + caster.getName().getString() + " healed you."), true);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}