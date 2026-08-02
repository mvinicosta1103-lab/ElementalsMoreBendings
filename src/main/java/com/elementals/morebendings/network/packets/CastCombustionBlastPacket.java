package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionElement;
import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionExplosionAbility;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import dev.saperate.elementals.data.Bender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "começa a focar o Combustion Blast agora" (tecla
 * dedicada vinculada em {@code ModKeyMappings.CAST_COMBUSTION_BLAST}),
 * independente do elemento ativo no momento ou do slot numérico em que a
 * habilidade estiver vinculada. Mesmo padrão do {@link CastGasCloudPacket},
 * só que aqui a ability É canalizada -- diferente do Gas Cloud (instantâneo),
 * {@code CombustionExplosionAbility.onCall} termina deixando
 * {@code currAbility} setado como esta ability. Isso é seguro: o resto do
 * fluxo (onTick/onLeftClick/onRightClick) já é tratado pelo sistema de cast
 * genérico do mod base assim que {@code currAbility} está setado, do mesmo
 * jeito que seria se o jogador tivesse apertado um bind1-4 normal.
 */
public record CastCombustionBlastPacket() {

    public static final StreamCodec<FriendlyByteBuf, CastCombustionBlastPacket> STREAM_CODEC =
            StreamCodec.unit(new CastCombustionBlastPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CAST_COMBUSTION_BLAST_ID);
    }

    public static void handle(PacketContext<CastCombustionBlastPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();

        if (!CombustionElement.isCombustionBender(player)
                || !CombustionElement.hasUpgrade(player, CombustionElement.COMBUSTION_EXPLOSION)) {
            // Não é Combustion bender, ou o nó raiz "combustionExplosion"
            // ainda não foi comprado -- ver CombustionElement#autoUnlockRoot.
            // Ignora em silêncio, igual o resto do sistema de cast faz.
            return;
        }

        Bender bender = Bender.getBender(player);
        if (bender == null || bender.currAbility != null) {
            // Já está canalizando alguma coisa (esta ability ou outra) --
            // não deixa a tecla dedicada reiniciar o foco no meio do
            // caminho nem atropelar outra ability em andamento.
            return;
        }

        new CombustionExplosionAbility().onCall(bender, 0L);
    }
}