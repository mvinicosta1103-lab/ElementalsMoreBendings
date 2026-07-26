package com.elementals.morebendings.bending.airsubbendings.atmosphere;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todos os campos de pressão ({@link PressureZoneState}) ativos no
 * servidor — um por caster. Mesmo esquema que {@code MudTrapManager} e
 * {@code SandTornadoManager} deste addon: roda de forma independente do
 * sistema de onTick do mod base, dirigido pelo listener registrado em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod} no
 * NeoForge.EVENT_BUS ({@link ServerTickEvent.Post}).
 */
public final class PressureZoneManager {

    private static final Map<UUID, PressureZoneState> ACTIVE = new HashMap<>();

    private PressureZoneManager() {
    }

    public static boolean hasActiveZone(ServerPlayer caster) {
        return ACTIVE.containsKey(caster.getUUID());
    }

    public static void startZone(ServerLevel level, ServerPlayer caster) {
        PressureZoneState state = new PressureZoneState(level, caster);
        state.begin();
        ACTIVE.put(caster.getUUID(), state);
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, PressureZoneState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            PressureZoneState state = it.next().getValue();
            if (!state.tick()) {
                it.remove();
            }
        }
    }
}