package com.elementals.morebendings.network;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.network.packets.BuyGasUpgradePacket;
import com.elementals.morebendings.network.packets.CastGasCloudPacket;
import com.elementals.morebendings.network.packets.SyncGasProgressPacket;
import com.elementals.morebendings.network.packets.ToggleFlyingPacket;
import commonnetwork.api.Network;
import commonnetwork.networking.data.Side;
import net.minecraft.resources.ResourceLocation;

/**
 * Registro central dos pacotes do addon, no mesmo estilo de
 * {@code dev.saperate.elementals.network.ElementalsNetworking} do mod base
 * (mesma lib commonnetwork, já presente em libs/). Chamado uma vez no
 * construtor de {@code ElementalsMoreBendingsMod} (NÃO em
 * {@code CommonClass.init()} / {@code FMLCommonSetupEvent} — nessa altura o
 * {@code RegisterPayloadHandlersEvent} da commonnetwork já disparou e os
 * pacotes PLAY não podem mais ser adicionados).
 */
public final class ModNetworking {

    public static final ResourceLocation BUY_GAS_UPGRADE_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "buy_gas_upgrade");
    public static final ResourceLocation CAST_GAS_CLOUD_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_cloud");
    public static final ResourceLocation TOGGLE_FLYING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_flying");
    public static final ResourceLocation SYNC_GAS_PROGRESS_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_gas_progress");

    public static void register() {
        Network.registerPacket(BuyGasUpgradePacket.type(), BuyGasUpgradePacket.class,
                BuyGasUpgradePacket.STREAM_CODEC, BuyGasUpgradePacket::handle);
        Network.registerPacket(CastGasCloudPacket.type(), CastGasCloudPacket.class,
                CastGasCloudPacket.STREAM_CODEC, CastGasCloudPacket::handle);
        Network.registerPacket(ToggleFlyingPacket.type(), ToggleFlyingPacket.class,
                ToggleFlyingPacket.STREAM_CODEC, ToggleFlyingPacket::handle);
        Network.registerPacket(SyncGasProgressPacket.type(), SyncGasProgressPacket.class,
                SyncGasProgressPacket.STREAM_CODEC, SyncGasProgressPacket::handle);
    }

    public static void expectSideOrThrow(Side current, Side expected) {
        if (!current.equals(expected)) {
            throw new RuntimeException("Pacote recebido no lado errado (esperava " + expected + ", era " + current + ")");
        }
    }

    private ModNetworking() {
    }
}