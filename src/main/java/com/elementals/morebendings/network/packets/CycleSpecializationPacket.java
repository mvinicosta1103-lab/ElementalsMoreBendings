package com.elementals.morebendings.network.packets;

import com.elementals.morebendings.bending.airsubbendings.common.SpecializationCycle;
import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import com.elementals.morebendings.network.ModNetworking;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cliente → servidor: "troca minha especialização ativa agora" (tecla
 * vinculada em {@code ModKeyMappings.CYCLE_SPECIALIZATION}).
 * <p>
 * Diferente de comprar/desbloquear no menu de upgrades -- só decide qual
 * das especializações já compradas produz efeito quando Gas Cloud/Heavy
 * Fog é lançado (ver {@link SpecializationCycle}).
 * <p>
 * Cicla SÓ a árvore do elemento que o jogador tem selecionado no momento
 * (o elemento "ativo" do mod base, trocado pelo Cycle Elements dele --
 * ver {@link Bender#getElement()}), nunca as duas juntas. Se o jogador
 * tiver Gas E Mist mas estiver com outro elemento selecionado (ou nem
 * Air), ou selecionado em um elemento sem nenhuma especialização
 * comprada ainda, nada é trocado e ele recebe um aviso.
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

        Bender bender = Bender.getBender(player);
        if (bender == null) {
            return;
        }
        Element active = bender.getElement();

        String newSpecialization;
        String messageKey;

        if (active == GasElement.get() && GasElement.isGasBender(player)) {
            newSpecialization = SpecializationCycle.cycleGas(player);
            messageKey = "upgrade.elementals.noSpecialization";
        } else if (active == MistElement.get() && MistElement.isMistBender(player)) {
            newSpecialization = SpecializationCycle.cycleMist(player);
            messageKey = "upgrade.elementals.noSpecialization";
        } else {
            newSpecialization = null;
            messageKey = "upgrade.elementals.cycleSpecializationWrongElement";
        }

        if (newSpecialization == null) {
            player.displayClientMessage(Component.translatable(messageKey), true);
            return;
        }

        player.displayClientMessage(Component.translatable("upgrade.elementals." + newSpecialization), true);
    }
}