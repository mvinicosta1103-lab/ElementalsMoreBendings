package com.elementals.morebendings.bending.watersubbendings.plant;

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
 * Renderer do espinho — mesmo esquema de {@code CrystalShardEntityRenderer}:
 * "caixote" alongado, texturizado com uma sprite do atlas de blocos
 * (block/cactus_side, textura vanilla com padrão de espinhos), rotacionado
 * pra apontar na direção de voo.
 *
 * NOTA: mesma ressalva do CrystalShardEntityRenderer -- não consigo abrir o
 * jogo pra conferir visualmente a rotação. Se aparecer de lado/virado
 * errado, inverte o sinal do yRot/xRot abaixo.
 */
public class PlantThornVolleyEntityRenderer extends EntityRenderer<PlantThornVolleyEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/cactus_side");

    // Tint verde-escuro sobre a textura de cacto, pra parecer espinho/vinha, não cacto de verdade.
    private static final float R = 0.35f, G = 0.55f, B = 0.25f, A = 1.0f;

    public PlantThornVolleyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PlantThornVolleyEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        Matrix4f rot = new Matrix4f()
                .rotateY((float) Math.toRadians(-yRot))
                .rotateX((float) Math.toRadians(xRot))
                .scale(0.08f, 0.08f, 0.45f)
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
    public ResourceLocation getTextureLocation(PlantThornVolleyEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}