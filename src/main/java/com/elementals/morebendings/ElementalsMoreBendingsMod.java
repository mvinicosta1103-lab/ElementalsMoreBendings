package com.elementals.morebendings;

import com.elementals.morebendings.commands.MoreBendingCommand;
import com.elementals.morebendings.network.ModNetworking;
import com.elementals.morebendings.registry.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public class ElementalsMoreBendingsMod {

    public ElementalsMoreBendingsMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        // Registries que precisam do mod bus (DeferredRegister).
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // IMPORTANTE: tem que ser aqui no construtor, não em CommonClass.init()
        // (FMLCommonSetupEvent). A commonnetwork registra os pacotes de verdade
        // no NeoForge escutando RegisterPayloadHandlersEvent, que dispara na
        // fase de registro — ANTES do common setup. Se ModNetworking.register()
        // rodar depois disso, os pacotes (SyncGasProgressPacket etc.) nunca são
        // registrados e qualquer envio/recebimento falha silenciosamente.
        ModNetworking.register();

        // RegisterCommandsEvent é disparado no bus "de jogo", não no mod bus.
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        CommonClass.init();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        MoreBendingCommand.register(event.getDispatcher());
    }
}