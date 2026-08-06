package com.elementals.morebendings.situations;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.StateDataSaverAndLoader;
import dev.saperate.elementals.network.packets.common.SyncLevelPacket;
import dev.saperate.elementals.network.packets.common.SyncUpgradeListPacket;
import commonnetwork.api.Network;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.concurrent.ThreadLocalRandom;

/**
 * "Situations System": roda em background no servidor e deixa qualquer
 * jogador aprender uma sub-bending sem usar o scroll correspondente, desde
 * que esteja na situação ambiental certa (ver {@link SituationsRegistry})
 * -- uma chance pequena é rolada a cada checagem periódica enquanto a
 * condição continuar batendo, então ficar mais tempo na situação aumenta a
 * chance total de aprender, sem garantir nada na hora.
 * <br><br>
 * Mesmo esquema de {@code BloodProximityTracker} (roda a cada
 * {@link #CHECK_INTERVAL_TICKS} ticks, iterando os jogadores online do
 * servidor) e de sincronização pós-{@code addElement} do
 * {@code AbstractSubbendingScrollItem}/{@code MoreBendingCommand}.
 */
public final class SituationsSystem {

    private static final int CHECK_INTERVAL_TICKS = 100; // 1x a cada 5s

    private SituationsSystem() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Bender bender = Bender.getBender(player);
            if (bender == null) {
                continue;
            }

            for (SituationDefinition definition : SituationsRegistry.ALL) {
                if (bender.hasElement(definition.subbendingElement().get())) {
                    continue; // já tem essa sub-bending
                }
                if (!bender.hasElement(definition.parentElement().get())) {
                    continue; // nem é do tipo de dobrador certo -- não gasta tempo checando o ambiente
                }
                if (!definition.situation().matches(player)) {
                    continue;
                }
                if (ThreadLocalRandom.current().nextDouble() >= definition.chancePerCheck()) {
                    continue;
                }

                grant(bender, player, definition);
            }
        }
    }

    /** Mesma sincronização + persistência que o scroll/MoreBendingCommand fazem pós-addElement. */
    private static void grant(Bender bender, ServerPlayer player, SituationDefinition definition) {
        bender.addElement(definition.subbendingElement().get(), true);
        definition.onGranted().accept(bender);

        Network.getNetworkHandler().sendToClient(SyncUpgradeListPacket.createFromBender(bender), player);
        Network.getNetworkHandler().sendToClient(SyncLevelPacket.createFromBender(bender), player);
        StateDataSaverAndLoader.getServerState(player.getServer()).setDirty();

        player.sendSystemMessage(Component.literal(definition.discoveryMessage()));
    }
}
