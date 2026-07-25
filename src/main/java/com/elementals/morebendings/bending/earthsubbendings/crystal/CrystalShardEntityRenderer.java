package com.elementals.morebendings.bending.earthsubbendings.crystal;

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
 * Renderer do estilhaço. Não usa item model nem .json de bloco — desenha um
 * "caixote" alongado na mão, texturizado com a textura vanilla de ametista
 * (block/amethyst_block, do atlas de blocos), igual o mod base faz pro
 * MetalBulletEntityRenderer (que usa block/iron_block num cubo pequeno).
 * A diferença é que aqui a caixa é bem mais fina/comprida (vira um espeto)
 * e é rotacionada pra apontar na direção que o estilhaço está voando, em
 * vez de ficar sempre parada olhando pro mesmo lado.
 *
 * NOTA: eu não consigo abrir o jogo pra conferir visualmente o sentido da
 * rotação — se o estilhaço aparecer "de lado" ou virado pro lado errado,
 * é só inverter o sinal do yRot/xRot abaixo (trocar toRadians(-yRot) por
 * toRadians(yRot), etc.) até apontar certo.
 */
public class CrystalShardEntityRenderer extends EntityRenderer<CrystalShardEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/amethyst_block");

    // Cor de tint aplicada sobre a textura (deixa mais roxo/claro que o bloco cru).
    private static final float R = 0.78f, G = 0.55f, B = 0.95f, A = 1.0f;

    public CrystalShardEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CrystalShardEntity entity, float entityYaw, float partialTick,
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

        // Mesma receita que o MetalBulletEntityRenderer do mod base usa: o tex é uma
        // sprite do atlas de blocos, então o RenderType certo é translucentMovingBlock(),
        // não um RenderType de textura avulsa (senão o jogo tenta abrir
        // "block/amethyst_block" como arquivo de textura próprio e não acha nada).
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
    public ResourceLocation getTextureLocation(CrystalShardEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}