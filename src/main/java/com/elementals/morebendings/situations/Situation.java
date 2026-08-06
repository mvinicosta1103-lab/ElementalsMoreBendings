package com.elementals.morebendings.situations;

import net.minecraft.server.level.ServerPlayer;

/**
 * Uma condição do mundo/ambiente que, se verdadeira no momento da checagem
 * periódica (ver {@link SituationsSystem}), torna o jogador elegível a
 * rolar a chance de aprender a sub-bending associada.
 * <br><br>
 * Implementações devem ser baratas o suficiente pra rodar a cada
 * {@link SituationsSystem#CHECK_INTERVAL_TICKS} ticks por jogador -- evite
 * varreduras com raio muito grande (ver {@link SituationChecks}).
 */
@FunctionalInterface
public interface Situation {
    boolean matches(ServerPlayer player);
}