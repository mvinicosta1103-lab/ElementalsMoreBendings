package com.elementals.morebendings;

import com.elementals.morebendings.bending.airsubbendings.common.CloudRootHealer;
import com.elementals.morebendings.bending.common.AbilityBindingHealer;
import com.elementals.morebendings.bending.airsubbendings.flying.FlyingAbility;
import com.elementals.morebendings.bending.airsubbendings.gas.GasLeakManager;
import com.elementals.morebendings.bending.earthsubbendings.bone.BloodProximityTracker;
import com.elementals.morebendings.bending.earthsubbendings.bone.BonePuppeteerManager;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalPrisonManager;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalSpikeManager;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalWallManager;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaArmorCombatHandler;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaFlowManager;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaPoolManager;
import com.elementals.morebendings.bending.earthsubbendings.lava.MagmaSpikeManager;
import com.elementals.morebendings.bending.earthsubbendings.lava.VolcanicEruptionManager;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudSpikeManager;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudTrapManager;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandQuicksandManager;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandTornadoManager;
import com.elementals.morebendings.bending.airsubbendings.atmosphere.PressureZoneManager;
import com.elementals.morebendings.bending.airsubbendings.mist.MistCloudManager;
import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaBoostCombatHandler;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalArmorManager;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalArmorSetManager;
import com.elementals.morebendings.registry.ModArmorMaterials;
import com.elementals.morebendings.registry.ModCreativeTabs;
import com.elementals.morebendings.registry.ModItems;
import com.elementals.morebendings.effects.MoreBendingsEffects;
import com.elementals.morebendings.client.ModKeyMappings;
import com.elementals.morebendings.client.layers.PlasmaFirstPersonFireHandler;
import com.elementals.morebendings.commands.MoreBendingCommand;
import com.elementals.morebendings.network.ModNetworking;
import com.elementals.morebendings.registry.ModAttachments;
import com.elementals.morebendings.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.elementals.morebendings.bending.airsubbendings.sound.EchoSenseManager;
import com.elementals.morebendings.bending.airsubbendings.sound.SilenceFieldManager;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantVineWallManager;
import com.elementals.morebendings.bending.watersubbendings.spirit.CurseMinionManager;
import com.elementals.morebendings.bending.watersubbendings.spirit.PurifyingWaterManager;


@Mod(Constants.MOD_ID)
public class ElementalsMoreBendingsMod {

    public ElementalsMoreBendingsMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        // Registries que precisam do mod bus (DeferredRegister).
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);

        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        MoreBendingsEffects.register(modEventBus);

        // ClientClass usa classes client-only (EntityRenderersEvent) — só registra
        // o listener se a gente realmente estiver rodando no cliente, senão o
        // servidor dedicado quebra ao tentar carregar essa classe.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientClass::onRegisterRenderers);
            modEventBus.addListener(ClientClass::onAddLayers);

            // Keybind de "ligar/desligar voo" (Flying) -- registro da tecla
            // em si precisa do mod bus (RegisterKeyMappingsEvent); o listener
            // que efetivamente lê a tecla e manda o pacote roda no bus de
            // jogo (ver bloco de NeoForge.EVENT_BUS abaixo).
            modEventBus.addListener(ModKeyMappings::register);
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

        // Desmancha os clusters de farpas de mudSpikes ativos depois do
        // tempo, devolvendo o terreno original -- ver MudSpikeManager.
        NeoForge.EVENT_BUS.addListener(MudSpikeManager::onServerTick);

        // Dirige os tornados de sandTornado ativos (giro/sucção/dano) --
        // ver SandTornadoManager. Mesmo esquema do MudTrapManager.
        NeoForge.EVENT_BUS.addListener(SandTornadoManager::onServerTick);

        // Desmancha as crateras de sandQuicksand ativas depois do tempo --
        // não depende do caster ficar agachado/por perto, ver
        // SandQuicksandManager (mesmo esquema de tick independente do
        // MudTrapManager/SandTornadoManager acima).
        NeoForge.EVENT_BUS.addListener(SandQuicksandManager::onServerTick);

        NeoForge.EVENT_BUS.addListener(PressureZoneManager::onServerTick);

        // Corrige sozinho, a cada login, qualquer Gas/Mist/Bone bender cujo nó
        // raiz (gasCloud/mistCloud) não esteja marcado como comprado --
        // sem isso a árvore de upgrades fica travada pra sempre (ver
        // CloudRootHealer). Substitui a necessidade de rodar
        // "/morebending grant <player> gas" manualmente como reparo.
        NeoForge.EVENT_BUS.addListener(CloudRootHealer::onPlayerLoggedIn);

        // Recalcula os 12 slots de tecla (boundAbilities) de cada jogador a
        // cada login -- corrige habilidades novas (ex: lavaFlow) que ficam
        // travadas em null pra quem já tinha o elemento desbloqueado antes
        // delas existirem. Ver AbilityBindingHealer para a causa raiz.
        NeoForge.EVENT_BUS.addListener(AbilityBindingHealer::onPlayerLoggedIn);

        // Verifica em background se algum Earth bender chegou perto o
        // suficiente de um Blood bender pra desbloquear o pré-requisito de
        // Bone Bending -- ver BloodProximityTracker.
        NeoForge.EVENT_BUS.addListener(BloodProximityTracker::onServerTick);

        // Esfria as poças de lavaPool ativas depois do tempo -- ver LavaPoolManager.
        NeoForge.EVENT_BUS.addListener(LavaPoolManager::onServerTick);

        // Cresce as faixas de lavaFlow ativas fileira por fileira e depois
        // esfria cada uma de uma vez -- ver LavaFlowManager.
        NeoForge.EVENT_BUS.addListener(LavaFlowManager::onServerTick);

        // Desmancha os grupos de espinhos de magmaSpike ativos depois do
        // tempo, devolvendo o terreno original -- ver MagmaSpikeManager.
        NeoForge.EVENT_BUS.addListener(MagmaSpikeManager::onServerTick);

        // Dirige as crateras de volcanicEruption ativas (núcleo de lava +
        // anel de espinhos, cada um com seu próprio tempo de reversão) --
        // ver VolcanicEruptionManager.
        NeoForge.EVENT_BUS.addListener(VolcanicEruptionManager::onServerTick);

        // Incendeia quem acerta um golpe corpo a corpo direto em quem
        // estiver com lavaArmor ativa -- ver LavaArmorCombatHandler.
        NeoForge.EVENT_BUS.addListener(LavaArmorCombatHandler::onIncomingDamage);

        // Aplica Náusea + Envenenamento em quem estiver dentro de uma nuvem
        // residual de gasLeak, exceto o próprio caster -- ver GasLeakManager.
        NeoForge.EVENT_BUS.addListener(GasLeakManager::onServerTick);

        // Dirige as névoas de Heavy Fog (mistCloud) ativas -- Cegueira +
        // Escuridão (+ especialização) tick a tick -- ver MistCloudManager.
        NeoForge.EVENT_BUS.addListener(MistCloudManager::onServerTick);

        // Estamina/partículas do voo da sub-bending Flying -- ver FlyingAbility.
        NeoForge.EVENT_BUS.addListener(FlyingAbility::onServerTick);
        // Limpa o UUID de quem desconecta voando -- ver FlyingAbility#onPlayerLoggedOut.
        NeoForge.EVENT_BUS.addListener(FlyingAbility::onPlayerLoggedOut);

        // Lê a tecla de ligar/desligar voo e manda o ToggleFlyingPacket pro
        // servidor -- ver ModKeyMappings. Só existe no cliente pelo mesmo
        // motivo do listener de renderers acima.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(ModKeyMappings::onClientTick);

            // Desenha o fogo de plasma nas mãos em PRIMEIRA pessoa -- o
            // PlasmaHandFlameLayer (registrado lá em cima via onAddLayers)
            // só cobre terceira pessoa, porque o braço em primeira pessoa é
            // desenhado por um caminho totalmente separado do jogo. Ver
            // PlasmaFirstPersonFireHandler.
            NeoForge.EVENT_BUS.addListener(PlasmaFirstPersonFireHandler::onRenderArm);
        }

        // Bônus de dano + queimada em qualquer golpe corpo a corpo enquanto
        // o Plasma Boost estiver ativo -- roda no servidor, então sem
        // checagem de Dist.CLIENT. Ver PlasmaBoostCombatHandler.
        NeoForge.EVENT_BUS.addListener(PlasmaBoostCombatHandler::onIncomingDamage);

        NeoForge.EVENT_BUS.addListener(PlantVineWallManager::onServerTick);

        // Expira as zonas de purifyingWater ativas e processa quem estiver
        // pego nelas -- ver PurifyingWaterManager.
        NeoForge.EVENT_BUS.addListener(PurifyingWaterManager::onServerTick);

        // Força o retarget contínuo de quem estiver amaldiçoado por
        // curseMinion -- ver CurseMinionManager.
        NeoForge.EVENT_BUS.addListener(CurseMinionManager::onServerTick);

        // Move os fantoches ativos de bonePuppeteer na direção que o caster
        // está olhando, tick a tick, e libera a IA quando a possessão acaba
        // -- ver BonePuppeteerManager.
        NeoForge.EVENT_BUS.addListener(BonePuppeteerManager::onServerTick);

        // Aplica Lentidão em quem entra na zona de silenceField (Sound)
        // enquanto o toggle estiver ativo -- ver SilenceFieldManager.
        NeoForge.EVENT_BUS.addListener(SilenceFieldManager::onServerTick);

        // Dispara o pulso periódico de echoSense (Sound), revelando
        // entidades próximas via partículas -- ver EchoSenseManager.
        NeoForge.EVENT_BUS.addListener(EchoSenseManager::onServerTick);

        // Desmancha os clusters de espinhos de crystalSpike ativos depois do
        // tempo, devolvendo o terreno original -- ver CrystalSpikeManager.
        NeoForge.EVENT_BUS.addListener(CrystalSpikeManager::onServerTick);

        // Estilhaça as paredes de crystalWall ativas depois do tempo -- ver
        // CrystalWallManager. Mesmo esquema do PlantVineWallManager.
        NeoForge.EVENT_BUS.addListener(CrystalWallManager::onServerTick);

        // Estilhaça as gaiolas de crystalPrison ativas depois do tempo,
        // devolvendo o terreno original -- ver CrystalPrisonManager.
        NeoForge.EVENT_BUS.addListener(CrystalPrisonManager::onServerTick);

        NeoForge.EVENT_BUS.addListener(CrystalArmorManager::onServerTick);

        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer sp) CrystalArmorSetManager.restoreOnLogout(sp);
        });

        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer sp) CrystalArmorSetManager.reapplyAfterRespawn(sp);
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        CommonClass.init();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        MoreBendingCommand.register(event.getDispatcher());
    }
}