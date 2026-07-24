package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.data.SubbendingType;
import com.elementals.morebendings.network.ModNetworking;
import com.elementals.morebendings.registry.ModAttachments;
import commonnetwork.api.Network;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "quero comprar o nó X da árvore do Gas". Enviado pela
 * {@code GasSkillTreeScreen} quando o jogador clica num nó desbloqueável.
 *
 * Mesmo padrão de record + StreamCodec que {@code BuyUpgradePacket} usa no
 * mod base.
 */
public record BuyGasUpgradePacket(String upgradeName) {

    public static final StreamCodec<FriendlyByteBuf, BuyGasUpgradePacket> STREAM_CODEC =
            StreamCodec.ofMember(BuyGasUpgradePacket::encode, BuyGasUpgradePacket::new);

    public BuyGasUpgradePacket(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.BUY_GAS_UPGRADE_ID);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.upgradeName);
    }

    public static void handle(PacketContext<BuyGasUpgradePacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();
        String name = ctx.message().upgradeName();

        GasElement.tryBuyUpgrade(player, name); // não faz nada se inválido/sem pontos — silencioso de propósito

        PlayerSubbendingData data = player.getData(ModAttachments.SUBBENDINGS);
        Network.getNetworkHandler().sendToClient(
                new SyncGasProgressPacket(data.getPoints(SubbendingType.GAS),
                        data.getUnlockedUpgrades(SubbendingType.GAS).stream().toList()),
                player);
    }
}