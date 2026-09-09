package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.avatarstate.AvatarBendingFreezeManager;
import com.elementals.morebendings.bending.avatarstate.AvatarBendingOptions;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "escolhi {@code chosenId} na GUI de Energybend"
 * (ver {@code AvatarBendingChoiceScreen}, {@link
 * OpenAvatarBendingChoicePacket}). {@code chosenId} vem no mesmo formato
 * de {@link AvatarBendingOptions#buildOptionsBlob} ({@code B:<SYMBOL>} ou
 * {@code S:<subbendingId>}).
 * <p>
 * Sempre libera a escolha pendente do caster em {@link
 * AvatarBendingFreezeManager} ao final, escolha aplicada com sucesso ou
 * não -- o alvo nunca deveria continuar congelado depois que o caster
 * fechou a GUI com uma escolha.
 */
public record ChooseAvatarBendingPacket(boolean grantMode, String chosenId) {

    public static final StreamCodec<FriendlyByteBuf, ChooseAvatarBendingPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ChooseAvatarBendingPacket::grantMode,
                    ByteBufCodecs.STRING_UTF8, ChooseAvatarBendingPacket::chosenId,
                    ChooseAvatarBendingPacket::new);

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CHOOSE_AVATAR_BENDING_ID);
    }

    public static void handle(PacketContext<ChooseAvatarBendingPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer caster = ctx.sender();

        ServerPlayer target = AvatarBendingFreezeManager.pendingTarget(caster);
        try {
            if (target == null) {
                return;
            }
            AvatarBendingOptions.apply(caster, target, ctx.message().chosenId(), ctx.message().grantMode());
        } finally {
            AvatarBendingFreezeManager.release(caster);
        }
    }
}