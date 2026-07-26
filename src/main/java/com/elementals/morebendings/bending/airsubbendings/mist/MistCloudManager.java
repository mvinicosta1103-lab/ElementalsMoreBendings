package com.elementals.morebendings.bending.airsubbendings.mist;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as névoas ({@link MistCloudState}) ativas no servidor —
 * uma por caster. Mesmo esquema de {@code PressureZoneManager} (Atmosphere)
 * e {@code GasLeakManager} (Gas): roda de forma independente do sistema de
 * onTick do mod base, dirigido pelo listener registrado em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod} no
 * NeoForge.EVENT_BUS ({@link ServerTickEvent.Post}).
 */
public final class MistCloudManager {

    private static final Map<UUID, MistCloudState> ACTIVE = new HashMap<>();

    private MistCloudManager() {
    }

    public static boolean hasActiveZone(ServerPlayer caster) {
        return ACTIVE.containsKey(caster.getUUID());
    }

    public static void startZone(ServerLevel level, ServerPlayer caster, double radius, int durationTicks) {
        MistCloudState state = new MistCloudState(level, caster, radius, durationTicks);
        ACTIVE.put(caster.getUUID(), state);
    }

    /**
     * Chamado por {@link MistFogEntity#interact} quando alguém clica com o
     * botão direito na névoa. Só acelera a dissipação se {@code player} for
     * de fato o caster daquela zona específica -- qualquer outro jogador
     * clicando não tem efeito algum (não é uma forma de "estragar" a névoa
     * de outra pessoa).
     *
     * @return true se realmente acelerou (era o caster e a névoa ainda não
     * estava terminando mais rápido que isso de qualquer forma).
     */
    public static boolean tryAccelerateDissipation(MistFogEntity entity, ServerPlayer player) {
        MistCloudState state = ACTIVE.get(player.getUUID());
        if (state == null || state.getVisualEntity() != entity || state.getCaster() != player) {
            return false;
        }
        state.accelerate();
        return true;
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, MistCloudState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            MistCloudState state = it.next().getValue();
            if (!state.tick()) {
                it.remove();
            }
        }
    }
}