package com.elementals.morebendings.bending.watersubbendings.plant;

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
 * Dono de todas as paredes de vineWall ativas -- uma por caster, mesmo
 * esquema de {@link com.elementals.morebendings.bending.earthsubbendings.mud.MudTrapManager}.
 * Cada entrada guarda as entidades {@link EarthBlockEntity} flutuantes que
 * formam a parede (ver {@link PlantVineWallAbility}) e uma contagem
 * regressiva de ticks; quando ela zera, a parede desmancha sozinha.
 */
public final class PlantVineWallManager {

    private static final Map<UUID, WallState> ACTIVE = new HashMap<>();

    private PlantVineWallManager() {
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
                state.collapse();
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

        /** Desmancha as folhas com partícula + som -- nenhuma delas vira bloco de verdade. */
        void collapse() {
            for (EarthBlockEntity entity : entities) {
                if (!entity.isAlive()) {
                    continue;
                }
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
                            entity.getX(), entity.getY() + 0.5, entity.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
                }
                entity.discard();
            }
            if (!entities.isEmpty()) {
                EarthBlockEntity any = entities.getFirst();
                any.level().playSound(null, any.blockPosition(), SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }
    }
}