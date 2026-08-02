package com.elementals.morebendings.client.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Armadura" improvisada da crystalArmor -- o mod base não tem slot de
 * armadura de verdade pra sub-bendings, então cobrimos o PlayerModel com
 * placas de bloco de ametista ancoradas na pose de cada ModelPart, mesma
 * técnica de {@link PlasmaFireRenderer#renderHand}. A cabeça fica de fora
 * de propósito -- só um colar fino na base do pescoço -- pra não tampar os
 * olhos do jogador.
 *
 * Os offsets/escalas abaixo são chute inicial; ajuste em jogo (F3+B ajuda a
 * ver a hitbox/pivots de cada parte) até as placas encaixarem sem cravar
 * demais no corpo nem flutuar longe dele.
 */
public class CrystalArmorRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final BlockState PLATE_STATE = Blocks.AMETHYST_BLOCK.defaultBlockState();
    private static final int FULL_BRIGHT = 15728880; // LightTexture.FULL_BRIGHT

    public CrystalArmorRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (!ClientCrystalArmorCache.isActive(player.getUUID())) return;

        PlayerModel<AbstractClientPlayer> model = getParentModel();

        renderPlate(poseStack, buffer, model.body, 0.60f, 0f, 0.20f, 0f, 1.30f);
        renderPlate(poseStack, buffer, model.rightArm, 0.28f, 0f, 0.20f, 0f, 1.0f);
        renderPlate(poseStack, buffer, model.leftArm, 0.28f, 0f, 0.20f, 0f, 1.0f);
        renderPlate(poseStack, buffer, model.rightLeg, 0.30f, 0f, 0.20f, 0f, 1.0f);
        renderPlate(poseStack, buffer, model.leftLeg, 0.30f, 0f, 0.20f, 0f, 1.0f);

        renderCollar(poseStack, buffer, model.head);
    }

    /** @param heightMult multiplicador extra de altura (ex: torso mais alto que os membros) */
    private void renderPlate(PoseStack poseStack, MultiBufferSource buffer, ModelPart part,
                             float scale, float xOffset, float yOffset, float zOffset, float heightMult) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        poseStack.translate(xOffset, yOffset, zOffset);
        poseStack.scale(scale, scale * heightMult, scale);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                PLATE_STATE, poseStack, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private void renderCollar(PoseStack poseStack, MultiBufferSource buffer, ModelPart head) {
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        // Pivot da cabeça já fica na base do pescoço -- descer um pouco pra
        // sentar sobre os ombros, bem abaixo da linha dos olhos.
        poseStack.translate(0f, 0.05f, 0f);
        poseStack.scale(0.56f, 0.12f, 0.56f);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                PLATE_STATE, poseStack, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}