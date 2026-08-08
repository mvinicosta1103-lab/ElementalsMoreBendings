package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.avatarstate.AvatarBendingSelection;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import dev.saperate.elementals.elements.Element;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: avança o elemento-base "selecionado" pra
 * conceder/remover (Ar → Água → Terra → Fogo → Ar...), ver
 * {@link AvatarBendingSelection}. Tecla dedicada, ver {@code
 * ModKeyMappings.CYCLE_AVATAR_BENDING}. Não exige Avatar State ativo --
 * ciclar é só troca de estado local, sem custo (ver a doc de
 * {@code AvatarBendingSelection}); só CONCEDER/REMOVER de fato (ver
 * {@code CastAvatarBendingGrantPacket}/{@code CastAvatarBendingRemovePacket})
 * exige estar no Avatar State.
 */
public record CycleAvatarBendingPacket() {

    public static final StreamCodec<FriendlyByteBuf, CycleAvatarBendingPacket> STREAM_CODEC =
            StreamCodec.unit(new CycleAvatarBendingPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CYCLE_AVATAR_BENDING_ID);
    }

    public static void handle(PacketContext<CycleAvatarBendingPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();
        Element next = AvatarBendingSelection.cycle(player);
        player.displayClientMessage(Component.literal(
                "§eDobra selecionada: §f" + AvatarBendingSelection.displayName(next)), true);
    }
}