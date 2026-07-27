package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.client.layers.ClientPlasmaBoostCache;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Servidor → cliente: "este jogador ligou/desligou o Plasma Boost". */
public record SyncPlasmaBoostPacket(UUID playerId, boolean active) {

    public static final StreamCodec<FriendlyByteBuf, SyncPlasmaBoostPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, SyncPlasmaBoostPacket::playerId,
                    ByteBufCodecs.BOOL, SyncPlasmaBoostPacket::active,
                    SyncPlasmaBoostPacket::new);

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.SYNC_PLASMA_BOOST_ID);
    }

    public static void handle(PacketContext<SyncPlasmaBoostPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.CLIENT);
        SyncPlasmaBoostPacket packet = ctx.message();
        ClientPlasmaBoostCache.set(packet.playerId(), packet.active());
    }
}