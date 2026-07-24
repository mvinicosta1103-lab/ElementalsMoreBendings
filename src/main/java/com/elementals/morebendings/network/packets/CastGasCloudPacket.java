package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.airsubbendings.gas.GasCloudAbility;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "usei a Gas Cloud agora" (tecla vinculada em
 * {@code ModKeyMappings}). Sem payload — o servidor já sabe quem mandou.
 */
public record CastGasCloudPacket() {

    public static final StreamCodec<FriendlyByteBuf, CastGasCloudPacket> STREAM_CODEC =
            StreamCodec.unit(new CastGasCloudPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CAST_GAS_CLOUD_ID);
    }

    public static void handle(PacketContext<CastGasCloudPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();
        GasCloudAbility.execute(player);
    }
}