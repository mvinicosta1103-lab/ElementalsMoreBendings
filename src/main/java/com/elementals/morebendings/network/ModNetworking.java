package com.elementals.morebendings.network;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.network.packets.CastGasCloudPacket;
import com.elementals.morebendings.network.packets.CycleSpecializationPacket;
import com.elementals.morebendings.network.packets.ToggleFlyingPacket;
import commonnetwork.api.Network;
import commonnetwork.networking.data.Side;
import net.minecraft.resources.ResourceLocation;
import com.elementals.morebendings.network.packets.SyncPlasmaBoostPacket;
import com.elementals.morebendings.network.packets.TogglePlasmaBoostPacket;

public final class ModNetworking {

    public static final ResourceLocation TOGGLE_PLASMA_BOOST_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_plasma_boost");

    public static final ResourceLocation SYNC_PLASMA_BOOST_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_plasma_boost");

    public static final ResourceLocation TOGGLE_FLYING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_flying");

    public static final ResourceLocation CAST_GAS_CLOUD_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_cloud");

    public static final ResourceLocation CYCLE_SPECIALIZATION_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cycle_specialization");

    public static final ResourceLocation SYNC_CRYSTAL_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_crystal_armor");

    public static void register() {
        Network.registerPacket(ToggleFlyingPacket.type(), ToggleFlyingPacket.class,
                ToggleFlyingPacket.STREAM_CODEC, ToggleFlyingPacket::handle);
        Network.registerPacket(CastGasCloudPacket.type(), CastGasCloudPacket.class,
                CastGasCloudPacket.STREAM_CODEC, CastGasCloudPacket::handle);
        Network.registerPacket(CycleSpecializationPacket.type(), CycleSpecializationPacket.class,
                CycleSpecializationPacket.STREAM_CODEC, CycleSpecializationPacket::handle);
        Network.registerPacket(TogglePlasmaBoostPacket.type(), TogglePlasmaBoostPacket.class,
                TogglePlasmaBoostPacket.STREAM_CODEC, TogglePlasmaBoostPacket::handle);
        Network.registerPacket(SyncPlasmaBoostPacket.type(), SyncPlasmaBoostPacket.class,
                SyncPlasmaBoostPacket.STREAM_CODEC, SyncPlasmaBoostPacket::handle);
        Network.registerPacket(SyncCrystalArmorPacket.type(), SyncCrystalArmorPacket.class,
                SyncCrystalArmorPacket.STREAM_CODEC, SyncCrystalArmorPacket::handle);
    }

    public static void expectSideOrThrow(Side current, Side expected) {
        if (!current.equals(expected)) {
            throw new RuntimeException("Pacote recebido no lado errado (esperava " + expected + ", era " + current + ")");
        }
    }

    private ModNetworking() {
    }
}