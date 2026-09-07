package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.entities.earth.EarthBlockEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Dono de todos os abrigos de {@code mudShell} ativos -- mesmo esquema de
 * {@link com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalWallManager}/
 * {@link MudWallManager}: sem limite de abrigos simultâneos, cada cast só
 * acumula uma entrada nova na lista, com sua própria contagem regressiva
 * independente. Quando ela zera, só AQUELE abrigo desmancha -- os outros
 * continuam de pé.
 */
public final class MudShellManager {

    private static final List<ShellState> ACTIVE = new ArrayList<>();

    private MudShellManager() {
    }

    /** Chamado pela ability depois de subir as entidades que formam a casca do abrigo. Não faz overwrite -- só acumula. */
    public static void registerShell(LinkedList<EarthBlockEntity> entities, int durationTicks) {
        ACTIVE.add(new ShellState(entities, durationTicks));
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<ShellState> it = ACTIVE.iterator();
        while (it.hasNext()) {
            ShellState state = it.next();
            if (!state.tick()) {
                state.collapse();
                it.remove();
            }
        }
    }

    private static final class ShellState {
        private final LinkedList<EarthBlockEntity> entities;
        private int ticksLeft;

        private ShellState(LinkedList<EarthBlockEntity> entities, int durationTicks) {
            this.entities = entities;
            this.ticksLeft = durationTicks;
        }

        /** @return true enquanto o abrigo deve continuar de pé. */
        boolean tick() {
            ticksLeft--;
            return ticksLeft > 0;
        }

        /** Desmancha a casca inteira com partícula + som -- nenhuma entidade vira bloco de verdade. */
        void collapse() {
            for (EarthBlockEntity entity : entities) {
                if (!entity.isAlive()) {
                    continue;
                }
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                            entity.getX(), entity.getY() + 0.5, entity.getZ(), 8, 0.25, 0.3, 0.25, 0.02);
                }
                entity.discard();
            }
            if (!entities.isEmpty()) {
                EarthBlockEntity any = entities.getFirst();
                any.level().playSound(null, any.blockPosition(), SoundEvents.MUD_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
            }
        }
    }
}