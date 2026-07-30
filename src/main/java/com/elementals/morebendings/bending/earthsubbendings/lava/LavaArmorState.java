package com.elementals.morebendings.bending.earthsubbendings.lava;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 * Mais simples que {@code PlasmaBoostState} (Plasma Boost) de propósito:
 * aquele precisa sincronizar pro cliente porque tem uma camada de
 * renderização própria (mãos pegando fogo). Lava Armor não tem nenhum
 * visual extra além dos efeitos de status vanilla (Resistência a Fogo /
 * Resistência), então um {@code Set<UUID>} só-servidor já basta.
 */
public final class LavaArmorState {

    private static final Set<UUID> active = new HashSet<>();

    public static void activate(ServerPlayer player) {
        active.add(player.getUUID());
    }

    public static void deactivate(ServerPlayer player) {
        active.remove(player.getUUID());
    }

    public static boolean isActive(ServerPlayer player) {
        return active.contains(player.getUUID());
    }

    private LavaArmorState() {
    }
}