package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.avatarstate.AvatarStateManager;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Cliente → servidor: "aperta/solta a tecla de Avatar State" -- ver {@code ModKeyMappings}. */
public record ToggleAvatarStatePacket() {

    public static final StreamCodec<FriendlyByteBuf, ToggleAvatarStatePacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleAvatarStatePacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.TOGGLE_AVATAR_STATE_ID);
    }

    public static void handle(PacketContext<ToggleAvatarStatePacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();
        AvatarStateManager.toggle(player);
    }
}