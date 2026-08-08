package com.elementals.morebendings.network;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.network.packets.CastGasCloudPacket;
import com.elementals.morebendings.network.packets.CastGasSuffocatePacket;
import com.elementals.morebendings.network.packets.CastGasLeakPacket;
import com.elementals.morebendings.network.packets.CastGasIgnitePacket;
import com.elementals.morebendings.network.packets.CastGasJetPacket;
import com.elementals.morebendings.network.packets.CastGasMiasmaPacket;
import com.elementals.morebendings.network.packets.CastGasCorrosiveMistPacket;
import com.elementals.morebendings.network.packets.CastCombustionBlastPacket;
import com.elementals.morebendings.network.packets.CycleSpecializationPacket;
import com.elementals.morebendings.network.packets.ToggleFlyingPacket;
import com.elementals.morebendings.network.packets.SyncCrystalArmorPacket;
import commonnetwork.api.Network;
import commonnetwork.networking.data.Side;
import net.minecraft.resources.ResourceLocation;
import com.elementals.morebendings.network.packets.SyncPlasmaBoostPacket;
import com.elementals.morebendings.network.packets.TogglePlasmaBoostPacket;
import com.elementals.morebendings.network.packets.PlayPlasmaClawsFxPacket;
import com.elementals.morebendings.network.packets.ToggleAvatarStatePacket;
import com.elementals.morebendings.network.packets.SyncAvatarStatePacket;
import com.elementals.morebendings.network.packets.ToggleFireRingPacket;
import com.elementals.morebendings.network.packets.ToggleWaterRingPacket;
import com.elementals.morebendings.network.packets.ToggleEarthRingPacket;
import com.elementals.morebendings.network.packets.ToggleAirRingPacket;
import com.elementals.morebendings.network.packets.CycleAvatarBendingPacket;
import com.elementals.morebendings.network.packets.CastAvatarBendingGrantPacket;
import com.elementals.morebendings.network.packets.CastAvatarBendingRemovePacket;

public final class ModNetworking {

    public static final ResourceLocation TOGGLE_AVATAR_STATE_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_avatar_state");

    public static final ResourceLocation SYNC_AVATAR_STATE_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_avatar_state");

    public static final ResourceLocation TOGGLE_PLASMA_BOOST_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_plasma_boost");

    public static final ResourceLocation SYNC_PLASMA_BOOST_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_plasma_boost");

