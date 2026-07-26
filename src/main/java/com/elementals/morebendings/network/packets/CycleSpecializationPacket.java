package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.airsubbendings.common.SpecializationCycle;
import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Cliente → servidor: "troca minha especialização ativa agora" (tecla
 * vinculada em {@code ModKeyMappings.CYCLE_SPECIALIZATION}).
 * <p>
 * Diferente de comprar/desbloquear no menu de upgrades -- só decide qual
 * das especializações já compradas produz efeito quando Gas Cloud/Heavy
 * Fog é lançado (ver {@link SpecializationCycle}). Cicla Gas e Mist
 * juntos no mesmo aperto: inofensivo pra quem só tem um dos dois, e
 * conveniente pra quem tem ambos (cada árvore guarda seu próprio estado
 * ativo, independente uma da outra).
 */
public record CycleSpecializationPacket() {

    public static final StreamCodec<FriendlyByteBuf, CycleSpecializationPacket> STREAM_CODEC =
            StreamCodec.unit(new CycleSpecializationPacket());

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(ModNetworking.CYCLE_SPECIALIZATION_ID);
    }

    public static void handle(PacketContext<CycleSpecializationPacket> ctx) {
        ModNetworking.expectSideOrThrow(ctx.side(), Side.SERVER);
        ServerPlayer player = ctx.sender();

        List<Component> switched = new ArrayList<>();

        if (GasElement.isGasBender(player)) {
            String newGas = SpecializationCycle.cycleGas(player);
            if (newGas != null) {
                switched.add(Component.translatable("upgrade.elementals." + newGas));
            }
        }

        if (MistElement.isMistBender(player)) {
            String newMist = SpecializationCycle.cycleMist(player);
            if (newMist != null) {
                switched.add(Component.translatable("upgrade.elementals." + newMist));
            }
        }

        if (switched.isEmpty()) {
            player.displayClientMessage(Component.translatable("upgrade.elementals.noSpecialization"), true);
            return;
        }

        MutableComponent message = Component.literal("");
        for (int i = 0; i < switched.size(); i++) {
            if (i > 0) {
                message.append(" / ");
            }
            message.append(switched.get(i));
        }
        player.displayClientMessage(message, true);
    }
}