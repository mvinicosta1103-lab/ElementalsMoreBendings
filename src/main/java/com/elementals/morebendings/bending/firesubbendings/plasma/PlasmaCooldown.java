package com.elementals.morebendings.bending.firesubbendings.plasma;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cooldown de {@link PlasmaClawsAbility} por jogador, guardado só em memória
 * -- mesmo esquema do mapa {@code lastUse} de {@code GasCloudAbility}, só
 * que extraído pra classe própria porque {@code PlasmaClawsAbility} já
 * ficou grande o bastante com a lógica de dano/embutimento/Blue Fire.
 * Não persiste entre logins de propósito: um cooldown de no máximo ~1.2s
 * não faz diferença nenhuma sobrevivendo a um relog.
 */
final class PlasmaCooldown {

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    private PlasmaCooldown() {
    }

    static long getLastUse(ServerPlayer player) {
        return lastUse.getOrDefault(player.getUUID(), -100000L);
    }

    static void setLastUse(ServerPlayer player, long gameTime) {
        lastUse.put(player.getUUID(), gameTime);
    }
}