package com.elementals.morebendings.bending.earthsubbendings.sand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as crateras de areia movediça ({@link SandQuicksandState})
 * ativas no servidor — uma por caster, mesmo esquema de
 * {@link SandTornadoManager}/{@code MudTrapManager}. Dirigido pelo
 * listener de {@code ServerTickEvent.Post} registrado em
 * {@code ElementalsMoreBendingsMod}.
 *
 * Diferente de {@code SandTornadoManager} (que solta a coluna assim que o
 * caster para de agachar), aqui a cratera continua sozinha até estourar o
 * próprio tempo de duração — é uma armadilha plantada no chão, não um
 * efeito canalizado.
 */
public final class SandQuicksandManager {

    /** Raio da cratera. */
    private static final double RADIUS = 2.5;
    /** Duração total antes de desmanchar sozinha. 20 ticks = 1s. */
    private static final int DURATION_TICKS = 20 * 8; // 8s

    private static final Map<UUID, SandQuicksandState> ACTIVE = new HashMap<>();

    private SandQuicksandManager() {
    }

    public static boolean hasActivePit(ServerPlayer caster) {
        return ACTIVE.containsKey(caster.getUUID());
    }

    public static void startPit(ServerLevel level, ServerPlayer caster, Vec3 center) {
        SandQuicksandState state = new SandQuicksandState(level, caster, center, RADIUS, DURATION_TICKS);
        state.begin();
        ACTIVE.put(caster.getUUID(), state);
    }

    /** Libera a cratera do caster, se houver uma ativa, imediatamente. */
    public static void release(ServerPlayer caster) {
        SandQuicksandState state = ACTIVE.remove(caster.getUUID());
        if (state != null) {
            state.release();
        }
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, SandQuicksandState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            SandQuicksandState state = it.next().getValue();
            if (!state.tick()) {
                state.release();
                it.remove();
            }
        }
    }
}