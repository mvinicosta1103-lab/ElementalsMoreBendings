package com.elementals.morebendings.bending.earthsubbendings.lava;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.elementals.morebendings.client.render.CustomCubeRenderUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renderer de {@link MagmaSpikeVisualEntity} -- um espeto de verdade, não
 * mais uma troca de textura no chão. Desenha uma "estalagmite" de 3
 * segmentos empilhados afinando pra cima (base larga e escura -> meio
 * médio -> ponta fina e clara), igual o degradê vinho->laranja usado em
 * {@link LavaShurikenEntityRenderer}. Cada instância usa {@link
 * MagmaSpikeVisualEntity#getSeed()} pra variar altura/inclinação/giro de
 * forma determinística (mesmo valor em todo cliente), dando um cluster
 * com cara orgânica em vez de um bloco de espinhos idênticos e alinhados.
 * <p>
 * Animação: cresce do chão nos primeiros {@link #GROW_TICKS} e encolhe de
 * volta pro chão nos últimos {@link #RETRACT_TICKS} antes de {@link
 * MagmaSpikeVisualEntity#getLifetimeTicks()} -- em vez de simplesmente
 * aparecer/sumir, o que ficaria muito abrupto pra algo do tamanho de um
 * bloco.
 */
public class MagmaSpikeVisualEntityRenderer extends EntityRenderer<MagmaSpikeVisualEntity> {

    /** Textura própria do mod (não mais a sprite vanilla block/magma_block do atlas de blocos). */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("elementalsmorebendings", "textures/models/magma/magma_texture.png");

    // Tints leves só pra manter a leitura de "base fria -> ponta incandescente";
    // a textura em si (magma_texture.png) já carrega a cor/rachadura de lava,
    // então os multiplicadores ficam perto do branco em vez de recolorir tudo.
    private static final float BASE_R = 0.75f, BASE_G = 0.55f, BASE_B = 0.50f, BASE_A = 1.0f;
    private static final float MID_R = 0.92f, MID_G = 0.75f, MID_B = 0.65f, MID_A = 1.0f;
    private static final float TIP_R = 1.0f, TIP_G = 0.95f, TIP_B = 0.85f, TIP_A = 1.0f;

    /** Altura total do espinho (blocos) na variação "média" -- o seed varia isso em +-35%. */
    private static final float BASE_HEIGHT = 1.05f;
    private static final float BASE_WIDTH = 0.34f;

    private static final int GROW_TICKS = 5;
    private static final int RETRACT_TICKS = 8;

    public MagmaSpikeVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MagmaSpikeVisualEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int seed = entity.getSeed();
        float age = entity.tickCount + partialTick;
        int lifetime = entity.getLifetimeTicks();

        // Fatores "aleatórios" determinísticos a partir do seed -- mesmo espinho em todo cliente.
        float heightMult = 0.65f + 0.7f * ((seed % 100) / 100.0f); // ~0.65..1.35
        float widthMult = 0.8f + 0.5f * (((seed / 100) % 100) / 100.0f); // ~0.8..1.3
        float leanYaw = ((seed / 10_000) % 360);
        float leanAngle = -12f + 24f * (((seed / 3_700) % 100) / 100.0f); // pequena inclinação -12..+12 graus

        // Escala de crescimento/encolhimento (0..1). Sobe rápido nos primeiros GROW_TICKS,
        // fica em 1.0 no meio da vida, desce nos últimos RETRACT_TICKS antes de sumir.
        float growScale = Mth.clamp(age / GROW_TICKS, 0f, 1f);
        float retractStart = lifetime - RETRACT_TICKS;
        float retractScale = age <= retractStart ? 1f : Mth.clamp(1f - (age - retractStart) / RETRACT_TICKS, 0f, 1f);
        float scale = Math.min(growScale, retractScale);
        if (scale <= 0.02f) {
            return; // nada pra desenhar (acabou de nascer ou já encolheu totalmente)
        }

        float height = BASE_HEIGHT * heightMult * scale;
        float width = BASE_WIDTH * widthMult;

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(leanYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(leanAngle));

        // 3 segmentos empilhados verticalmente (eixo Y local, já rotacionado pra
        // simular a inclinação), cada um mais fino e mais alto na paleta que o anterior.
        drawSegment(vertexConsumer, poseStack, packedLight, 0.00f, height * 0.40f, width * 1.00f, BASE_R, BASE_G, BASE_B, BASE_A);
        drawSegment(vertexConsumer, poseStack, packedLight, height * 0.40f, height * 0.35f, width * 0.62f, MID_R, MID_G, MID_B, MID_A);
        drawSegment(vertexConsumer, poseStack, packedLight, height * 0.75f, height * 0.25f, width * 0.32f, TIP_R, TIP_G, TIP_B, TIP_A);

        poseStack.popPose();
    }

    /** Um segmento cúbico do espinho: começa em {@code yStart} (relativo ao chão) e sobe {@code segHeight}. */
    private void drawSegment(VertexConsumer vertexConsumer, PoseStack poseStack, int packedLight,
                             float yStart, float segHeight, float segWidth,
                             float r, float g, float b, float a) {
        if (segHeight <= 0.001f || segWidth <= 0.001f) {
            return;
        }
        float centerY = yStart + segHeight / 2.0f;
        float ty = centerY / segHeight;
        Matrix4f rot = new Matrix4f()
                .scale(segWidth, segHeight, segWidth)
                .translate(0.0f, ty, 0.0f);
        CustomCubeRenderUtils.drawCube(vertexConsumer, poseStack, packedLight, r, g, b, a,
                1.0f, rot, false, true, true);
    }

    @Override
    public ResourceLocation getTextureLocation(MagmaSpikeVisualEntity entity) {
        return TEXTURE;
    }
}