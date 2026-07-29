package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * Dirige as zonas de {@code silenceField} ativas -- aplica Lentidão em
 * quem entra no raio ao redor de cada bender com o toggle ligado, e limpa
 * o estado de quem desconectou ou deixou de ser um Sound bender válido.
 * Registrado via NeoForge.EVENT_BUS.addListener em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod}, mesmo
 * esquema de {@code MistCloudManager}/{@code PressureZoneManager}.
 */
public final class SilenceFieldManager {

    private static final int SLOWNESS_REFRESH_TICKS = 40; // reaplica 2s de Lentidão

    private SilenceFieldManager() {
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!SilenceFieldAbility.isActive(id)) {
                continue;
            }

            Bender bender = Bender.getBender(player);
            if (bender == null || !SoundElement.isSoundBender(bender)) {
                SilenceFieldAbility.deactivate(id);
                continue;
            }

            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }

            double radius = SoundElement.hasUpgrade(player, SoundElement.SILENCE_FIELD_RADIUS_I)
                    ? SilenceFieldAbility.BASE_RADIUS + 2.0 : SilenceFieldAbility.BASE_RADIUS;

            AABB area = player.getBoundingBox().inflate(radius);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive());

            for (LivingEntity entity : nearby) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        SLOWNESS_REFRESH_TICKS, 0, false, false));
            }
        }
    }
}
