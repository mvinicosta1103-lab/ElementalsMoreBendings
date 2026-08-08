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
 * Cliente → servidor: "liga/desliga o anel de Água" (tecla dedicada, ver
 * {@code ModKeyMappings.TOGGLE_RING_WATER}). Só tem efeito se o jogador já
 * estiver no Avatar State -- ver {@link AvatarStateManager#toggleRing}.
 */
public record ToggleWaterRingPacket() {

    public static final StreamCodec<FriendlyByteBuf, ToggleWaterRingPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleWaterRingPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.TOGGLE_WATER_RING_ID);
    }

    public static void handle(PacketContext<ToggleWaterRingPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();
        Boolean nowEnabled = AvatarStateManager.toggleRing(player, AvatarStateManager.RingElement.WATER);
        if (nowEnabled != null) {
            player.displayClientMessage(Component.literal(
                    nowEnabled ? "§bAnel de Água ligado." : "§7Anel de Água desligado."), true);
        }
    }
}