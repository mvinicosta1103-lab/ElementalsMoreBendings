package com.elementals.morebendings.bending.earthsubbendings.bone;

import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.registry.ModAttachments;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.blood.BloodElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * Roda em background no servidor verificando se algum Earth bender está
 * perto de um Blood bender.
 */
public final class BloodProximityTracker {

    private static final int CHECK_INTERVAL_TICKS = 20; // 1x por segundo

    private BloodProximityTracker() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        List<ServerPlayer> bloodBenders = new ArrayList<>();
        List<ServerPlayer> candidates = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Bender bender = Bender.getBender(player);

            if (bender != null) {
                if (bender.hasElement(BloodElement.get())) {
                    bloodBenders.add(player);
                }

                PlayerSubbendingData data = player.getData(ModAttachments.SUBBENDINGS);
                if (data != null && !data.hasMetBloodBender() && bender.hasElement(EarthElement.get())) {
                    candidates.add(player);
                }
            }
        }

        if (bloodBenders.isEmpty() || candidates.isEmpty()) {
            return;
        }

        for (ServerPlayer candidate : candidates) {
            for (ServerPlayer blood : bloodBenders) {
                if (blood == candidate) {
                    continue; // Não conta se for o próprio jogador
                }
                if (blood.level() == candidate.level()
                        && blood.distanceTo(candidate) <= BoneElement.BLOOD_PROXIMITY_RANGE) {
                    candidate.getData(ModAttachments.SUBBENDINGS).setMetBloodBender(true);
                    break;
                }
            }
        }
    }
}