package com.elementals.morebendings.bending.earthsubbendings.lava;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Enquanto {@link LavaArmorAbility} estiver ativa (ver {@link
 * LavaArmorState}), qualquer golpe corpo a corpo DIRETO sofrido pelo
 * bender incendeia quem bateu -- a "casca de lava" queima o atacante de
 * volta. Mesmo esquema (registro em {@code LivingIncomingDamageEvent}) de
 * {@code PlasmaBoostCombatHandler}, só que do lado oposto: aqui o
 * jogador com a habilidade ativa é a VÍTIMA no evento, não o atacante.
 *
 * Só conta ataque direto (mão, arma) de uma {@link LivingEntity} -- não
 * projéteis, não dano de fonte ambiental (cair, fogo, lava de verdade
 * etc.), já que o efeito é retaliação de contato, não um escudo mágico de
 * área.
 */
public final class LavaArmorCombatHandler {

    private static final int RETALIATION_FIRE_SECONDS = 3;

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!LavaArmorState.isActive(victim)) return;

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        if (attacker == victim) return;

        attacker.igniteForSeconds(RETALIATION_FIRE_SECONDS);
    }

    private LavaArmorCombatHandler() {
    }
}