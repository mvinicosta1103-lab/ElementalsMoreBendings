package com.elementals.morebendings.client;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.network.packets.ToggleFlyingPacket;
import com.mojang.blaze3d.platform.InputConstants;
import commonnetwork.api.Dispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

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
            InputConstants.KEY_V,
            CATEGORY
    );

    /** Registrado no mod event bus via RegisterKeyMappingsEvent. */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_FLYING);
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
    }

    private ModKeyMappings() {
    }
}