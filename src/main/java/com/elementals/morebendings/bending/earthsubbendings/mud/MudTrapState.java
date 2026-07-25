package com.elementals.morebendings.bending.earthsubbendings.mud;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado de uma única armadilha de lama ativa (um caster prende uma vítima
 * por vez). Dirigida tick a tick por {@link MudTrapManager#onServerTick}.
 *
 * Duas fases:
 *
 *  1. SINKING — a vítima é baixada suavemente por 1 bloco inteiro ao longo
 *     de {@link #SINK_TICKS} ticks. O bloco do chão logo abaixo dela é
 *     quebrado ({@link ServerLevel#destroyBlock}) pra abrir espaço — com
 *     partícula e som de barro quebrando.
 *
 *  2. BURIED — assim que termina de afundar, os dois blocos que a vítima
 *     agora ocupa (o buraco onde ela caiu + o espaço onde ela estava de pé)
 *     são reconstruídos como Barro sólido ({@link Blocks#MUD}). A vítima
 *     fica literalmente dentro do bloco — o próprio jogo já aplica dano de
 *     sufocamento sozinho pra quem está "dentro de bloco" (mecânica vanilla
 *     de {@code isInWall}, automática, não precisamos fazer nada extra pra
 *     isso). A gente só mantém ela travada no lugar tick a tick pra não
 *     escapar antes da hora.
 *
 * Libera (restaura o terreno original e solta a vítima) quando: o caster
 * para de agachar, a vítima morre/some, o caster desconecta/morre, ou
 * depois de {@link #MAX_DURATION_TICKS} por segurança (failsafe caso algo
 * dê errado e o release nunca seja chamado por outro caminho).
 */
public class MudTrapState {

    private static final int SINK_TICKS = 12; // ~0.6s pra afundar
    private static final int MAX_DURATION_TICKS = 20 * 30; // trava de segurança: 30s

    private final ServerLevel level;
    private final ServerPlayer caster;
    private final LivingEntity victim;

    private final BlockPos feetPos;   // bloco onde a vítima estava de pé
    private final BlockPos groundPos; // chão logo abaixo (feetPos.below())
    private final Map<BlockPos, BlockState> savedStates = new LinkedHashMap<>();

    private final double startX;
    private final double startY;
    private final double startZ;
    private final double targetY; // startY - 1 (afunda exatamente 1 bloco)

    private int ticksElapsed = 0;
    private boolean buried = false;

    public MudTrapState(ServerLevel level, ServerPlayer caster, LivingEntity victim) {
        this.level = level;
        this.caster = caster;
        this.victim = victim;

        this.feetPos = victim.blockPosition();
        this.groundPos = feetPos.below();

        this.startX = victim.getX();
        this.startY = victim.getY();
        this.startZ = victim.getZ();
        this.targetY = startY - 1.0;
    }

    /** Chamada uma vez, ao criar a armadilha — quebra o chão e inicia o afundamento. */
    public void begin() {
        savedStates.put(groundPos, level.getBlockState(groundPos));
        savedStates.put(feetPos, level.getBlockState(feetPos));

        level.destroyBlock(groundPos, false, victim, 512);
        level.playSound(null, feetPos, SoundEvents.MUD_BREAK, SoundSource.PLAYERS, 0.8f, 0.9f);
    }

    /** @return true enquanto a armadilha deve continuar ativa; false quando deve ser liberada. */
    public boolean tick() {
        ticksElapsed++;

        if (!caster.isAlive() || caster.isRemoved() || !victim.isAlive() || victim.isRemoved()) {
            return false;
        }
        if (!caster.isShiftKeyDown()) {
            return false;
        }
        if (ticksElapsed > MAX_DURATION_TICKS) {
            return false;
        }

        if (!buried) {
            sinkStep();
        } else {
            holdStep();
        }
        return true;
    }

    private void sinkStep() {
        int sinkTick = Math.min(ticksElapsed, SINK_TICKS);
        double progress = sinkTick / (double) SINK_TICKS;
        double y = startY + (targetY - startY) * progress;

        victim.moveTo(startX, y, startZ, victim.getYRot(), victim.getXRot());
        victim.setDeltaMovement(Vec3.ZERO);

        if (ticksElapsed % 3 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.MUD.defaultBlockState()),
                    startX, y + 0.5, startZ, 6, 0.25, 0.25, 0.25, 0.02);
        }

        if (sinkTick >= SINK_TICKS) {
            bury();
        }
    }

    private void bury() {
        level.setBlock(groundPos, Blocks.MUD.defaultBlockState(), 3);
        level.setBlock(feetPos, Blocks.MUD.defaultBlockState(), 3);
        level.playSound(null, feetPos, SoundEvents.MUD_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);
        buried = true;
    }

    private void holdStep() {
        // Mantém a vítima travada no fundo do buraco. Mesmo sufocando, uma
        // LivingEntity ainda processa AI/movimento, então sem isso ela iria
        // aos poucos se debater pra fora do bloco sólido.
        victim.moveTo(startX, targetY, startZ, victim.getYRot(), victim.getXRot());
        victim.setDeltaMovement(Vec3.ZERO);
    }

    /** Restaura o terreno original e solta a vítima. Chamada uma vez, ao final. */
    public void release() {
        for (Map.Entry<BlockPos, BlockState> entry : savedStates.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        if (victim.isAlive() && !victim.isRemoved()) {
            victim.moveTo(startX, startY, startZ, victim.getYRot(), victim.getXRot());
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.MUD.defaultBlockState()),
                    startX, startY + 0.5, startZ, 14, 0.3, 0.4, 0.3, 0.04);
            level.playSound(null, feetPos, SoundEvents.MUD_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }
}