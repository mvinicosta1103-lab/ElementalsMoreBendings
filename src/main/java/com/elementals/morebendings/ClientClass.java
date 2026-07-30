package com.elementals.morebendings;

import com.elementals.morebendings.bending.earthsubbendings.bone.BoneSpikeEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassShardEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaShurikenEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.lava.MagmaSpikeVisualEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudBallEntityRenderer;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudSpikeVisualEntityRenderer;
import com.elementals.morebendings.bending.airsubbendings.mist.MistFogEntityRenderer;
import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionBoltEntityRenderer;
import com.elementals.morebendings.registry.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.elementals.morebendings.client.layers.PlasmaHandFlameLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;

public class ClientClass {

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CRYSTAL_SHARD.get(), CrystalShardEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BONE_SPIKE.get(), BoneSpikeEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.GLASS_SHARD.get(), GlassShardEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MIST_FOG.get(), MistFogEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.COMBUSTION_BOLT.get(), CombustionBoltEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.LAVA_SHURIKEN.get(), LavaShurikenEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGMA_SPIKE_VISUAL.get(), MagmaSpikeVisualEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MUD_BALL.get(), MudBallEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MUD_SPIKE_VISUAL.get(), MudSpikeVisualEntityRenderer::new);
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new PlasmaHandFlameLayer(renderer));
            }
        }
    }

    private ClientClass() {
    }
}