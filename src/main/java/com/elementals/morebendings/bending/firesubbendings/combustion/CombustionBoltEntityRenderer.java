package com.elementals.morebendings.bending.firesubbendings.combustion;

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
 * Renderer do {@link CombustionBoltEntity}. Mesma técnica de
 * {@code CrystalShardEntityRenderer} (cubo pequeno da textura de um bloco
 * vanilla, tintado) -- aqui usa a textura de magma (já é uma textura
 * "brilhante"/quente no atlas de blocos) tintada num laranja-vermelho
 * intenso pra vender a ideia de bola de fogo condensada.
 *
 * Igual ao aviso do Crystal: eu não consigo abrir o jogo pra conferir a
 * rotação visualmente -- se aparecer "de lado", inverte o sinal do
 * yRot/xRot abaixo.
 */
public class CombustionBoltEntityRenderer extends EntityRenderer<CombustionBoltEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/magma");

    private static final float R = 1.0f, G = 0.35f, B = 0.1f, A = 1.0f;

    public CombustionBoltEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CombustionBoltEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        Matrix4f rot = new Matrix4f()
                .rotateY((float) Math.toRadians(-yRot))
                .rotateX((float) Math.toRadians(xRot))
                .scale(0.22f, 0.22f, 0.22f)
                .translate(0.0f, 0.0f, -0.5f);

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
    public ResourceLocation getTextureLocation(CombustionBoltEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}