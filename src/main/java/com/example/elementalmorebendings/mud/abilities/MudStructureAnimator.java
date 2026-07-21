package com.example.elementalmorebendings.mud.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/**
 * MudStructureAnimator
 * <p>
 * Utilitário compartilhado por {@link MudWallAbility} e
 * {@link MudShellAbility} pra animar uma estrutura de blocos de lama
 * "subindo" (construção camada por camada, de baixo pra cima) e, depois de
 * um tempo parada ({@code holdDurationTicks}), "descendo"/sumindo (também
 * camada por camada, de cima pra baixo), restaurando o {@link BlockState}
 * original de cada posição em vez de simplesmente virar ar — assim não
 * apaga blocos que já existiam ali antes do cast (ex: terra/pedra
 * dobrável que a parede cobriu).
 * <p>
 * Pensado pra ser guardado como o {@code data} de uma background ability
 * ({@code bender.addBackgroundAbility(this, animator)}) e receber um
 * {@link #tick()} a cada chamada de {@code onBackgroundTick} — o próprio
 * Bender já tica isso uma vez por tick do jogador, independente da
 * habilidade "atual" dele, então o jogador pode soltar outras habilidades
 * enquanto a estrutura ainda está subindo/descendo.
 */
public class MudStructureAnimator {

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

    public MudStructureAnimator(ServerLevel world, List<List<BlockPos>> layers,
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
            world.playSound(null, ref, SoundEvents.MUD_PLACE, SoundSource.BLOCKS,
                    0.6f, 0.9f + world.random.nextFloat() * 0.2f);
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
            world.playSound(null, ref, SoundEvents.MUD_BREAK, SoundSource.BLOCKS,
                    0.6f, 0.7f + world.random.nextFloat() * 0.2f);
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