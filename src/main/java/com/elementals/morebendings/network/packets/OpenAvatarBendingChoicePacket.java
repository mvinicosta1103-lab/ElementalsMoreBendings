package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.client.gui.AvatarBendingChoiceScreen;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Servidor → cliente: "abra a GUI de escolha de Energybend pra este alvo".
 * Disparado por {@code AvatarBendingOptions#beginChoice} ao apertar a
 * tecla dedicada de conceder/remover (ver {@code
 * CastAvatarBendingGrantPacket}/{@code CastAvatarBendingRemovePacket})
 * mirando um alvo válido -- o alvo já está congelado (ver {@code
 * AvatarBendingFreezeManager}) no momento em que este pacote sai.
 * <p>
 * {@code optionsBlob} carrega a lista de opções já pronta (ver {@code
 * AvatarBendingOptions#buildOptionsBlob}/{@code parseOptionsBlob}) -- um
 * único campo String em vez de um StreamCodec de lista de records, pra
 * manter a serialização simples.
 */
public record OpenAvatarBendingChoicePacket(boolean grantMode, UUID targetId, String targetName, String optionsBlob) {

    public static final StreamCodec<FriendlyByteBuf, OpenAvatarBendingChoicePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, OpenAvatarBendingChoicePacket::grantMode,
                    UUIDUtil.STREAM_CODEC, OpenAvatarBendingChoicePacket::targetId,
                    ByteBufCodecs.STRING_UTF8, OpenAvatarBendingChoicePacket::targetName,
                    ByteBufCodecs.STRING_UTF8, OpenAvatarBendingChoicePacket::optionsBlob,
                    OpenAvatarBendingChoicePacket::new);

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.OPEN_AVATAR_BENDING_CHOICE_ID);
    }

    public static void handle(PacketContext<OpenAvatarBendingChoicePacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.CLIENT);
        AvatarBendingChoiceScreen.open(ctx.message());
    }
}