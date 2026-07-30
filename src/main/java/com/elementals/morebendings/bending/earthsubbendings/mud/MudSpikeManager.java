package com.elementals.morebendings.bending.earthsubbendings.mud;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Dono de todos os grupos de espinhos de {@link MudSpikesAbility} ativos no
 * servidor. Cópia exata do esquema de {@code MagmaSpikeManager} (lista
 * própria, dirigida por listener em {@code ServerTickEvent.Post} registrado
 * no {@code NeoForge.EVENT_BUS} -- ver {@code ElementalsMoreBendingsMod}):
 * cada posição erguida em {@link net.minecraft.world.level.block.Blocks#MUD}
 * guarda o {@link BlockState} original de antes da erupção, pra poder
 * devolver o terreno exatamente como estava quando o grupo desmanchar.
 */
public final class MudSpikeManager {

    private static final List<SpikeGroup> ACTIVE = new ArrayList<>();

    private MudSpikeManager() {
    }

    /**
     * Registra um grupo de espinhos de lama recém-erguido. {@code originalStates}
     * é o {@link BlockState} de cada posição capturado pela ability ANTES
     * de sobrescrever com {@code Blocks.MUD} (ver {@link
     * MudSpikesAbility#raiseSpikes}), pra devolver o terreno original ao
     * desmanchar.
     */
    public static void registerSpikes(ServerLevel level, List<BlockPos> positions, Map<BlockPos, BlockState> originalStates) {
        ACTIVE.add(new SpikeGroup(level, positions, originalStates, MudSpikesAbility.RETRACT_AFTER_TICKS));
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<SpikeGroup> it = ACTIVE.iterator();
        while (it.hasNext()) {
            SpikeGroup group = it.next();
            group.ticksLeft--;
            if (group.ticksLeft <= 0) {
                group.retract();
                it.remove();
            }
        }
    }

    /** Um grupo de espinhos de lama em contagem regressiva pra desmanchar. */
    private static final class SpikeGroup {
        final ServerLevel level;
        final List<BlockPos> positions;
        final Map<BlockPos, BlockState> originalStates;
        int ticksLeft;

        SpikeGroup(ServerLevel level, List<BlockPos> positions, Map<BlockPos, BlockState> originalStates, int ticksLeft) {
            this.level = level;
            this.positions = positions;
            this.originalStates = originalStates != null ? originalStates : new HashMap<>();
            this.ticksLeft = ticksLeft;
        }

        /** Volta cada posição pro estado original salvo -- ou terra, se nenhum estado tiver sido capturado. */
        void retract() {
            for (BlockPos pos : positions) {
                if (!level.getBlockState(pos).is(Blocks.MUD)) {
                    continue; // o jogador pode ter minerado/trocado o bloco nesse meio tempo
                }
                BlockState restore = originalStates.getOrDefault(pos, Blocks.DIRT.defaultBlockState());
                level.setBlock(pos, restore, 3);
                level.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.0);
            }
        }
    }
}