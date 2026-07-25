package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.client.ClientGasProgress;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;

/**
 * Servidor → cliente: estado atual da árvore do Gas pra esse jogador
 * (pontos disponíveis + nomes dos nós já comprados). Mandado depois de
 * qualquer {@link BuyGasUpgradePacket} e quando a {@code GasSkillTreeScreen}
 * abre.
 */
public record SyncGasProgressPacket(int points, List<String> unlockedUpgrades) {

    public static final StreamCodec<FriendlyByteBuf, SyncGasProgressPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncGasProgressPacket::points,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncGasProgressPacket::unlockedUpgrades,
            SyncGasProgressPacket::new
    );

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.SYNC_GAS_PROGRESS_ID);
    }

    public static void handle(PacketContext<SyncGasProgressPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.CLIENT);
        SyncGasProgressPacket msg = ctx.message();
        ClientGasProgress.update(msg.points(), msg.unlockedUpgrades());
    }
}