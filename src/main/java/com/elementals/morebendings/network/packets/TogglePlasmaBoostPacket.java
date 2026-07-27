package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaBoostState;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record TogglePlasmaBoostPacket() {

    public static final StreamCodec<FriendlyByteBuf, TogglePlasmaBoostPacket> STREAM_CODEC =
            StreamCodec.unit(new TogglePlasmaBoostPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.TOGGLE_PLASMA_BOOST_ID);
    }

    public static void handle(PacketContext<TogglePlasmaBoostPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();
        boolean nowActive = PlasmaBoostState.toggle(player);
        player.displayClientMessage(
                Component.literal(nowActive ? "§bPlasma Boost ATIVADO" : "§7Plasma Boost desativado"),
                true);
    }
}