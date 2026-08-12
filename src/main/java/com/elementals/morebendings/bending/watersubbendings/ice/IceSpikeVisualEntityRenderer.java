package com.elementals.morebendings.bending.watersubbendings.ice;

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
 * Renderer de {@link IceSpikeVisualEntity} -- uma farpa de gelo de verdade
 * brotando do chão, mesmo esquema de crescer/encolher e "seed"
 * determinístico que {@code CrystalSpikeVisualEntityRenderer} usa pra
 * {@code crystalSpike}: duas farpas ANGULARES (pirâmides finas de 2
 * segmentos tapeando pra ponta), a secundária um pouco menor e girada,
 * formando um cluster. Usa a textura vanilla de {@code block/packed_ice}
 * com tint azul-esbranquiçado, pra ficar visualmente clara como "gelo" e
 * não se confundir com a paleta roxa de Crystal.
 */
public class IceSpikeVisualEntityRenderer extends EntityRenderer<IceSpikeVisualEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/packed_ice");

    // Base: azul-gelo mais escuro/saturado.
    private static final float BASE_R = 0.55f, BASE_G = 0.72f, BASE_B = 0.92f, BASE_A = 0.95f;
    // Ponta: quase branca, brilhante -- efeito de gelo fino na ponta.
    private static final float TIP_R = 0.90f, TIP_G = 0.97f, TIP_B = 1.0f, TIP_A = 0.95f;

    private static final float BASE_HEIGHT = 1.0f;
    private static final float BASE_WIDTH = 0.24f;
    private static final float SECONDARY_HEIGHT_MULT = 0.60f;
    private static final float SECONDARY_WIDTH_MULT = 0.68f;

    private static final int GROW_TICKS = 5;
    private static final int RETRACT_TICKS = 8;

    public IceSpikeVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(IceSpikeVisualEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int seed = entity.getSeed();
        float age = entity.tickCount + partialTick;
        int lifetime = entity.getLifetimeTicks();

        float heightMult = 0.65f + 0.7f * ((seed % 100) / 100.0f);
        float widthMult = 0.8f + 0.5f * (((seed / 100) % 100) / 100.0f);
        float mainYaw = ((seed / 10_000) % 360);
        float mainLean = -10f + 20f * (((seed / 3_700) % 100) / 100.0f);
        float secondaryYaw = mainYaw + 130f + 40f * (((seed / 7_919) % 100) / 100.0f);
        float secondaryLean = -16f + 28f * (((seed / 5_003) % 100) / 100.0f);

        float growScale = Mth.clamp(age / GROW_TICKS, 0f, 1f);
        float retractStart = lifetime - RETRACT_TICKS;
        float retractScale = age <= retractStart ? 1f : Mth.clamp(1f - (age - retractStart) / RETRACT_TICKS, 0f, 1f);
        float scale = Math.min(growScale, retractScale);
        if (scale <= 0.02f) {
            return;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());

        float mainHeight = BASE_HEIGHT * heightMult * scale;
        float mainWidth = BASE_WIDTH * widthMult;
        drawSpike(vertexConsumer, poseStack, packedLight, mainYaw, mainLean, mainHeight, mainWidth);

        float secHeight = mainHeight * SECONDARY_HEIGHT_MULT;
        float secWidth = mainWidth * SECONDARY_WIDTH_MULT;
        drawSpike(vertexConsumer, poseStack, packedLight, secondaryYaw, secondaryLean, secHeight, secWidth);

        RenderSystem.disableBlend();
    }

    /** Uma farpa angular: 2 segmentos empilhados, o de cima bem mais fino, tapeando rápido pra uma ponta. */
    private void drawSpike(VertexConsumer vertexConsumer, PoseStack poseStack, int packedLight,
                           float yaw, float lean, float height, float width) {
        if (height <= 0.02f || width <= 0.02f) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(lean));

        drawSegment(vertexConsumer, poseStack, packedLight, 0.00f, height * 0.55f, width * 1.00f, BASE_R, BASE_G, BASE_B, BASE_A);
        drawSegment(vertexConsumer, poseStack, packedLight, height * 0.55f, height * 0.45f, width * 0.30f, TIP_R, TIP_G, TIP_B, TIP_A);

        poseStack.popPose();
    }

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
    public ResourceLocation getTextureLocation(IceSpikeVisualEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}