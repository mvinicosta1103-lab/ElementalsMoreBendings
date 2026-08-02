package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.entities.earth.EarthBlockEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as paredes de crystalWall ativas -- uma por caster, mesmo
 * esquema exato de {@link com.elementals.morebendings.bending.watersubbendings.plant.PlantVineWallManager}.
 * Cada entrada guarda as entidades {@link EarthBlockEntity} flutuantes que
 * formam a parede (ver {@link CrystalWallAbility}) e uma contagem
 * regressiva de ticks; quando ela zera, a parede se estilhaça sozinha.
 */
public final class CrystalWallManager {

    private static final Map<UUID, WallState> ACTIVE = new HashMap<>();

    private CrystalWallManager() {
    }

    public static boolean hasActiveWall(Player caster) {
        return ACTIVE.containsKey(caster.getUUID());
    }

    /** Chamado pela ability depois de subir as entidades que formam a parede. */
    public static void registerWall(Player caster, LinkedList<EarthBlockEntity> entities, int durationTicks) {
        ACTIVE.put(caster.getUUID(), new WallState(entities, durationTicks));
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
                state.shatter();
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

        /** Estilhaça o cristal com partícula + som -- nenhuma entidade vira bloco de verdade. */
        void shatter() {
            for (EarthBlockEntity entity : entities) {
                if (!entity.isAlive()) {
                    continue;
                }
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                            entity.getX(), entity.getY() + 0.5, entity.getZ(), 10, 0.25, 0.3, 0.25, 0.02);
                }
                entity.discard();
            }
            if (!entities.isEmpty()) {
                EarthBlockEntity any = entities.getFirst();
                any.level().playSound(null, any.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.9f, 1.0f);
            }
        }
    }
}