package com.elementals.morebendings.bending.firesubbendings.plasma;

import com.elementals.morebendings.network.packets.SyncPlasmaBoostPacket;
import commonnetwork.api.Dispatcher;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlasmaBoostState {

    private static final Set<UUID> active = new HashSet<>();

    public static boolean toggle(ServerPlayer player) {
        if (!PlasmaElement.isPlasmaBender(player)) {
            return false;
        }
        UUID id = player.getUUID();
        boolean nowActive = !active.contains(id);
        if (nowActive) {
            active.add(id);
        } else {
            active.remove(id);
        }

        SyncPlasmaBoostPacket packet = new SyncPlasmaBoostPacket(id, nowActive);
        for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
            Dispatcher.sendToClient(packet, online);
        }
        return nowActive;
    }

    public static boolean isActive(ServerPlayer player) {
        return active.contains(player.getUUID());
    }

    public static void deactivate(ServerPlayer player) {
        active.remove(player.getUUID());
    }

    private PlasmaBoostState() {
    }
}