package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.client.layers.ClientAvatarStateCache;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Servidor → cliente: "este jogador ligou/desligou o Avatar State" (olhos brilhantes + etc). */
public record SyncAvatarStatePacket(UUID playerId, boolean active) {

    public static final StreamCodec<FriendlyByteBuf, SyncAvatarStatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, SyncAvatarStatePacket::playerId,
                    ByteBufCodecs.BOOL, SyncAvatarStatePacket::active,
                    SyncAvatarStatePacket::new);

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.SYNC_AVATAR_STATE_ID);
    }

    public static void handle(PacketContext<SyncAvatarStatePacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.CLIENT);
        SyncAvatarStatePacket packet = ctx.message();
        ClientAvatarStateCache.set(packet.playerId(), packet.active());
    }
}