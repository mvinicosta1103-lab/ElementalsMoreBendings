package com.elementals.morebendings.bending.firesubbendings.combustion;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cooldown de {@link CombustionExplosionAbility} por jogador, guardado só
 * em memória -- mesmo esquema de {@code PlasmaCooldown}. Só se aplica ao
 * tiro principal (o vent tem o próprio cooldown, separado, dentro de
 * {@link CombustionVentAbility}).
 */
final class CombustionCooldown {

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    private CombustionCooldown() {
    }

    static long getLastUse(ServerPlayer player) {
        return lastUse.getOrDefault(player.getUUID(), -100000L);
    }

    static void setLastUse(ServerPlayer player, long gameTime) {
        lastUse.put(player.getUUID(), gameTime);
    }
}