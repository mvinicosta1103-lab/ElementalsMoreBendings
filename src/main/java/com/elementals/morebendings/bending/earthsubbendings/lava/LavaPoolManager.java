package com.elementals.morebendings.bending.earthsubbendings.lava;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Dono de todas as poças de {@link LavaPoolAbility} ativas no servidor.
 * Roda de forma independente do sistema de onTick do mod base -- dirigido
 * pelo listener registrado em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod} no
 * NeoForge.EVENT_BUS ({@link ServerTickEvent.Post}), mesmo esquema do
 * {@code MudTrapManager}/{@code SandTornadoManager}.
 *
 * Diferente do MudTrap (que tem no máximo uma armadilha por caster e
 * precisa saber "de quem" é pra poder soltar via tecla), uma poça de lava
 * não pertence a ninguém depois de criada -- não trava nada no bender, só
 * precisa esfriar sozinha depois de um tempo. Por isso aqui é só uma lista,
 * sem chave por UUID.
 */
public final class LavaPoolManager {

    private static final List<PoolEntry> ACTIVE = new ArrayList<>();

    private LavaPoolManager() {
    }

    public static void registerPool(ServerLevel level, List<BlockPos> positions) {
        ACTIVE.add(new PoolEntry(level, positions, LavaPoolAbility.COOL_AFTER_TICKS));
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<PoolEntry> it = ACTIVE.iterator();
        while (it.hasNext()) {
            PoolEntry entry = it.next();
            entry.ticksLeft--;
            if (entry.ticksLeft <= 0) {
                entry.cool();
                it.remove();
            }
        }
    }

    /** Uma poça em contagem regressiva pra esfriar. */
    private static final class PoolEntry {
        final ServerLevel level;
        final List<BlockPos> positions;
        int ticksLeft;

        PoolEntry(ServerLevel level, List<BlockPos> positions, int ticksLeft) {
            this.level = level;
            this.positions = positions;
            this.ticksLeft = ticksLeft;
        }

        /** Vira basalto -- só se ainda for lava (o jogador pode ter mudado o bloco nesse meio tempo). */
        void cool() {
            for (BlockPos pos : positions) {
                if (level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.setBlock(pos, Blocks.BASALT.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.0);
                }
            }
        }
    }
}