    public static final ResourceLocation PLAY_PLASMA_CLAWS_FX_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "play_plasma_claws_fx");

    public static final ResourceLocation TOGGLE_FLYING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_flying");

    public static final ResourceLocation CAST_GAS_CLOUD_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_cloud");

    public static final ResourceLocation CAST_GAS_SUFFOCATE_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_suffocate");

    public static final ResourceLocation CAST_GAS_LEAK_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_leak");

    public static final ResourceLocation CAST_GAS_IGNITE_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_ignite");

    public static final ResourceLocation CAST_GAS_JET_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_jet");

    public static final ResourceLocation CAST_GAS_MIASMA_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_miasma");

    public static final ResourceLocation CAST_GAS_CORROSIVE_MIST_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_gas_corrosive_mist");

    public static final ResourceLocation CAST_COMBUSTION_BLAST_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_combustion_blast");

    public static final ResourceLocation CYCLE_SPECIALIZATION_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cycle_specialization");

    public static final ResourceLocation SYNC_CRYSTAL_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_crystal_armor");

    public static final ResourceLocation TOGGLE_FIRE_RING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_fire_ring");

    public static final ResourceLocation TOGGLE_WATER_RING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_water_ring");

    public static final ResourceLocation TOGGLE_EARTH_RING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_earth_ring");

    public static final ResourceLocation TOGGLE_AIR_RING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_air_ring");

    public static final ResourceLocation CYCLE_AVATAR_BENDING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cycle_avatar_bending");

    public static final ResourceLocation CAST_AVATAR_BENDING_GRANT_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_avatar_bending_grant");

    public static final ResourceLocation CAST_AVATAR_BENDING_REMOVE_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "cast_avatar_bending_remove");

    public static void register() {
        Network.registerPacket(ToggleFlyingPacket.type(), ToggleFlyingPacket.class,
                ToggleFlyingPacket.STREAM_CODEC, ToggleFlyingPacket::handle);
        Network.registerPacket(CastGasCloudPacket.type(), CastGasCloudPacket.class,
                CastGasCloudPacket.STREAM_CODEC, CastGasCloudPacket::handle);
        Network.registerPacket(CastGasSuffocatePacket.type(), CastGasSuffocatePacket.class,
                CastGasSuffocatePacket.STREAM_CODEC, CastGasSuffocatePacket::handle);
        Network.registerPacket(CastGasLeakPacket.type(), CastGasLeakPacket.class,
                CastGasLeakPacket.STREAM_CODEC, CastGasLeakPacket::handle);
        Network.registerPacket(CastGasIgnitePacket.type(), CastGasIgnitePacket.class,
                CastGasIgnitePacket.STREAM_CODEC, CastGasIgnitePacket::handle);
        Network.registerPacket(CastGasJetPacket.type(), CastGasJetPacket.class,
                CastGasJetPacket.STREAM_CODEC, CastGasJetPacket::handle);
        Network.registerPacket(CastGasMiasmaPacket.type(), CastGasMiasmaPacket.class,
                CastGasMiasmaPacket.STREAM_CODEC, CastGasMiasmaPacket::handle);
        Network.registerPacket(CastGasCorrosiveMistPacket.type(), CastGasCorrosiveMistPacket.class,
                CastGasCorrosiveMistPacket.STREAM_CODEC, CastGasCorrosiveMistPacket::handle);
        Network.registerPacket(CastCombustionBlastPacket.type(), CastCombustionBlastPacket.class,
                CastCombustionBlastPacket.STREAM_CODEC, CastCombustionBlastPacket::handle);
        Network.registerPacket(CycleSpecializationPacket.type(), CycleSpecializationPacket.class,
                CycleSpecializationPacket.STREAM_CODEC, CycleSpecializationPacket::handle);
        Network.registerPacket(TogglePlasmaBoostPacket.type(), TogglePlasmaBoostPacket.class,
                TogglePlasmaBoostPacket.STREAM_CODEC, TogglePlasmaBoostPacket::handle);
        Network.registerPacket(SyncPlasmaBoostPacket.type(), SyncPlasmaBoostPacket.class,
                SyncPlasmaBoostPacket.STREAM_CODEC, SyncPlasmaBoostPacket::handle);
        Network.registerPacket(SyncCrystalArmorPacket.type(), SyncCrystalArmorPacket.class,
                SyncCrystalArmorPacket.STREAM_CODEC, SyncCrystalArmorPacket::handle);
        Network.registerPacket(PlayPlasmaClawsFxPacket.type(), PlayPlasmaClawsFxPacket.class,
                PlayPlasmaClawsFxPacket.STREAM_CODEC, PlayPlasmaClawsFxPacket::handle);
        Network.registerPacket(ToggleAvatarStatePacket.type(), ToggleAvatarStatePacket.class,
                ToggleAvatarStatePacket.STREAM_CODEC, ToggleAvatarStatePacket::handle);
        Network.registerPacket(SyncAvatarStatePacket.type(), SyncAvatarStatePacket.class,
                SyncAvatarStatePacket.STREAM_CODEC, SyncAvatarStatePacket::handle);
        Network.registerPacket(ToggleFireRingPacket.type(), ToggleFireRingPacket.class,
                ToggleFireRingPacket.STREAM_CODEC, ToggleFireRingPacket::handle);
        Network.registerPacket(ToggleWaterRingPacket.type(), ToggleWaterRingPacket.class,
                ToggleWaterRingPacket.STREAM_CODEC, ToggleWaterRingPacket::handle);
        Network.registerPacket(ToggleEarthRingPacket.type(), ToggleEarthRingPacket.class,
                ToggleEarthRingPacket.STREAM_CODEC, ToggleEarthRingPacket::handle);
        Network.registerPacket(ToggleAirRingPacket.type(), ToggleAirRingPacket.class,
                ToggleAirRingPacket.STREAM_CODEC, ToggleAirRingPacket::handle);
        Network.registerPacket(CycleAvatarBendingPacket.type(), CycleAvatarBendingPacket.class,
                CycleAvatarBendingPacket.STREAM_CODEC, CycleAvatarBendingPacket::handle);
        Network.registerPacket(CastAvatarBendingGrantPacket.type(), CastAvatarBendingGrantPacket.class,
                CastAvatarBendingGrantPacket.STREAM_CODEC, CastAvatarBendingGrantPacket::handle);
        Network.registerPacket(CastAvatarBendingRemovePacket.type(), CastAvatarBendingRemovePacket.class,
                CastAvatarBendingRemovePacket.STREAM_CODEC, CastAvatarBendingRemovePacket::handle);
    }

    public static void expectSideOrThrow(Side current, Side expected) {
        if (!current.equals(expected)) {
            throw new RuntimeException("Pacote recebido no lado errado (esperava " + expected + ", era " + current + ")");
        }
    }

    private ModNetworking() {
    }
}