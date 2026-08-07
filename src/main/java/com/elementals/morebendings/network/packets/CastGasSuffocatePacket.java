package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.airsubbendings.gas.GasSuffocateAbility;
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
 * Cliente → servidor: "solta o Suffocate agora" (tecla dedicada vinculada
 * em {@code ModKeyMappings.CAST_GAS_SUFFOCATE}), independente do elemento
 * ativo no momento ou do slot numérico em que a habilidade estiver
 * vinculada. Mesmo padrão do {@link CastGasCloudPacket}.
 */
public record CastGasSuffocatePacket() {

    public static final StreamCodec<FriendlyByteBuf, CastGasSuffocatePacket> STREAM_CODEC =
            StreamCodec.unit(new CastGasSuffocatePacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CAST_GAS_SUFFOCATE_ID);
    }

    public static void handle(PacketContext<CastGasSuffocatePacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();

        if (!GasElement.isGasBender(player) || !GasElement.hasUpgrade(player, GasElement.GAS_SUFFOCATE)) {
            // Não é Gas bender, ou o nó "gasSuffocate" ainda não foi
            // comprado. Ignora em silêncio, igual o resto do sistema de
            // cast faz.
            return;
        }

        Bender bender = Bender.getBender(player);
        new GasSuffocateAbility().onCall(bender, 0L);
    }
}