package com.elementals.morebendings.bending.watersubbendings.spirit;

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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * "curseMinion" — segunda habilidade raiz da árvore de Spirit (ver
 * {@link SpiritElement}).
 *
 * Mira (raycast via {@link SapsUtils#raycastFull}) uma criatura na direção
 * olhada; se acertar um {@link Mob} vivo, ele fica amaldiçoado por
 * {@link CurseMinionManager#CURSE_DURATION_TICKS} -- passa a atacar o
 * caster e, periodicamente, qualquer outro mob/player perto dele, trocando
 * de alvo sozinho (ver {@link CurseMinionManager}).
 *
 * Só funciona em {@link Mob}: {@link Player}s não têm IA de alvo
 * (`Mob#setTarget`), então não há como "amaldiçoar" um jogador de verdade
 * -- se o raycast acertar um jogador, a ability avisa e não faz nada.
 *
 * Instantânea, igual {@code PurifyingWaterAbility} / {@code
 * MudSurgeAbility}: OBRIGATÓRIO liberar {@code currAbility} no final.
 */
public class CurseMinionAbility implements Ability {

    private static final double RANGE = 12.0;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false,
                entity -> entity instanceof LivingEntity living && living != player && living.isAlive());

        if (!(hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult eHit)) {
            caster.displayClientMessage(Component.literal("Nenhum alvo encontrado."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!(eHit.getEntity() instanceof Mob victim)) {
            caster.displayClientMessage(Component.literal("Só é possível amaldiçoar criaturas."), true);
            bender.setCurrAbility(null);
            return;
        }

        CurseMinionManager.curse(level, caster, victim);

        level.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                victim.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, victim.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0f, 1.0f);

        bender.setCurrAbility(null); // libera a trava -- ver MudSurgeAbility para explicação completa
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}