package com.elementals.morebendings.bending.earthsubbendings.lava;

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
 * Dono de todas as crateras de {@link VolcanicEruptionAbility} ativas no
 * servidor. Mesmo esquema de {@link LavaPoolManager}/{@link
 * MagmaSpikeManager} (listas próprias, dirigidas por um listener em
 * {@code ServerTickEvent.Post} registrado no {@code NeoForge.EVENT_BUS} --
 * ver {@code ElementalsMoreBendingsMod}), mas precisa gerenciar DOIS
 * grupos independentes por erupção -- o núcleo de lava de verdade (esfria
 * pra basalto, igual {@link LavaPoolManager}) e o anel de espinhos de
 * magma ao redor (volta pro bloco original, igual {@link
 * MagmaSpikeManager}) -- porque têm durações e comportamentos de reversão
 * diferentes. Por isso duas listas (e duas classes de entrada) em vez de
 * reaproveitar os managers dos irmãos diretamente.
 */
public final class VolcanicEruptionManager {

    private static final List<CoreEntry> ACTIVE_CORES = new ArrayList<>();
    private static final List<SpikeGroup> ACTIVE_SPIKES = new ArrayList<>();

    private VolcanicEruptionManager() {
    }

    public static void registerCore(ServerLevel level, List<BlockPos> positions) {
        ACTIVE_CORES.add(new CoreEntry(level, positions, VolcanicEruptionAbility.CORE_COOL_AFTER_TICKS));
    }

    public static void registerSpikes(ServerLevel level, List<BlockPos> positions, Map<BlockPos, BlockState> originalStates) {
        ACTIVE_SPIKES.add(new SpikeGroup(level, positions, originalStates, VolcanicEruptionAbility.SPIKE_RETRACT_AFTER_TICKS));
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ACTIVE_CORES.isEmpty()) {
            Iterator<CoreEntry> coreIt = ACTIVE_CORES.iterator();
            while (coreIt.hasNext()) {
                CoreEntry entry = coreIt.next();
                entry.ticksLeft--;
                if (entry.ticksLeft <= 0) {
                    entry.cool();
                    coreIt.remove();
                }
            }
        }

        if (!ACTIVE_SPIKES.isEmpty()) {
            Iterator<SpikeGroup> spikeIt = ACTIVE_SPIKES.iterator();
            while (spikeIt.hasNext()) {
                SpikeGroup group = spikeIt.next();
                group.ticksLeft--;
                if (group.ticksLeft <= 0) {
                    group.retract();
                    spikeIt.remove();
                }
            }
        }
    }

    /** O núcleo de lava de uma erupção, em contagem regressiva pra esfriar pra basalto. */
    private static final class CoreEntry {
        final ServerLevel level;
        final List<BlockPos> positions;
        int ticksLeft;

        CoreEntry(ServerLevel level, List<BlockPos> positions, int ticksLeft) {
            this.level = level;
            this.positions = positions;
            this.ticksLeft = ticksLeft;
        }

        void cool() {
            for (BlockPos pos : positions) {
                if (level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.setBlock(pos, Blocks.BASALT.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.25, 0.15, 0.25, 0.0);
                }
            }
        }
    }

    /** O anel de espinhos de magma de uma erupção, em contagem regressiva pra desmanchar. */
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

        void retract() {
            for (BlockPos pos : positions) {
                if (!level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) {
                    continue; // o jogador pode ter minerado/trocado o bloco nesse meio tempo
                }
                BlockState restore = originalStates.getOrDefault(pos, Blocks.STONE.defaultBlockState());
                level.setBlock(pos, restore, 3);
                level.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.0);
            }
        }
    }
}