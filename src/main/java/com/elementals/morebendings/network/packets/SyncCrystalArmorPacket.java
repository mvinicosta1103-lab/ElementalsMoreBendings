package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.client.layers.ClientCrystalArmorCache;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Servidor → cliente: "este jogador ligou/desligou a Armadura de Cristal". */
public record SyncCrystalArmorPacket(UUID playerId, boolean active) {

    public static final StreamCodec<FriendlyByteBuf, SyncCrystalArmorPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, SyncCrystalArmorPacket::playerId,
                    ByteBufCodecs.BOOL, SyncCrystalArmorPacket::active,
                    SyncCrystalArmorPacket::new);

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.SYNC_CRYSTAL_ARMOR_ID);
    }

    public static void handle(PacketContext<SyncCrystalArmorPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.CLIENT);
        SyncCrystalArmorPacket packet = ctx.message();
        ClientCrystalArmorCache.set(packet.playerId(), packet.active());
    }
}