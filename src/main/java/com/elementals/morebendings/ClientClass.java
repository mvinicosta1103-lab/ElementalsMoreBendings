package com.elementals.morebendings;

import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardEntityRenderer;
import com.elementals.morebendings.registry.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Ponto de entrada pra tudo que só pode existir no lado cliente (renderers,
 * models, etc). Só é referenciada a partir de ElementalsMoreBendingsMod
 * quando {@code FMLEnvironment.dist == Dist.CLIENT} — em servidor dedicado
 * essa classe nunca chega a ser carregada, então pode usar
 * EntityRenderersEvent (classe client-only do NeoForge) sem medo de crashar
 * o server.
 */
public class ClientClass {

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CRYSTAL_SHARD.get(), CrystalShardEntityRenderer::new);
    }

    private ClientClass() {
    }
}