package com.elementals.morebendings.bending.earthsubbendings.lava;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.saperate.elementals.client.entities.utils.RenderUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Renderer de {@link LavaSurfWaveVisualEntity} -- uma onda de lava rente
 * ao chão: achatada debaixo/na frente dos pés do jogador e curvando pra
 * cima ATRÁS dele (a "crista", na direção oposta ao avanço -- é a onda
 * empurrando o jogador, não uma esteira arrastando atrás). Mesma técnica
 * de múltiplos cubos com offset de {@code MudSurgeChunkEntityRenderer},
 * orientada pelo yaw da entidade (ver {@link
 * LavaSurfWaveVisualEntity#followPlayer}). Pulsa levemente com o tempo
 * (seno determinístico por seed+idade) pra não ficar um bloco estático
 * grudado nos pés.
 */
public class LavaSurfWaveVisualEntityRenderer extends EntityRenderer<LavaSurfWaveVisualEntity> {

    /** Reaproveita a mesma textura de magma usada pelo resto da árvore de lava. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("elementalsmorebendings", "textures/models/magma/magma_texture.png");

    private static final float BASE_R = 0.90f, BASE_G = 0.32f, BASE_B = 0.10f, BASE_A = 0.95f;
    private static final float CREST_R = 1.00f, CREST_G = 0.70f, CREST_B = 0.25f, CREST_A = 0.90f;

    /**
     * Cada linha: offsetX, offsetY, offsetZ (relativo aos pés -- Z negativo
     * = atrás do jogador, na direção contrária ao avanço), largura, altura,
     * comprimento, e um fator de tint 0..1 (0 = cor base/escura, 1 = cor de
     * crista/clara) usado pra interpolar entre BASE_* e CREST_*.
     */
    private static final float[][] SEGMENTS = {
            {0.00f, 0.02f, 0.10f, 1.15f, 0.10f, 0.55f, 0.0f},   // corpo achatado debaixo/na frente dos pés
            {0.00f, 0.12f, -0.30f, 0.95f, 0.22f, 0.45f, 0.5f},  // meio, começando a curvar/subir atrás
            {0.00f, 0.32f, -0.55f, 0.70f, 0.30f, 0.30f, 1.0f},  // crista, mais alta e clara -- a "espuma"
    };

    public LavaSurfWaveVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LavaSurfWaveVisualEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int seed = entity.getSeed();
        float age = entity.tickCount + partialTick;
        float bob = 0.02f * (float) Math.sin((age + (seed % 20)) * 0.5);

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        poseStack.pushPose();
        // 180 - yaw pra Z negativo (offsets "atrás") ficar de fato atrás da direção em que o jogador olha,
        // mesma convenção que MudSurgeChunkEntityRenderer usa pra orientar pela direção de avanço.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));

        for (float[] seg : SEGMENTS) {
            float ox = seg[0], oy = seg[1] + bob, oz = seg[2];
            float w = seg[3], h = seg[4], l = seg[5], tint = seg[6];
            float r = BASE_R + (CREST_R - BASE_R) * tint;
            float g = BASE_G + (CREST_G - BASE_G) * tint;
            float b = BASE_B + (CREST_B - BASE_B) * tint;
            float a = BASE_A + (CREST_A - BASE_A) * tint;

            Matrix4f rot = new Matrix4f()
                    .scale(w, h, l)
                    .translate(ox / w, oy / h, oz / l);
            RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, r, g, b, a, TEXTURE,
                    1.0f, rot, false, true, true);
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(LavaSurfWaveVisualEntity entity) {
        return TEXTURE;
    }
}