package com.elementals.morebendings.bending.earthsubbendings.mud;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as armadilhas de lama ({@link MudTrapState}) ativas no
 * servidor — uma por caster. Roda de forma independente do sistema de
 * onTick do mod base: é dirigida pelo listener registrado em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod} no
 * NeoForge.EVENT_BUS ({@link ServerTickEvent.Post}), então continua
 * atualizando (e sabe se libera) mesmo em cenas raras onde a Ability não
 * receberia mais onTick (ex: desconexão abrupta do caster).
 */
public final class MudTrapManager {

    private static final Map<UUID, MudTrapState> ACTIVE = new HashMap<>();

    private MudTrapManager() {
    }

    public static boolean hasActiveTrap(ServerPlayer caster) {
        return ACTIVE.containsKey(caster.getUUID());
    }

    public static void startTrap(ServerLevel level, ServerPlayer caster, LivingEntity victim) {
        MudTrapState state = new MudTrapState(level, caster, victim);
        state.begin();
        ACTIVE.put(caster.getUUID(), state);
    }

    /** Libera a armadilha do caster, se houver uma ativa (restaura terreno + solta a vítima). */
    public static void release(ServerPlayer caster) {
        MudTrapState state = ACTIVE.remove(caster.getUUID());
        if (state != null) {
            state.release();
        }
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, MudTrapState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            MudTrapState state = it.next().getValue();
            if (!state.tick()) {
                state.release();
                it.remove();
            }
        }
    }
}