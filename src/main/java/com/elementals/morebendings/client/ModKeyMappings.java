package com.elementals.morebendings.client;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.network.packets.CastGasCloudPacket;
import com.elementals.morebendings.network.packets.CycleSpecializationPacket;
import com.elementals.morebendings.network.packets.ToggleFlyingPacket;
import com.mojang.blaze3d.platform.InputConstants;
import commonnetwork.api.Dispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.elementals.morebendings.network.packets.TogglePlasmaBoostPacket;

/**
 * Keybinds do addon. Só é carregada no lado cliente -- ver
 * {@code ElementalsMoreBendingsMod}, que só registra os listeners desta
 * classe quando {@code FMLEnvironment.dist == Dist.CLIENT} (igual
 * {@code ClientClass}, por causa de {@code KeyMapping}/{@code Minecraft}
 * serem classes client-only).
 */
public final class ModKeyMappings {

    private static final String CATEGORY = "key.categories." + Constants.MOD_ID;

    /** Liga/desliga o voo da sub-bending Flying (ver {@code FlyingAbility}). */
    public static final KeyMapping TOGGLE_FLYING = new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_flying",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            CATEGORY
    );

    /**
     * Solta o Gas Cloud direto, sem precisar trocar pro elemento Air/Gas
     * nem usar o slot numérico de habilidade padrão do mod base (ver
     * {@code GasCloudAbility}). Funciona em qualquer elemento ativo,
     * desde que o jogador já seja um Gas bender -- o servidor
     * (CastGasCloudPacket) confirma isso antes de disparar a habilidade.
     */
    public static final KeyMapping CAST_GAS_CLOUD = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_gas_cloud",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_PLASMA_BOOST = new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_plasma_boost",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_J,
            CATEGORY
    );

    /**
     * Troca qual especialização de Gas Cloud/Heavy Fog está ATIVA no
     * momento (Suffocate/Leak/Ignite ou Choke/Veil/Freeze) -- ver
     * {@code SpecializationCycle}. Comprar as três não é mais exclusivo;
     * esta tecla decide qual delas realmente age quando a habilidade é
     * lançada. Só afeta a árvore do elemento (Gas ou Mist) que o jogador
     * tem selecionado no momento -- ver {@code CycleSpecializationPacket}.
     */
    public static final KeyMapping CYCLE_SPECIALIZATION = new KeyMapping(
            "key." + Constants.MOD_ID + ".cycle_specialization",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            CATEGORY
    );

    /** Registrado no mod event bus via RegisterKeyMappingsEvent. */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_FLYING);
        event.register(CAST_GAS_CLOUD);
        event.register(CYCLE_SPECIALIZATION);
        event.register(TOGGLE_PLASMA_BOOST);
    }

    /**
     * Registrado via NeoForge.EVENT_BUS em ElementalsMoreBendingsMod. Roda
     * uma vez por tick de cliente; {@code consumeClick()} já garante que só
     * disparamos uma vez por aperto (não segura o pacote enquanto a tecla
     * fica pressionada).
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        while (TOGGLE_FLYING.consumeClick()) {
            Dispatcher.sendToServer(new ToggleFlyingPacket());
        }
        while (CAST_GAS_CLOUD.consumeClick()) {
            Dispatcher.sendToServer(new CastGasCloudPacket());
        }
        while (CYCLE_SPECIALIZATION.consumeClick()) {
            Dispatcher.sendToServer(new CycleSpecializationPacket());
        }
        while (TOGGLE_PLASMA_BOOST.consumeClick()) {
            Dispatcher.sendToServer(new TogglePlasmaBoostPacket());
        }
    }

    private ModKeyMappings() {
    }
}