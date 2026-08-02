package com.elementals.morebendings.bending.earthsubbendings.crystal;

import net.minecraft.core.BlockPos;
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
 * Dono de todas as gaiolas de {@link CrystalPrisonAbility} ativas no
 * servidor. Mesmo esquema de {@link CrystalSpikeManager}: cada bloco de
 * {@link net.minecraft.world.level.block.Blocks#AMETHYST_BLOCK} erguido ao
 * redor da vítima guarda o {@link BlockState} original de antes da prisão,
 * pra devolver o terreno exatamente como estava quando a gaiola se
 * estilhaçar -- ao mesmo tempo (ou antes, se a vítima quebrar os blocos na
 * mão) que o efeito {@code STUNNED} aplicado pela ability expira sozinho.
 */
public final class CrystalPrisonManager {

    private static final List<CageGroup> ACTIVE = new ArrayList<>();

    private CrystalPrisonManager() {
    }

    /** Registra uma gaiola recém-erguida ao redor de uma vítima. */
    public static void registerCage(ServerLevel level, List<BlockPos> positions, Map<BlockPos, BlockState> originalStates, int durationTicks) {
        ACTIVE.add(new CageGroup(level, positions, originalStates, durationTicks));
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<CageGroup> it = ACTIVE.iterator();
        while (it.hasNext()) {
            CageGroup group = it.next();
            group.ticksLeft--;
            if (group.ticksLeft <= 0) {
                group.shatter();
                it.remove();
            }
        }
    }

    /** Uma gaiola de cristal em contagem regressiva pra se estilhaçar. */
    private static final class CageGroup {
        final ServerLevel level;
        final List<BlockPos> positions;
        final Map<BlockPos, BlockState> originalStates;
        int ticksLeft;

        CageGroup(ServerLevel level, List<BlockPos> positions, Map<BlockPos, BlockState> originalStates, int ticksLeft) {
            this.level = level;
            this.positions = positions;
            this.originalStates = originalStates != null ? originalStates : new HashMap<>();
            this.ticksLeft = ticksLeft;
        }

        /** Volta cada posição pro estado original salvo -- ou ar, se nenhum estado tiver sido capturado. */
        void shatter() {
            boolean any = false;
            for (BlockPos pos : positions) {
                if (!level.getBlockState(pos).is(Blocks.AMETHYST_BLOCK)) {
                    continue; // a vítima (ou outra coisa) pode ter quebrado o bloco nesse meio tempo
                }
                BlockState restore = originalStates.getOrDefault(pos, Blocks.AIR.defaultBlockState());
                level.setBlock(pos, restore, 3);
                level.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.15, 0.15, 0.15, 0.02);
                any = true;
            }
            if (any && !positions.isEmpty()) {
                level.playSound(null, positions.get(0), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.9f, 1.0f);
            }
        }
    }
}