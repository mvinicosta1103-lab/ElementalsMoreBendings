package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;

/**
 * Ramo de especialização "gasIgnite" (ver {@link GasElement}) — a opção de
 * maior risco/recompensa: ignita a própria nuvem de gás, causando uma
 * explosão instantânea centrada no caster. Não quebra blocos de propósito
 * (gás não é dinamite) — só dano de área, incluindo no próprio caster se ele
 * não sair de perto a tempo.
 *
 * É um nó terminal (sem upgrades de melhoria ainda) e o mais caro dos três
 * (3 pontos), justamente por não precisar de investimento extra pra ser forte.
 */
public class GasIgniteAbility {

    private static final float EXPLOSION_POWER = 2.5f;

    public static void applyOnCloud(ServerPlayer caster, ServerLevel level, double radius) {
        if (!GasElement.hasUpgrade(caster, GasElement.GAS_IGNITE)) {
            return;
        }
        level.explode(caster, null, (ExplosionDamageCalculator) null,
                caster.getX(), caster.getY() + 0.5, caster.getZ(),
                EXPLOSION_POWER, false, Level.ExplosionInteraction.TRIGGER);
    }

    private GasIgniteAbility() {
    }
}