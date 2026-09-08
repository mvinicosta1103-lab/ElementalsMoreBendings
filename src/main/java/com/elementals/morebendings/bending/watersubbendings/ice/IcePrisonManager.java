package com.elementals.morebendings.bending.watersubbendings.ice;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as prisões de gelo ({@link IcePrisonState}) ativas no
 * servidor — uma por caster. Mesmo esquema exato de {@code MudTrapManager}:
 * dirigido de forma independente do sistema de onTick do mod base, via
 * listener registrado em {@code ElementalsMoreBendingsMod} no
 * NeoForge.EVENT_BUS ({@link ServerTickEvent.Post}), pra continuar
 * atualizando mesmo em cenas raras onde a Ability não receberia mais
 * onTick (ex: desconexão abrupta do caster).
 */
public final class IcePrisonManager {

    private static final Map<UUID, IcePrisonState> ACTIVE = new HashMap<>();

    private IcePrisonManager() {
    }

    public static boolean hasActivePrison(ServerPlayer caster) {
        return ACTIVE.containsKey(caster.getUUID());
    }

    public static void startPrison(ServerLevel level, ServerPlayer caster, LivingEntity victim) {
        IcePrisonState state = new IcePrisonState(level, caster, victim);
        state.begin();
        ACTIVE.put(caster.getUUID(), state);
    }

    /** Libera a prisão do caster, se houver uma ativa (restaura os blocos + solta a vítima). */
    public static void release(ServerPlayer caster) {
        IcePrisonState state = ACTIVE.remove(caster.getUUID());
        if (state != null) {
            state.release();
        }
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, IcePrisonState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            IcePrisonState state = it.next().getValue();
            if (!state.tick()) {
                state.release();
                it.remove();
            }
        }
    }
}