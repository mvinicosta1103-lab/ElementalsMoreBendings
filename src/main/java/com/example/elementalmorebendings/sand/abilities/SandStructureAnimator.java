package com.example.elementalmorebendings.sand.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/**
 * SandStructureAnimator
 * <p>
 * Igual a {@code CrystalStructureAnimator}/{@code MudStructureAnimator}, só
 * que com sons de areia/arenito. Anima uma estrutura de blocos "crescendo"
 * camada por camada (de baixo pra cima), ficando de pé por um tempo e
 * depois "recolhendo"/sumindo do mesmo jeito, restaurando o
 * {@link BlockState} original de cada posição.
 * <p>
 * Não compartilha código com as outras cópias de propósito — mesma decisão
 * de design documentada em {@code AbilitySupport}: cada subbending tem seu
 * próprio pacote autocontido.
 */
public class SandStructureAnimator {

    public enum Phase { RISING, HOLDING, SINKING, DONE }

    private final ServerLevel world;
    private final List<List<BlockPos>> layers; // ordem de construção, de baixo pra cima
    private final Map<BlockPos, BlockState> originalStates;
    private final BlockState fillState;
    private final int ticksPerLayer;
    private final int holdDurationTicks;

    private Phase phase = Phase.RISING;
    private int layerIndex = 0;
    private int tickTimer = 0;
    private int holdTimer = 0;

    public SandStructureAnimator(ServerLevel world, List<List<BlockPos>> layers,
                                 Map<BlockPos, BlockState> originalStates,
                                 BlockState fillState, int ticksPerLayer, int holdDurationTicks) {
        this.world = world;
        this.layers = layers;
        this.originalStates = originalStates;
        this.fillState = fillState;
        this.ticksPerLayer = Math.max(1, ticksPerLayer);
        this.holdDurationTicks = Math.max(0, holdDurationTicks);
    }

    public Phase getPhase() {
        return phase;
    }

    /** @return true quando a animação terminou por completo (pode sair do background). */
    public boolean tick() {
        switch (phase) {
            case RISING -> tickRising();
            case HOLDING -> tickHolding();
            case SINKING -> tickSinking();
            default -> { }
        }
        return phase == Phase.DONE;
    }

    private void tickRising() {
        if (tickTimer++ < ticksPerLayer) {
            return;
        }
        tickTimer = 0;
        if (layerIndex >= layers.size()) {
            phase = Phase.HOLDING;
            holdTimer = 0;
            return;
        }
        placeLayer(layers.get(layerIndex));
        layerIndex++;
    }

    private void tickHolding() {
        if (++holdTimer >= holdDurationTicks) {
            phase = Phase.SINKING;
            layerIndex = layers.size() - 1;
            tickTimer = 0;
        }
    }

    private void tickSinking() {
        if (tickTimer++ < ticksPerLayer) {
            return;
        }
        tickTimer = 0;
        if (layerIndex < 0) {
            phase = Phase.DONE;
            return;
        }
        restoreLayer(layers.get(layerIndex));
        layerIndex--;
    }

    private void placeLayer(List<BlockPos> layer) {
        for (BlockPos pos : layer) {
            if (!world.isLoaded(pos)) {
                continue;
            }
            world.setBlock(pos, fillState, 3);
        }
        if (!layer.isEmpty()) {
            BlockPos ref = layer.get(layer.size() / 2);
            world.playSound(null, ref, SoundEvents.SAND_PLACE, SoundSource.BLOCKS,
                    0.7f, 0.9f + world.random.nextFloat() * 0.2f);
        }
    }

    private void restoreLayer(List<BlockPos> layer) {
        for (BlockPos pos : layer) {
            if (!world.isLoaded(pos)) {
                continue;
            }
            BlockState original = originalStates.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            world.setBlock(pos, original, 3);
        }
        if (!layer.isEmpty()) {
            BlockPos ref = layer.get(layer.size() / 2);
            world.playSound(null, ref, SoundEvents.SAND_BREAK, SoundSource.BLOCKS,
                    0.7f, 0.7f + world.random.nextFloat() * 0.2f);
        }
    }

    /** Cancela imediatamente, restaurando de uma vez tudo que já tinha sido colocado. */
    public void cancelAndRestoreAll() {
        for (List<BlockPos> layer : layers) {
            for (BlockPos pos : layer) {
                if (!world.isLoaded(pos)) {
                    continue;
                }
                BlockState original = originalStates.getOrDefault(pos, Blocks.AIR.defaultBlockState());
                world.setBlock(pos, original, 3);
            }
        }
        phase = Phase.DONE;
    }
}