package com.elementals.morebendings.bending.earthsubbendings.crystal;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
 * Renderer de {@link CrystalSpikeVisualEntity} -- uma farpa de ametista de
 * verdade brotando do chão, não mais só a troca de textura pra {@code
 * amethyst_block}. Mesmo esquema de crescer/encolher e "seed"
 * determinístico que {@code MagmaSpikeVisualEntityRenderer} já usa pra
 * {@code magmaSpike}, mas com duas farpas ANGULARES em vez de uma
 * estalagmite orgânica de 3 segmentos -- cada farpa é uma pirâmide fina
 * (2 segmentos tapeando bem rápido pra ponta), e a farpa secundária nasce
 * um pouco mais baixa e girada, formando um cluster de geodo em vez de um
 * espeto sozinho. Usa a mesma textura ({@code block/amethyst_block}) e o
 * mesmo tom roxo de tint que {@link CrystalShardEntityRenderer} já usa,
 * pra ficar visualmente da mesma "família" que o resto da árvore de
 * Crystal.
 */
public class CrystalSpikeVisualEntityRenderer extends EntityRenderer<CrystalSpikeVisualEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/amethyst_block");

    // Base: roxo escuro, quase ametista bruta.
    private static final float BASE_R = 0.40f, BASE_G = 0.22f, BASE_B = 0.55f, BASE_A = 1.0f;
    // Ponta: lilás claro e brilhante -- mesmo tom que CrystalShardEntityRenderer usa.
    private static final float TIP_R = 0.82f, TIP_G = 0.62f, TIP_B = 0.98f, TIP_A = 1.0f;

    /** Altura da farpa PRINCIPAL (blocos) na variação "média" -- o seed varia isso em +-35%. */
    private static final float BASE_HEIGHT = 1.0f;
    private static final float BASE_WIDTH = 0.26f;
    /** A farpa secundária é sempre um pouco menor que a principal, pra dar sensação de cluster. */
    private static final float SECONDARY_HEIGHT_MULT = 0.62f;
    private static final float SECONDARY_WIDTH_MULT = 0.7f;

    private static final int GROW_TICKS = 5;
    private static final int RETRACT_TICKS = 8;

    public CrystalSpikeVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CrystalSpikeVisualEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int seed = entity.getSeed();
        float age = entity.tickCount + partialTick;
        int lifetime = entity.getLifetimeTicks();

        // Fatores "aleatórios" determinísticos a partir do seed -- mesma farpa em todo cliente.
        float heightMult = 0.65f + 0.7f * ((seed % 100) / 100.0f); // ~0.65..1.35
        float widthMult = 0.8f + 0.5f * (((seed / 100) % 100) / 100.0f); // ~0.8..1.3
        float mainYaw = ((seed / 10_000) % 360);
        float mainLean = -10f + 20f * (((seed / 3_700) % 100) / 100.0f); // -10..+10 graus
        float secondaryYaw = mainYaw + 130f + 40f * (((seed / 7_919) % 100) / 100.0f); // girada em relação à principal
        float secondaryLean = -16f + 28f * (((seed / 5_003) % 100) / 100.0f);

        // Escala de crescimento/encolhimento (0..1). Sobe rápido nos primeiros GROW_TICKS,
        // fica em 1.0 no meio da vida, desce nos últimos RETRACT_TICKS antes de sumir.
        float growScale = Mth.clamp(age / GROW_TICKS, 0f, 1f);
        float retractStart = lifetime - RETRACT_TICKS;
        float retractScale = age <= retractStart ? 1f : Mth.clamp(1f - (age - retractStart) / RETRACT_TICKS, 0f, 1f);
        float scale = Math.min(growScale, retractScale);
        if (scale <= 0.02f) {
            return; // nada pra desenhar (acabou de nascer ou já encolheu totalmente)
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());

        float mainHeight = BASE_HEIGHT * heightMult * scale;
        float mainWidth = BASE_WIDTH * widthMult;
        drawSpike(vertexConsumer, poseStack, packedLight, 0.0f, mainYaw, mainLean, mainHeight, mainWidth);

        float secHeight = mainHeight * SECONDARY_HEIGHT_MULT;
        float secWidth = mainWidth * SECONDARY_WIDTH_MULT;
        drawSpike(vertexConsumer, poseStack, packedLight, 0.0f, secondaryYaw, secondaryLean, secHeight, secWidth);

        RenderSystem.disableBlend();
    }

    /** Uma farpa angular: 2 segmentos empilhados, o de cima bem mais fino, tapeando rápido pra uma ponta. */
    private void drawSpike(VertexConsumer vertexConsumer, PoseStack poseStack, int packedLight,
                           float yOffset, float yaw, float lean, float height, float width) {
        if (height <= 0.02f || width <= 0.02f) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0, yOffset, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(lean));

        drawSegment(vertexConsumer, poseStack, packedLight, 0.00f, height * 0.55f, width * 1.00f, BASE_R, BASE_G, BASE_B, BASE_A);
        drawSegment(vertexConsumer, poseStack, packedLight, height * 0.55f, height * 0.45f, width * 0.30f, TIP_R, TIP_G, TIP_B, TIP_A);

        poseStack.popPose();
    }

    /** Um segmento cúbico da farpa: começa em {@code yStart} (relativo ao chão) e sobe {@code segHeight}. */
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
        RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, r, g, b, a, TEXTURE,
                1.0f, rot, false, true, true);
    }

    @Override
    public ResourceLocation getTextureLocation(CrystalSpikeVisualEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}