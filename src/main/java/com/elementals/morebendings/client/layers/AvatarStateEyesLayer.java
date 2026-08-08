package com.elementals.morebendings.client.layers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.saperate.elementals.client.entities.utils.RenderUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Olhos brilhantes de quem está no Avatar State (ver
 * {@code AvatarStateManager}) -- duas cubozinhos minúsculos, luminosos
 * (luz forçada no máximo, ignora a luz do ambiente, igual o truque de
 * {@code PlasmaFireRenderer#FULL_BRIGHT}), plantados na posição
 * aproximada dos olhos do modelo do jogador. Textura vem do próprio atlas
 * de blocos (glowstone), mesma técnica de "cubo customizado" que
 * {@code CrystalArmorRenderLayer} usa pra casca de cristal -- não precisa
 * bater com o UV da skin porque não é a skin, é um cubo à parte por cima.
 * <p>
 * Posição/tamanho é chute inicial a partir das dimensões padrão da cabeça
 * do modelo Steve (0.5x0.5x0.5, pivot na base do pescoço); ajuste em jogo
 * (F5 terceira pessoa) se sobrar/faltar alinhamento com o rosto.
 */
public class AvatarStateEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/glowstone");

    private static final int FULL_BRIGHT = 15728880; // LightTexture.FULL_BRIGHT

    // Cor do brilho -- branco-azulado, meio "Estado Espiritual".
    private static final float EYE_R = 0.85f, EYE_G = 0.95f, EYE_B = 1.0f, EYE_A = 1.0f;

    // Olho: bem achatado (quase uma pastilha), largura/altura pequenas.
    private static final float EYE_WIDTH = 0.075f;
    private static final float EYE_HEIGHT = 0.05f;
    private static final float EYE_DEPTH = 0.035f;

    // Deslocamento a partir do pivot da cabeça (base do pescoço): sobe até
    // a altura do rosto, avança até quase a superfície frontal da cabeça,
    // e abre pros dois lados pra cada olho.
    private static final float EYE_X_OFFSET = 0.085f;
    private static final float EYE_Y_OFFSET = -0.42f;
    private static final float EYE_Z_OFFSET = -0.235f;

    public AvatarStateEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (!ClientAvatarStateCache.isActive(player.getUUID())) return;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());
        PlayerModel<AbstractClientPlayer> model = getParentModel();

        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        drawEye(poseStack, vertexConsumer, -EYE_X_OFFSET);
        drawEye(poseStack, vertexConsumer, EYE_X_OFFSET);
        poseStack.popPose();

        RenderSystem.disableBlend();
    }

    private void drawEye(PoseStack poseStack, VertexConsumer buffer, float xOffset) {
        poseStack.pushPose();
        poseStack.translate(xOffset, EYE_Y_OFFSET, EYE_Z_OFFSET);
        poseStack.scale(EYE_WIDTH, EYE_HEIGHT, EYE_DEPTH);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        Matrix4f identity = new Matrix4f();
        RenderUtils.drawCube(buffer, poseStack, FULL_BRIGHT, EYE_R, EYE_G, EYE_B, EYE_A, TEXTURE,
                1.0f, identity, false, true, true);
        poseStack.popPose();
    }
}