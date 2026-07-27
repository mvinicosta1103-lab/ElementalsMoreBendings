package com.elementals.morebendings.bending.watersubbendings.plant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as paredes de vineWall ativas -- uma por caster, mesmo
 * esquema de {@link com.elementals.morebendings.bending.earthsubbendings.mud.MudTrapManager}.
 * Cada entrada guarda os blocos originais (pra restaurar depois) e uma
 * contagem regressiva de ticks; quando ela zera, a parede desmancha
 * sozinha.
 */
public final class PlantVineWallManager {

    private static final Map<UUID, WallState> ACTIVE = new HashMap<>();

    private PlantVineWallManager() {
    }

    public static boolean hasActiveWall(Player caster) {
        return ACTIVE.containsKey(caster.getUUID());
    }

    /**
     * Sobe uma parede de {@link Blocks#OAK_LEAVES} em cada coluna informada,
     * a partir dela pra cima ({@code height} blocos), salvando o estado
     * original de cada bloco substituído. Só substitui blocos que o
     * jogador poderia normalmente atravessar/empurrar (ar, grama alta,
     * água, etc.) -- nunca sobrescreve pedra, construções, etc.
     *
     * @return true se pelo menos um bloco foi realmente colocado.
     */
    public static boolean raise(ServerLevel level, Player caster, java.util.List<BlockPos> columns,
                                int height, int durationTicks) {
        Map<BlockPos, BlockState> saved = new HashMap<>();

        for (BlockPos column : columns) {
            for (int h = 0; h < height; h++) {
                BlockPos pos = column.above(h);
                BlockState current = level.getBlockState(pos);
                if (!current.canBeReplaced()) {
                    continue;
                }
                saved.put(pos, current);
                level.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), 3);
            }
        }

        if (saved.isEmpty()) {
            return false;
        }

        ACTIVE.put(caster.getUUID(), new WallState(level, saved, durationTicks));
        return true;
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, WallState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            WallState state = it.next().getValue();
            if (!state.tick()) {
                state.revert();
                it.remove();
            }
        }
    }

    private static final class WallState {
        private final ServerLevel level;
        private final Map<BlockPos, BlockState> saved;
        private int ticksLeft;

        private WallState(ServerLevel level, Map<BlockPos, BlockState> saved, int durationTicks) {
            this.level = level;
            this.saved = saved;
            this.ticksLeft = durationTicks;
        }

        /** @return true enquanto a parede deve continuar de pé. */
        boolean tick() {
            ticksLeft--;
            return ticksLeft > 0;
        }

        void revert() {
            for (Map.Entry<BlockPos, BlockState> entry : saved.entrySet()) {
                // só restaura se ainda for a nossa folha -- se alguém
                // quebrou/substituiu manualmente nesse meio tempo, não
                // sobrescreve de volta por cima.
                if (level.getBlockState(entry.getKey()).is(Blocks.OAK_LEAVES)) {
                    level.setBlock(entry.getKey(), entry.getValue(), 3);
                }
            }
            for (BlockPos pos : saved.keySet()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.0);
            }
            if (!saved.isEmpty()) {
                BlockPos any = saved.keySet().iterator().next();
                level.playSound(null, any, SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }
    }
}