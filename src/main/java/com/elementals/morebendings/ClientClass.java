package com.elementals.morebendings;

import com.elementals.morebendings.bending.earthsubbendings.bone.BoneSpikeEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalSpikeVisualEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassShardEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaGeyserVisualEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaShurikenEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaSurfWaveVisualEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.lava.MagmaSpikeVisualEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudBallEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudSpikeVisualEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudSurgeChunkEntityRenderer;
import com.elementals.morebendings.bending.airsubbendings.mist.MistFogEntityRenderer;
import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionBoltEntityRenderer;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantThornVolleyEntityRenderer;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantVineGraspVisualEntityRenderer;
import com.elementals.morebendings.bending.watersubbendings.ice.IceShardEntityRenderer;
import com.elementals.morebendings.bending.watersubbendings.ice.IceSpikeVisualEntityRenderer;
import com.elementals.morebendings.registry.ModBlocks;
import com.elementals.morebendings.registry.ModEntities;
import com.elementals.morebendings.client.layers.CrystalArmorRenderLayer;
import com.elementals.morebendings.client.layers.AvatarStateEyesLayer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import com.elementals.morebendings.client.layers.PlasmaHandFlameLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;

public class ClientClass {

    // Cor "padrão" de água do vanilla (o mesmo tint aplicado ao bioma
    // plains) -- aplicada por cima da sprite water_still no bloco
    // fantasma do anel de Água (ver ModBlocks.WATER_RING_DISPLAY). Sem
    // isso a sprite crua fica bem mais pálida/acinzentada do que a água
    // de verdade, que só fica azul por causa desse tint.
    private static final int WATER_RING_TINT = 0x3F76E4;

    /**
     * Registra o tint azul do bloco fantasma usado pelo anel de Água do
     * Avatar State -- ver {@link ModBlocks#WATER_RING_DISPLAY} e
     * {@code AvatarStateManager}. Sem isso o model (que usa o parent
     * {@code minecraft:block/leaves} só pra ganhar tintindex em cada
     * face) renderiza sem cor nenhuma aplicada.
     */
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> WATER_RING_TINT,
                ModBlocks.WATER_RING_DISPLAY.get());
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CRYSTAL_SHARD.get(), CrystalShardEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BONE_SPIKE.get(), BoneSpikeEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.GLASS_SHARD.get(), GlassShardEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MIST_FOG.get(), MistFogEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.COMBUSTION_BOLT.get(), CombustionBoltEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.LAVA_SHURIKEN.get(), LavaShurikenEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGMA_SPIKE_VISUAL.get(), MagmaSpikeVisualEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.LAVA_GEYSER_VISUAL.get(), LavaGeyserVisualEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.LAVA_SURF_WAVE.get(), LavaSurfWaveVisualEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CRYSTAL_SPIKE_VISUAL.get(), CrystalSpikeVisualEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ICE_SHARD.get(), IceShardEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ICE_SPIKE_VISUAL.get(), IceSpikeVisualEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MUD_BALL.get(), MudBallEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MUD_SPIKE_VISUAL.get(), MudSpikeVisualEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MUD_SURGE_CHUNK.get(), MudSurgeChunkEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.PLANT_THORN.get(), PlantThornVolleyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.PLANT_VINE_GRASP.get(), PlantVineGraspVisualEntityRenderer::new);
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new PlasmaHandFlameLayer(renderer));
                renderer.addLayer(new CrystalArmorRenderLayer(renderer));
                renderer.addLayer(new AvatarStateEyesLayer(renderer));
            }
        }
    }

    private ClientClass() {
    }
}