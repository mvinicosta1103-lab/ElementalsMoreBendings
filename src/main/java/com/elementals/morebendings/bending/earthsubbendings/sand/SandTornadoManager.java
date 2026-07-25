package com.elementals.morebendings.bending.earthsubbendings.sand;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todos os tornados de areia ({@link SandTornadoState}) ativos no
 * servidor -- um por caster. Mesmo esquema que {@code MudTrapManager}
 * deste addon: roda de forma independente do sistema de onTick do mod
 * base, dirigido pelo listener registrado em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod} no
 * NeoForge.EVENT_BUS ({@link ServerTickEvent.Post}).
 */
public final class SandTornadoManager {

    private static final Map<UUID, SandTornadoState> ACTIVE = new HashMap<>();

    private SandTornadoManager() {
    }

    public static boolean hasActiveTornado(ServerPlayer caster) {
        return ACTIVE.containsKey(caster.getUUID());
    }

    public static void startTornado(ServerLevel level, ServerPlayer caster, BlockPos ground, Vec3 origin) {
        SandTornadoState state = new SandTornadoState(level, caster, ground, origin);
        state.begin();
        ACTIVE.put(caster.getUUID(), state);
    }

    /** Libera o tornado do caster, se houver um ativo (restaura os blocos sugados). */
    public static void release(ServerPlayer caster) {
        SandTornadoState state = ACTIVE.remove(caster.getUUID());
        if (state != null) {
            state.release();
        }
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, SandTornadoState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            SandTornadoState state = it.next().getValue();
            if (!state.tick()) {
                state.release();
                it.remove();
            }
        }
    }
}