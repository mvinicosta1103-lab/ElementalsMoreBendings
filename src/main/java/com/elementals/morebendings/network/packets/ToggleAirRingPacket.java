package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.avatarstate.AvatarStateManager;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "liga/desliga o anel de Ar" (tecla dedicada, ver
 * {@code ModKeyMappings.TOGGLE_RING_AIR}). Só tem efeito se o jogador já
 * estiver no Avatar State -- ver {@link AvatarStateManager#toggleRing}.
 */
public record ToggleAirRingPacket() {

    public static final StreamCodec<FriendlyByteBuf, ToggleAirRingPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleAirRingPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.TOGGLE_AIR_RING_ID);
    }

    public static void handle(PacketContext<ToggleAirRingPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();
        Boolean nowEnabled = AvatarStateManager.toggleRing(player, AvatarStateManager.RingElement.AIR);
        if (nowEnabled != null) {
            player.displayClientMessage(Component.literal(
                    nowEnabled ? "§fAnel de Ar ligado." : "§7Anel de Ar desligado."), true);
        }
    }
}