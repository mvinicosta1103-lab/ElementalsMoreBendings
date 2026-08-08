package com.elementals.morebendings.client;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.network.packets.CastGasCloudPacket;
import com.elementals.morebendings.network.packets.CastGasSuffocatePacket;
import com.elementals.morebendings.network.packets.CastGasLeakPacket;
import com.elementals.morebendings.network.packets.CastGasIgnitePacket;
import com.elementals.morebendings.network.packets.CastGasJetPacket;
import com.elementals.morebendings.network.packets.CastGasMiasmaPacket;
import com.elementals.morebendings.network.packets.CastGasCorrosiveMistPacket;
import com.elementals.morebendings.network.packets.CastCombustionBlastPacket;
import com.elementals.morebendings.network.packets.CycleSpecializationPacket;
import com.elementals.morebendings.network.packets.ToggleFlyingPacket;
import com.mojang.blaze3d.platform.InputConstants;
import commonnetwork.api.Dispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.elementals.morebendings.network.packets.TogglePlasmaBoostPacket;
import com.elementals.morebendings.network.packets.ToggleAvatarStatePacket;
import com.elementals.morebendings.network.packets.ToggleFireRingPacket;
import com.elementals.morebendings.network.packets.ToggleWaterRingPacket;
import com.elementals.morebendings.network.packets.ToggleEarthRingPacket;
import com.elementals.morebendings.network.packets.ToggleAirRingPacket;
import com.elementals.morebendings.network.packets.CycleAvatarBendingPacket;
import com.elementals.morebendings.network.packets.CastAvatarBendingGrantPacket;
import com.elementals.morebendings.network.packets.CastAvatarBendingRemovePacket;

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

    /**
     * Solta o Suffocate direto, sem precisar trocar pro elemento Gas nem
     * usar o slot numérico de habilidade padrão do mod base (ver
     * {@code GasSuffocateAbility}). Antes esta e as outras habilidades de
     * Gas abaixo dependiam só do slot numérico compartilhado do mod base,
     * o que fazia todas caírem na mesma tecla (R) quando não configuradas
     * manualmente -- agora cada uma tem sua própria tecla dedicada, igual
     * o Gas Cloud já tinha.
     */
    public static final KeyMapping CAST_GAS_SUFFOCATE = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_gas_suffocate",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_J,
            CATEGORY
    );

    /** Solta o Gas Leak direto (ver {@code GasLeakAbility}). */
    public static final KeyMapping CAST_GAS_LEAK = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_gas_leak",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            CATEGORY
    );

    /** Solta o Gas Ignite direto (ver {@code GasIgniteAbility}). */
    public static final KeyMapping CAST_GAS_IGNITE = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_gas_ignite",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_L,
            CATEGORY
    );

    /** Solta o Gas Jet direto (ver {@code GasPropulsionAbility}). */
    public static final KeyMapping CAST_GAS_JET = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_gas_jet",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_N,
            CATEGORY
    );

    /** Solta o Miasma direto (ver {@code GasMiasmaAbility}). */
    public static final KeyMapping CAST_GAS_MIASMA = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_gas_miasma",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
            CATEGORY
    );

    /** Solta o Corrosive Mist direto (ver {@code CorrosiveGasAbility}). */
    public static final KeyMapping CAST_GAS_CORROSIVE_MIST = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_gas_corrosive_mist",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_U,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_PLASMA_BOOST = new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_plasma_boost",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            CATEGORY
    );

    /**
     * Começa a focar o Combustion Blast direto, sem precisar trocar pro
     * elemento Fire/Combustion nem usar o slot numérico de habilidade
     * padrão do mod base (ver {@code CombustionExplosionAbility}).
     * Funciona em qualquer elemento ativo, desde que o jogador já seja um
     * Combustion bender com o nó raiz comprado -- o servidor
     * (CastCombustionBlastPacket) confirma isso antes de iniciar o foco.
     * Diferente do Gas Cloud, essa tecla só INICIA a canalização; soltar
     * (clique esquerdo) ou cancelar (clique direito) continua sendo feito
     * com o mouse, igual qualquer outra ability canalizada.
     */
    public static final KeyMapping CAST_COMBUSTION_BLAST = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_combustion_blast",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
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

    /**
     * Liga/desliga o Avatar State (ver {@code AvatarStateManager}) -- só
     * funciona se o jogador já dominar os 4 elementos-base; o servidor
     * confirma isso antes de conceder o boost + as bendings.
     */
    public static final KeyMapping TOGGLE_AVATAR_STATE = new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_avatar_state",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_P,
            CATEGORY
    );

    /** Liga/desliga individualmente o anel de Fogo do Avatar State (ver {@code AvatarStateManager#toggleRing}). */
    public static final KeyMapping TOGGLE_RING_FIRE = new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_ring_fire",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_I,
            CATEGORY
    );

    /** Liga/desliga individualmente o anel de Água do Avatar State (ver {@code AvatarStateManager#toggleRing}). */
    public static final KeyMapping TOGGLE_RING_WATER = new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_ring_water",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_T,
            CATEGORY
    );

    /** Liga/desliga individualmente o anel de Terra do Avatar State (ver {@code AvatarStateManager#toggleRing}). */
    public static final KeyMapping TOGGLE_RING_EARTH = new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_ring_earth",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Y,
            CATEGORY
    );

    /** Liga/desliga individualmente o anel de Ar do Avatar State (ver {@code AvatarStateManager#toggleRing}). */
    public static final KeyMapping TOGGLE_RING_AIR = new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_ring_air",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_C,
            CATEGORY
    );

    /**
     * Avança o elemento-base selecionado pra conceder/remover (ver
     * {@code AvatarBendingSelection}, {@code CycleAvatarBendingPacket}).
     */
    public static final KeyMapping CYCLE_AVATAR_BENDING = new KeyMapping(
            "key." + Constants.MOD_ID + ".cycle_avatar_bending",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            CATEGORY
    );

    /**
     * Concede o elemento-base selecionado ({@code AvatarBendingSelection})
     * a quem estiver mirando -- só funciona no Avatar State (ver {@code
     * AvatarBendingGrantAbility}).
     */
    public static final KeyMapping CAST_AVATAR_BENDING_GRANT = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_avatar_bending_grant",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Z,
            CATEGORY
    );

    /**
     * Remove o elemento-base selecionado ({@code AvatarBendingSelection})
     * de quem estiver mirando -- só funciona no Avatar State (ver {@code
     * AvatarBendingRemoveAbility}).
     */
    public static final KeyMapping CAST_AVATAR_BENDING_REMOVE = new KeyMapping(
            "key." + Constants.MOD_ID + ".cast_avatar_bending_remove",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            CATEGORY
    );

    /** Registrado no mod event bus via RegisterKeyMappingsEvent. */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_FLYING);
        event.register(CAST_GAS_CLOUD);
        event.register(CAST_GAS_SUFFOCATE);
        event.register(CAST_GAS_LEAK);
        event.register(CAST_GAS_IGNITE);
        event.register(CAST_GAS_JET);
        event.register(CAST_GAS_MIASMA);
        event.register(CAST_GAS_CORROSIVE_MIST);
        event.register(CYCLE_SPECIALIZATION);
        event.register(TOGGLE_PLASMA_BOOST);
        event.register(CAST_COMBUSTION_BLAST);
        event.register(TOGGLE_AVATAR_STATE);
        event.register(TOGGLE_RING_FIRE);
        event.register(TOGGLE_RING_WATER);
        event.register(TOGGLE_RING_EARTH);
        event.register(TOGGLE_RING_AIR);
        event.register(CYCLE_AVATAR_BENDING);
        event.register(CAST_AVATAR_BENDING_GRANT);
        event.register(CAST_AVATAR_BENDING_REMOVE);
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
        while (CAST_GAS_SUFFOCATE.consumeClick()) {
            Dispatcher.sendToServer(new CastGasSuffocatePacket());
        }
        while (CAST_GAS_LEAK.consumeClick()) {
            Dispatcher.sendToServer(new CastGasLeakPacket());
        }
        while (CAST_GAS_IGNITE.consumeClick()) {
            Dispatcher.sendToServer(new CastGasIgnitePacket());
        }
        while (CAST_GAS_JET.consumeClick()) {
            Dispatcher.sendToServer(new CastGasJetPacket());
        }
        while (CAST_GAS_MIASMA.consumeClick()) {
            Dispatcher.sendToServer(new CastGasMiasmaPacket());
        }
        while (CAST_GAS_CORROSIVE_MIST.consumeClick()) {
            Dispatcher.sendToServer(new CastGasCorrosiveMistPacket());
        }
        while (CYCLE_SPECIALIZATION.consumeClick()) {
            Dispatcher.sendToServer(new CycleSpecializationPacket());
        }
        while (TOGGLE_PLASMA_BOOST.consumeClick()) {
            Dispatcher.sendToServer(new TogglePlasmaBoostPacket());
        }
        while (CAST_COMBUSTION_BLAST.consumeClick()) {
            Dispatcher.sendToServer(new CastCombustionBlastPacket());
        }
        while (TOGGLE_AVATAR_STATE.consumeClick()) {
            Dispatcher.sendToServer(new ToggleAvatarStatePacket());
        }
        while (TOGGLE_RING_FIRE.consumeClick()) {
            Dispatcher.sendToServer(new ToggleFireRingPacket());
        }
        while (TOGGLE_RING_WATER.consumeClick()) {
            Dispatcher.sendToServer(new ToggleWaterRingPacket());
        }
        while (TOGGLE_RING_EARTH.consumeClick()) {
            Dispatcher.sendToServer(new ToggleEarthRingPacket());
        }
        while (TOGGLE_RING_AIR.consumeClick()) {
            Dispatcher.sendToServer(new ToggleAirRingPacket());
        }
        while (CYCLE_AVATAR_BENDING.consumeClick()) {
            Dispatcher.sendToServer(new CycleAvatarBendingPacket());
        }
        while (CAST_AVATAR_BENDING_GRANT.consumeClick()) {
            Dispatcher.sendToServer(new CastAvatarBendingGrantPacket());
        }
        while (CAST_AVATAR_BENDING_REMOVE.consumeClick()) {
            Dispatcher.sendToServer(new CastAvatarBendingRemovePacket());
        }
    }

    private ModKeyMappings() {
    }
}