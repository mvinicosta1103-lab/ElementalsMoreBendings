package com.elementals.morebendings.bending.earthsubbendings.crystal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Dono de todos os grupos de espinhos de {@link CrystalSpikeAbility} ativos
 * no servidor. Mesmo esquema exato de {@code MudSpikeManager}: lista
 * própria, dirigida por listener em {@code ServerTickEvent.Post} registrado
 * no {@code NeoForge.EVENT_BUS} (ver {@code ElementalsMoreBendingsMod}) --
 * cada posição erguida em {@link net.minecraft.world.level.block.Blocks#AMETHYST_BLOCK}
 * guarda o {@link BlockState} original de antes da erupção, pra devolver o
 * terreno exatamente como estava quando o grupo desmanchar.
 */
public final class CrystalSpikeManager {

    private static final List<SpikeGroup> ACTIVE = new ArrayList<>();

    private CrystalSpikeManager() {
    }

    /**
     * Registra um grupo de espinhos de cristal recém-erguido. {@code originalStates}
     * é o {@link BlockState} de cada posição capturado pela ability ANTES
     * de sobrescrever com {@code Blocks.AMETHYST_BLOCK} (ver {@link
     * CrystalSpikeAbility#raiseSpikes}), pra devolver o terreno original ao
     * desmanchar.
     */
    public static void registerSpikes(ServerLevel level, List<BlockPos> positions, Map<BlockPos, BlockState> originalStates) {
        ACTIVE.add(new SpikeGroup(level, positions, originalStates, CrystalSpikeAbility.RETRACT_AFTER_TICKS));
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

    /** Um grupo de espinhos de cristal em contagem regressiva pra desmanchar. */
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

        /** Volta cada posição pro estado original salvo -- ou pedra, se nenhum estado tiver sido capturado. */
        void retract() {
            boolean any = false;
            for (BlockPos pos : positions) {
                if (!level.getBlockState(pos).is(Blocks.AMETHYST_BLOCK)) {
                    continue; // o jogador pode ter minerado/trocado o bloco nesse meio tempo
                }
                BlockState restore = originalStates.getOrDefault(pos, Blocks.STONE.defaultBlockState());
                level.setBlock(pos, restore, 3);
                level.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.2, 0.15, 0.2, 0.02);
                any = true;
            }
            if (any && !positions.isEmpty()) {
                BlockPos any0 = positions.get(0);
                level.playSound(null, any0, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.7f, 1.1f);
            }
        }
    }
}