package com.elementals.morebendings.bending.watersubbendings.ice;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.saperate.elementals.client.entities.utils.RenderUtils;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renderer do estilhaço de gelo -- mesmo esquema exato de {@code
 * CrystalShardEntityRenderer}: um "caixote" fino e alongado (vira um
 * espeto), texturizado com a textura vanilla de {@code block/packed_ice}
 * (sprite do atlas de blocos), rotacionado pra apontar na direção de voo.
 *
 * NOTA: se o estilhaço aparecer virado pro lado errado, inverta o sinal do
 * yRot/xRot abaixo -- mesma ressalva que {@code CrystalShardEntityRenderer}
 * já documenta.
 */
public class IceShardEntityRenderer extends EntityRenderer<IceShardEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/packed_ice");

    // Tint azul-esbranquiçado, translúcido -- "gelo", não pedra.
    private static final float R = 0.80f, G = 0.90f, B = 1.0f, A = 0.9f;

    public IceShardEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(IceShardEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        Matrix4f rot = new Matrix4f()
                .rotateY((float) Math.toRadians(-yRot))
                .rotateX((float) Math.toRadians(xRot))
                .scale(0.08f, 0.08f, 0.55f)
                .translate(0.0f, 0.0f, -0.5f); // centraliza o espeto no ponto da entidade

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());
        RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, R, G, B, A, TEXTURE,
                1.0f, rot, false, true, true);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(IceShardEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}