package com.elementals.morebendings.bending.firesubbendings.plasma;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Liga/desliga do aprimoramento de dano "Plasma Boost" -- mesmo esquema do
 * conjunto {@code flying} de {@link
 * com.elementals.morebendings.bending.airsubbendings.flying.FlyingAbility}:
 * estado em memória, sem gastar Chi pra manter ativo (só existe enquanto o
 * jogador não desligar manualmente, ver {@code TogglePlasmaBoostPacket}).
 * Não persiste entre logins de propósito -- se o servidor reiniciar ou o
 * jogador desconectar, o boost cai; ele religa com a tecla de novo.
 */
public final class PlasmaBoostState {

    private static final Set<UUID> active = new HashSet<>();

    public static boolean toggle(ServerPlayer player) {
        if (!PlasmaElement.isPlasmaBender(player)) {
            return false;
        }
        UUID id = player.getUUID();
        if (active.contains(id)) {
            active.remove(id);
            return false;
        }
        active.add(id);
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        return active.contains(player.getUUID());
    }

    /** Chame no logout/morte se quiser garantir que não fique "preso" ligado. */
    public static void deactivate(ServerPlayer player) {
        active.remove(player.getUUID());
    }

    private PlasmaBoostState() {
    }
}