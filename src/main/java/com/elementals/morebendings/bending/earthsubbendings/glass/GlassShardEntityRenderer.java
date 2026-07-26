package com.elementals.morebendings.bending.earthsubbendings.glass;

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
 * Renderer do estilhaço de vidro. Mesma receita da
 * {@code CrystalShardEntityRenderer}: nada de item model/bloco próprio,
 * desenha um "espeto" fino texturizado com uma sprite vanilla do atlas de
 * blocos (aqui, block/glass em vez de block/amethyst_block), rotacionado
 * pra apontar na direção de voo da entidade.
 *
 * NOTA: assim como no CrystalShardEntityRenderer, não dá pra abrir o jogo
 * pra conferir visualmente o sentido da rotação -- se aparecer "de lado"
 * ou virado pro lado errado, inverte o sinal do yRot/xRot abaixo.
 */
public class GlassShardEntityRenderer extends EntityRenderer<GlassShardEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/glass");

    // Tint quase neutro/levemente azulado, com um pouco de transparência pra
    // lembrar vidro em vez do bloco opaco cru.
    private static final float R = 0.85f, G = 0.92f, B = 0.95f, A = 0.85f;

    public GlassShardEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GlassShardEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // Alinha o eixo Z local (eixo de "comprimento" usado pelo RenderUtils.drawCube)
        // com a direção de voo do estilhaço, e depois achata/alonga pra virar um espeto.
        Matrix4f rot = new Matrix4f()
                .rotateY((float) Math.toRadians(-yRot))
                .rotateX((float) Math.toRadians(xRot))
                .scale(0.09f, 0.09f, 0.5f)
                .translate(0.0f, 0.0f, -0.5f); // centraliza o espeto no ponto da entidade

        // Mesma receita que CrystalShardEntityRenderer/MetalBulletEntityRenderer: a tex
        // é uma sprite do atlas de blocos, então o RenderType certo é
        // translucentMovingBlock(), não um RenderType de textura avulsa.
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
    public ResourceLocation getTextureLocation(GlassShardEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}