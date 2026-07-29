package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dirige o pulso periódico de {@code echoSense} -- a cada {@link
 * EchoSenseAbility#PULSE_INTERVAL_TICKS} ticks, revela entidades vivas
 * próximas ao bender via partículas visíveis só para ele. Registrado via
 * NeoForge.EVENT_BUS.addListener em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod}, mesmo
 * esquema de {@code SilenceFieldManager}.
 */
public final class EchoSenseManager {

    private static final Map<UUID, Integer> tickCounters = new HashMap<>();

    private EchoSenseManager() {
    }

    static void resetTimer(UUID playerId) {
        tickCounters.put(playerId, 0);
    }

    static void clearTimer(UUID playerId) {
        tickCounters.remove(playerId);
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!EchoSenseAbility.isActive(id)) {
                continue;
            }

            Bender bender = Bender.getBender(player);
            if (bender == null || !SoundElement.isSoundBender(bender)) {
                EchoSenseAbility.deactivate(id);
                continue;
            }

            int ticks = tickCounters.getOrDefault(id, 0) + 1;
            if (ticks < EchoSenseAbility.PULSE_INTERVAL_TICKS) {
                tickCounters.put(id, ticks);
                continue;
            }
            tickCounters.put(id, 0);

            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }

            double radius = SoundElement.hasUpgrade(player, SoundElement.ECHO_SENSE_RADIUS_I)
                    ? EchoSenseAbility.BASE_RADIUS + 6.0 : EchoSenseAbility.BASE_RADIUS;

            AABB area = player.getBoundingBox().inflate(radius);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive());

            for (LivingEntity entity : nearby) {
                level.sendParticles(player, ParticleTypes.NOTE,
                        true,
                        entity.getX(), entity.getY() + entity.getBbHeight() + 0.3, entity.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
}
