package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.client.layers.ClientPlasmaClawsFxCache;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Servidor → cliente: "a garra de plasma desse jogador acabou de ativar",
 * pra tocar o flash de fogo nas mãos (ver {@link ClientPlasmaClawsFxCache}).
 * Diferente do Boost, não tem estado "desligado" -- o flash expira sozinho
 * no cliente. */
public record PlayPlasmaClawsFxPacket(UUID playerId) {

    public static final StreamCodec<FriendlyByteBuf, PlayPlasmaClawsFxPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, PlayPlasmaClawsFxPacket::playerId,
                    PlayPlasmaClawsFxPacket::new);

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.PLAY_PLASMA_CLAWS_FX_ID);
    }

    public static void handle(PacketContext<PlayPlasmaClawsFxPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.CLIENT);
        PlayPlasmaClawsFxPacket packet = ctx.message();
        ClientPlasmaClawsFxCache.trigger(packet.playerId());
    }
}