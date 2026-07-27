package com.elementals.morebendings.client.layers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Espelho client-side de {@code PlasmaBoostState}, populado só por
 * {@link com.elementals.morebendings.network.packets.SyncPlasmaBoostPacket}. */
public final class ClientPlasmaBoostCache {

    private static final Set<UUID> active = new HashSet<>();

    public static void set(UUID playerId, boolean isActive) {
        if (isActive) {
            active.add(playerId);
        } else {
            active.remove(playerId);
        }
    }

    public static boolean isActive(UUID playerId) {
        return active.contains(playerId);
    }

    private ClientPlasmaBoostCache() {
    }
}