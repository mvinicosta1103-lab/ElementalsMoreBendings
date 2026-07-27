package com.elementals.morebendings.client.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

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

        if (!ClientPlasmaBoostCache.isActive(player.getUUID())) return;

        ResourceLocation tex = (player.tickCount / 3) % 2 == 0 ? SOUL_FIRE_0 : SOUL_FIRE_1;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(tex));

        PlayerModel<AbstractClientPlayer> model = getParentModel();
        model.rightArm.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        model.leftArm.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}