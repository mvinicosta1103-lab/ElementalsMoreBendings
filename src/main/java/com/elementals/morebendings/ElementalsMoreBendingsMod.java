package com.elementals.morebendings;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Ponto de entrada do mod. Sem esta classe anotada com @Mod, o NeoForge
 * nunca chega a instanciar nada deste jar e o CommonClass.init() nunca
 * roda.
 */
@Mod(Constants.MOD_ID)
public class ElementalsMoreBendingsMod {

    public ElementalsMoreBendingsMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        CommonClass.init();
    }
}
