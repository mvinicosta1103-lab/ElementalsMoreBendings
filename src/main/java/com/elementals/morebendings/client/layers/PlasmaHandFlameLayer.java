package com.elementals.morebendings.client.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PlasmaHandFlameLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    // Fogo normal do Minecraft. Troque para Blocks.SOUL_FIRE se quiser a
    // variante azul (soul fire) em vez do fogo laranja padrão.
    private static final BlockState FIRE_STATE = Blocks.FIRE.defaultBlockState();
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    private static final float FIRE_SCALE = 0.4F;

    public PlasmaHandFlameLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (!ClientPlasmaBoostCache.isActive(player.getUUID())) return;

        renderHandFire(poseStack, buffer, HumanoidArm.RIGHT);
        renderHandFire(poseStack, buffer, HumanoidArm.LEFT);
    }

    private void renderHandFire(PoseStack poseStack, MultiBufferSource buffer, HumanoidArm arm) {
        poseStack.pushPose();

        getParentModel().translateToHand(arm, poseStack);

        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        boolean isLeft = arm == HumanoidArm.LEFT;
        poseStack.translate((isLeft ? -1 : 1) / 16.0F, 0.125F, -0.625F);

        poseStack.scale(FIRE_SCALE, FIRE_SCALE, FIRE_SCALE);
        poseStack.translate(-0.5, -0.5, -0.5);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                FIRE_STATE, poseStack, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }
}