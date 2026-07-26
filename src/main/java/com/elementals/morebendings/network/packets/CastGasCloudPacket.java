package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.airsubbendings.gas.GasCloudAbility;
import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import dev.saperate.elementals.data.Bender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "solta o Gas Cloud agora" (tecla dedicada vinculada
 * em {@code ModKeyMappings.CAST_GAS_CLOUD}), independente do elemento
 * ativo no momento ou do slot numérico em que a habilidade estiver
 * vinculada. Mesmo padrão do {@link ToggleFlyingPacket}.
 *
 * {@code GasCloudAbility.onCall} é instantâneo (não canaliza e não deixa
 * {@code currAbility} setado), então é seguro chamar direto aqui sem
 * passar pelo sistema de cast (bind + hold) do mod base.
 */
public record CastGasCloudPacket() {

    public static final StreamCodec<FriendlyByteBuf, CastGasCloudPacket> STREAM_CODEC =
            StreamCodec.unit(new CastGasCloudPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CAST_GAS_CLOUD_ID);
    }

    public static void handle(PacketContext<CastGasCloudPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();

        if (!GasElement.isGasBender(player) || !GasElement.hasUpgrade(player, GasElement.GAS_CLOUD)) {
            // Não é Gas bender, ou o nó raiz "gasCloud" ainda não foi
            // comprado -- ver GasElement#autoUnlockRoot. Ignora em
            // silêncio, igual o resto do sistema de cast faz.
            return;
        }

        Bender bender = Bender.getBender(player);
        new GasCloudAbility().onCall(bender, 0L);
    }
}