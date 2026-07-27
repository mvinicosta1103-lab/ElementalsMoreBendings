package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaBoostState;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Cliente → servidor: "liga/desliga meu Plasma Boost agora" (tecla em ModKeyMappings). */
public record TogglePlasmaBoostPacket() {

    public static final StreamCodec<FriendlyByteBuf, TogglePlasmaBoostPacket> STREAM_CODEC =
            StreamCodec.unit(new TogglePlasmaBoostPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.TOGGLE_PLASMA_BOOST_ID);
    }

    public static void handle(PacketContext<TogglePlasmaBoostPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        PlasmaBoostState.toggle(ctx.sender());
    }
}