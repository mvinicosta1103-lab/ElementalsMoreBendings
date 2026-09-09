package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.avatarstate.AvatarBendingFreezeManager;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "fechei a GUI de Energybend sem escolher nada"
 * (ver {@code AvatarBendingChoiceScreen#onClose}). Libera o alvo na hora
 * em {@link AvatarBendingFreezeManager} em vez de deixá-lo congelado até
 * o timeout de 30s de {@link AvatarBendingFreezeManager#onServerTick}.
 */
public record CancelAvatarBendingChoicePacket() {

    public static final StreamCodec<FriendlyByteBuf, CancelAvatarBendingChoicePacket> STREAM_CODEC =
            StreamCodec.unit(new CancelAvatarBendingChoicePacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CANCEL_AVATAR_BENDING_CHOICE_ID);
    }

    public static void handle(PacketContext<CancelAvatarBendingChoicePacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        AvatarBendingFreezeManager.release(ctx.sender());
    }
}