package com.elementals.morebendings.bending.firesubbendings.plasma;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Enquanto o Plasma Boost estiver ativo (ver {@link PlasmaBoostState}),
 * qualquer golpe desferido pelo jogador -- soco, espada, qualquer ataque
 * corpo a corpo direto -- ganha um bônus de dano e acende o alvo em
 * chamas, já que as mãos estão literalmente em chamas de plasma.
 *
 * Isso é INDEPENDENTE da habilidade {@link PlasmaClawsAbility} (que já tem
 * seu próprio sistema de dano/queimada mais elaborado, com especializações
 * etc., quando conjurada de propósito). Este handler é só o efeito
 * passivo de "tá com a mão pegando fogo" -- os bônus SOMAM se o jogador
 * acertar um golpe de plasmaClaws com o boost ligado ao mesmo tempo.
 */
public final class PlasmaBoostCombatHandler {

    private static final float BOOST_BONUS_DAMAGE = 3.0f;
    private static final int BOOST_FIRE_TICKS = 80; // 4s

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        // Só conta ataque direto do próprio jogador (mão, arma) -- não
        // projéteis, não dano de fonte externa (lava, cair no fogo etc.).
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
        if (!PlasmaBoostState.isActive(attacker)) return;

        event.setAmount(event.getAmount() + BOOST_BONUS_DAMAGE);

        LivingEntity target = event.getEntity();
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), BOOST_FIRE_TICKS));
    }

    private PlasmaBoostCombatHandler() {
    }
}