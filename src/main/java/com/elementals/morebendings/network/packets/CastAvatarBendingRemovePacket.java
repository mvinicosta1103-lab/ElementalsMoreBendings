package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.avatarstate.AvatarBendingRemoveAbility;
import com.elementals.morebendings.bending.avatarstate.AvatarStateManager;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import dev.saperate.elementals.data.Bender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "remove o elemento-base selecionado do jogador que
 * estou mirando" (tecla dedicada, ver {@code
 * ModKeyMappings.CAST_AVATAR_BENDING_REMOVE}). Só funciona enquanto o
 * jogador está no Avatar State -- ver {@link AvatarStateManager#isActive}.
 *
 * {@link AvatarBendingRemoveAbility#onCall} é instantânea (não canaliza e
 * não deixa {@code currAbility} setado), então é seguro chamar direto
 * aqui, mesmo padrão de {@link CastGasCloudPacket}.
 */
public record CastAvatarBendingRemovePacket() {

    public static final StreamCodec<FriendlyByteBuf, CastAvatarBendingRemovePacket> STREAM_CODEC =
            StreamCodec.unit(new CastAvatarBendingRemovePacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CAST_AVATAR_BENDING_REMOVE_ID);
    }

    public static void handle(PacketContext<CastAvatarBendingRemovePacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();

        if (!AvatarStateManager.isActive(player)) {
            player.displayClientMessage(Component.literal(
                    "§7Você precisa estar no Avatar State pra fazer isso."), true);
            return;
        }

        Bender bender = Bender.getBender(player);
        new AvatarBendingRemoveAbility().onCall(bender, 0L);
    }
}