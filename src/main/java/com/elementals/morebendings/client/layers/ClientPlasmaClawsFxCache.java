package com.elementals.morebendings.client.layers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Espelho client-side de "a garra de plasma desse jogador acabou de ser
 * ativada", populado só por
 * {@link com.elementals.morebendings.network.packets.PlayPlasmaClawsFxPacket}.
 *
 * Diferente de {@link ClientPlasmaBoostCache} (que é um liga/desliga
 * persistente), aqui guardamos um TIMESTAMP DE EXPIRAÇÃO por jogador: cada
 * ativação da garra "acende" o fogo por {@link #FX_DURATION_MS} e depois
 * apaga sozinho -- não precisa de pacote de "desligar" nem de estado
 * server-side pra manter sincronizado.
 */
public final class ClientPlasmaClawsFxCache {

    // Duração do "flash" de fogo nas mãos após cada ativação da garra.
    // Curto o bastante pra parecer um golpe, não um estado permanente.
    public static final long FX_DURATION_MS = 350L;

    private static final Map<UUID, Long> expiresAt = new HashMap<>();

    public static void trigger(UUID playerId) {
        expiresAt.put(playerId, System.currentTimeMillis() + FX_DURATION_MS);
    }

    public static boolean isActive(UUID playerId) {
        Long expiry = expiresAt.get(playerId);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() >= expiry) {
            expiresAt.remove(playerId);
            return false;
        }
        return true;
    }

    private ClientPlasmaClawsFxCache() {
    }
}