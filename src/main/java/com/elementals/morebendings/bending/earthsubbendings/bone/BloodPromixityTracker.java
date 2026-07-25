package com.elementals.morebendings.bending.earthsubbendings.bone;

import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.registry.ModAttachments;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.blood.BloodElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Roda em background no servidor (registrado em NeoForge.EVENT_BUS via
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod}, mesmo
 * esquema que {@link
 * com.elementals.morebendings.bending.earthsubbendings.mud.MudTrapManager}
 * já usa) verificando, a cada {@link #CHECK_INTERVAL_TICKS}, se algum Earth
 * bender que ainda não satisfez o pré-requisito de Bone Bending (ver {@link
 * BoneElement#canAcquire}) está a até {@link
 * BoneElement#BLOOD_PROXIMITY_RANGE} blocos de um Blood bender online.
 *
 * Assim que isso acontece uma vez, fica marcado pra sempre em {@link
 * PlayerSubbendingData#setMetBloodBender} -- é por isso que essa checagem
 * não pode simplesmente rodar dentro de {@code BoneElement.canAcquire}
 * (chamado só no instante da concessão via comando): o encontro pode ter
 * acontecido horas ou dias antes, com o jogador já offline ou longe do
 * Blood bender na hora em que alguém for de fato conceder a sub-bending.
 *
 * IMPORTANTE (não consegui testar isso rodando o jogo de verdade): a
 * assinatura de {@code ServerTickEvent.Post#getServer()} é a que o NeoForge
 * 21.1.x deveria expor pra pegar o MinecraftServer a partir do evento -- se
 * o nome do método tiver mudado nessa versão específica, é só trocar por
 * como {@code MudTrapManager}/outros listeners deste addon acessam o
 * server (ou por {@code ServerLifecycleHooks.getCurrentServer()}).
 */
public final class BloodProximityTracker {

    private static final int CHECK_INTERVAL_TICKS = 20; // 1x por segundo -- não precisa ser por tick

    private BloodProximityTracker() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        List<ServerPlayer> bloodBenders = new ArrayList<>();
        List<ServerPlayer> candidates = new ArrayList<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Bender bender = Bender.getBender(player);

            if (bender.hasElement(BloodElement.get())) {
                bloodBenders.add(player);
            }

            PlayerSubbendingData data = player.getData(ModAttachments.SUBBENDINGS);
            if (!data.hasMetBloodBender() && bender.hasElement(EarthElement.get())) {
                candidates.add(player);
            }
        }

        if (bloodBenders.isEmpty() || candidates.isEmpty()) {
            return;
        }

        for (ServerPlayer candidate : candidates) {
            for (ServerPlayer blood : bloodBenders) {
                if (blood == candidate) {
                    continue; // não conta como "perto de um Blood bender" ser um você mesmo
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