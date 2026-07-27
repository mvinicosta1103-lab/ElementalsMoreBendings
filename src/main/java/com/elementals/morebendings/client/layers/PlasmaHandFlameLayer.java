package com.elementals.morebendings.client.layers;

import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaBoostState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PlasmaHandFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation SOUL_FIRE_0 =
            ResourceLocation.withDefaultNamespace("textures/block/soul_fire_0.png");
    private static final ResourceLocation SOUL_FIRE_1 =
            ResourceLocation.withDefaultNamespace("textures/block/soul_fire_1.png");

    public PlasmaHandFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        // Cliente não tem acesso direto ao ServerPlayer -- precisa de um jeito
        // de saber o estado (ver observação abaixo sobre sync pro cliente).
        if (!ClientPlasmaBoostCache.isActive(player.getUUID())) return;

        ResourceLocation tex = (player.tickCount / 3) % 2 == 0 ? SOUL_FIRE_0 : SOUL_FIRE_1;
        // ... resto igual ao esqueleto que já te passei (renderFlameOnArm nos dois braços)
    }
}