package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.entities.earth.EarthBlockEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Dono de todas as paredes de {@code mudWall} ativas -- mesmo esquema de
 * {@link com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalWallManager}:
 * sem limite de paredes simultâneas, cada cast só acumula uma entrada nova
 * na lista, com sua própria contagem regressiva independente.
 */
public final class MudWallManager {

    private static final List<WallState> ACTIVE = new ArrayList<>();

    private MudWallManager() {
    }

    /** Chamado pela ability depois de subir as entidades que formam a parede. Não faz overwrite -- só acumula. */
    public static void registerWall(Player caster, LinkedList<EarthBlockEntity> entities, int durationTicks) {
        ACTIVE.add(new WallState(entities, durationTicks));
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<WallState> it = ACTIVE.iterator();
        while (it.hasNext()) {
            WallState state = it.next();
            if (!state.tick()) {
                state.crumble();
                it.remove();
            }
        }
    }

    private static final class WallState {
        private final LinkedList<EarthBlockEntity> entities;
        private int ticksLeft;

        private WallState(LinkedList<EarthBlockEntity> entities, int durationTicks) {
            this.entities = entities;
            this.ticksLeft = durationTicks;
        }

        /** @return true enquanto a parede deve continuar de pé. */
        boolean tick() {
            ticksLeft--;
            return ticksLeft > 0;
        }

        /** Desmancha a lama com partícula + som -- nenhuma entidade vira bloco de verdade. */
        void crumble() {
            for (EarthBlockEntity entity : entities) {
                if (!entity.isAlive()) {
                    continue;
                }
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                            entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.25, 0.3, 0.25, 0.02);
                }
                entity.discard();
            }
            if (!entities.isEmpty()) {
                EarthBlockEntity any = entities.getFirst();
                any.level().playSound(null, any.blockPosition(), SoundEvents.MUD_BREAK, SoundSource.PLAYERS, 0.9f, 1.0f);
            }
        }
    }
}