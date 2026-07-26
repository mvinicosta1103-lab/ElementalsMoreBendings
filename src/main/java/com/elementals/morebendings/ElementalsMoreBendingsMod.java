package com.elementals.morebendings;

import com.elementals.morebendings.bending.earthsubbendings.bone.BloodProximityTracker;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaPoolManager;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudTrapManager;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandTornadoManager;
import com.elementals.morebendings.commands.MoreBendingCommand;
import com.elementals.morebendings.network.ModNetworking;
import com.elementals.morebendings.registry.ModAttachments;
import com.elementals.morebendings.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public class ElementalsMoreBendingsMod {

    public ElementalsMoreBendingsMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        // Registries que precisam do mod bus (DeferredRegister).
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);

        // ClientClass usa classes client-only (EntityRenderersEvent) — só registra
        // o listener se a gente realmente estiver rodando no cliente, senão o
        // servidor dedicado quebra ao tentar carregar essa classe.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientClass::onRegisterRenderers);
        }

        // IMPORTANTE: tem que ser aqui no construtor, não em CommonClass.init()
        // (FMLCommonSetupEvent). A commonnetwork registra os pacotes de verdade
        // no NeoForge escutando RegisterPayloadHandlersEvent, que dispara na
        // fase de registro — ANTES do common setup. Se ModNetworking.register()
        // rodar depois disso, os pacotes (SyncGasProgressPacket etc.) nunca são
        // registrados e qualquer envio/recebimento falha silenciosamente.
        ModNetworking.register();

        // RegisterCommandsEvent é disparado no bus "de jogo", não no mod bus.
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Dirige as armadilhas de mudTrap ativas (afundar/sufocar/soltar) --
        // ver MudTrapManager. Independente do sistema de onTick do mod base.
        NeoForge.EVENT_BUS.addListener(MudTrapManager::onServerTick);

        // Dirige os tornados de sandTornado ativos (giro/sucção/dano) --
        // ver SandTornadoManager. Mesmo esquema do MudTrapManager.
        NeoForge.EVENT_BUS.addListener(SandTornadoManager::onServerTick);

        // Verifica em background se algum Earth bender chegou perto o
        // suficiente de um Blood bender pra desbloquear o pré-requisito de
        // Bone Bending -- ver BloodProximityTracker.
        NeoForge.EVENT_BUS.addListener(BloodProximityTracker::onServerTick);

        // Esfria as poças de lavaPool ativas depois do tempo -- ver LavaPoolManager.
        NeoForge.EVENT_BUS.addListener(LavaPoolManager::onServerTick);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        CommonClass.init();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        MoreBendingCommand.register(event.getDispatcher());
    }
}