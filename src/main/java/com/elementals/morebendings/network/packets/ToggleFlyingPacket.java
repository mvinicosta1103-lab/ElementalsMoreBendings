package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.airsubbendings.flying.FlyingAbility;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "liga/desliga meu voo agora" (tecla vinculada em
 * {@code ModKeyMappings}).
 */
public record ToggleFlyingPacket() {

    public static final StreamCodec<FriendlyByteBuf, ToggleFlyingPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleFlyingPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.TOGGLE_FLYING_ID);
    }

    public static void handle(PacketContext<ToggleFlyingPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();
        FlyingAbility.toggle(player);
    }
}