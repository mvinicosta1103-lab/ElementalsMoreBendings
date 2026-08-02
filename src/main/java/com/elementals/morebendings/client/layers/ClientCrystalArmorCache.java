package com.elementals.morebendings.client.layers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Espelho client-side de {@code CrystalArmorAbility}, populado só por
 * {@link com.elementals.morebendings.network.packets.SyncCrystalArmorPacket}. */
public final class ClientCrystalArmorCache {

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

    private ClientCrystalArmorCache() {
    }
}