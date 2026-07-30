package com.elementals.morebendings.bending.earthsubbendings.lava;

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
 * Renderer do {@link LavaShurikenEntity}.
 *
 * === CAUSA DO CRASH "Lava Shuriken travando o jogo" ===
 * A entidade LAVA_SHURIKEN (ver ModEntities) nunca tinha um
 * EntityRenderer registrado -- não existia essa classe, e
 * ClientClass#onRegisterRenderers nunca chamava
 * event.registerEntityRenderer(ModEntities.LAVA_SHURIKEN.get(), ...).
 *
 * Quando uma entidade sem renderer registrado entra na tela,
 * EntityRenderDispatcher#getRenderer devolve null, e a primeira coisa
 * que o vanilla faz com esse retorno (EntityRenderDispatcher#shouldRender,
 * chamado de dentro de LevelRenderer#renderLevel) é invocar um método
 * nela sem checar null -- daí o
 * "Cannot invoke ... because entityrenderer is null" no crash report.
 * Isso acontece assim que a entidade é spawnada no client (onCall da
 * ability), então o jogo trava na hora que você usa lavaShuriken.
 *
 * Segue exatamente o padrão já usado por CrystalShardEntityRenderer/
 * GlassShardEntityRenderer neste addon: sem model/.json próprio, desenha
 * um "espeto" fino usando RenderUtils.drawCube com uma sprite do atlas
 * de blocos do próprio vanilla (aqui, magma_block, pra combinar com o
 * tema de lava), rotacionado pra apontar na direção do voo.
 */
public class LavaShurikenEntityRenderer extends EntityRenderer<LavaShurikenEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/magma_block");

    // Tint quente (laranja/vermelho) por cima da textura de magma.
    private static final float R = 1.0f, G = 0.55f, B = 0.15f, A = 1.0f;

    public LavaShurikenEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LavaShurikenEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        Matrix4f rot = new Matrix4f()
                .rotateY((float) Math.toRadians(-yRot))
                .rotateX((float) Math.toRadians(xRot))
                .scale(0.09f, 0.09f, 0.5f)
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
    public ResourceLocation getTextureLocation(LavaShurikenEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}