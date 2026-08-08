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
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renderer de {@link LavaGeyserVisualEntity} -- diferente de {@link
 * MagmaSpikeVisualEntityRenderer} (estalagmite estática, com uma
 * inclinação FIXA por seed), aqui os 3 segmentos empilhados GIRAM em
 * torno do próprio eixo Y continuamente, com velocidade de giro crescendo
 * por segmento (o topo gira mais rápido que a base) -- dá a sensação de
 * um jato sob pressão torcendo no ar, não algo brotando devagar do chão.
 * Sobe quase instantâneo ({@link #ERUPT_TICKS}) mas agora FICA de pé
 * jorrando por alguns segundos antes de recolher ({@link #RETRACT_TICKS}
 * antes do fim de {@link LavaGeyserVisualEntity#getJetTicks()}) -- ver
 * {@link LavaGeyserAbility}. O modelo some no fim do jato, mas a
 * entidade continua viva além disso só emitindo fuligem (ver {@link
 * LavaGeyserVisualEntity#tick()}), por isso este renderer usa {@code
 * getJetTicks()} (não {@code getLifetimeTicks()}) pra decidir quando
 * parar de desenhar.
 */
public class LavaGeyserVisualEntityRenderer extends EntityRenderer<LavaGeyserVisualEntity> {

    /** Reaproveita a mesma textura de magma que o espinho de magmaSpike/volcanicEruption usa. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("elementalsmorebendings", "textures/models/magma/magma_texture.png");

    // Degradê base incandescente (vermelho-escuro) -> topo do jato (amarelo-alaranjado, mais claro/transparente).
    private static final float BASE_R = 0.85f, BASE_G = 0.35f, BASE_B = 0.15f, BASE_A = 1.0f;
    private static final float MID_R = 1.00f, MID_G = 0.55f, MID_B = 0.20f, MID_A = 0.95f;
    private static final float TIP_R = 1.00f, TIP_G = 0.85f, TIP_B = 0.35f, TIP_A = 0.85f;

    private static final float BASE_HEIGHT = 2.1f;
    private static final float BASE_WIDTH = 0.30f;

    private static final int ERUPT_TICKS = 3; // sobe quase instantâneo -- é um jato, não algo brotando devagar
    private static final int RETRACT_TICKS = 6;

    public LavaGeyserVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LavaGeyserVisualEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int seed = entity.getSeed();
        float age = entity.tickCount + partialTick;
        // getJetTicks(), não getLifetimeTicks() -- o modelo some no fim do JATO, mesmo que a
        // entidade continue viva mais um tempo só pra terminar de soltar fuligem (ver classe).
        int lifetime = entity.getJetTicks();

        // Fatores "aleatórios" determinísticos a partir do seed -- mesmo jato em todo cliente.
        float heightMult = 0.85f + 0.3f * ((seed % 100) / 100.0f); // ~0.85..1.15
        float widthMult = 0.85f + 0.3f * (((seed / 100) % 100) / 100.0f); // ~0.85..1.15
        float spinOffset = (seed / 10_000) % 360;

        // Escala de erupção/recolhimento (0..1), igual esquema de MagmaSpikeVisualEntityRenderer.
        float growScale = Mth.clamp(age / ERUPT_TICKS, 0f, 1f);
        float retractStart = lifetime - RETRACT_TICKS;
        float retractScale = age <= retractStart ? 1f : Mth.clamp(1f - (age - retractStart) / RETRACT_TICKS, 0f, 1f);
        float scale = Math.min(growScale, retractScale);
        if (scale <= 0.02f) {
            return; // nada pra desenhar (acabou de nascer ou já recolheu totalmente)
        }

        float height = BASE_HEIGHT * heightMult * scale;
        float width = BASE_WIDTH * widthMult;

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        poseStack.pushPose();

        // 3 segmentos empilhados, cada um com seu próprio giro (mais rápido quanto mais alto),
        // em vez do lean fixo do espinho de magma -- é isso que lê como "jato torcendo".
        drawSegment(vertexConsumer, poseStack, packedLight, 0.00f, height * 0.40f, width * 1.00f,
                spinOffset + age * 6f, BASE_R, BASE_G, BASE_B, BASE_A);
        drawSegment(vertexConsumer, poseStack, packedLight, height * 0.40f, height * 0.35f, width * 0.70f,
                spinOffset + age * 12f, MID_R, MID_G, MID_B, MID_A);
        drawSegment(vertexConsumer, poseStack, packedLight, height * 0.75f, height * 0.25f, width * 0.40f,
                spinOffset + age * 20f, TIP_R, TIP_G, TIP_B, TIP_A);

        poseStack.popPose();
    }

    /** Um segmento cúbico do jato, com seu próprio giro em torno do eixo Y (spinDegrees). */
    private void drawSegment(VertexConsumer vertexConsumer, PoseStack poseStack, int packedLight,
                             float yStart, float segHeight, float segWidth, float spinDegrees,
                             float r, float g, float b, float a) {
        if (segHeight <= 0.001f || segWidth <= 0.001f) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));

        float centerY = yStart + segHeight / 2.0f;
        float ty = centerY / segHeight;
        Matrix4f rot = new Matrix4f()
                .scale(segWidth, segHeight, segWidth)
                .translate(0.0f, ty, 0.0f);
        RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, r, g, b, a, TEXTURE,
                1.0f, rot, false, true, true);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(LavaGeyserVisualEntity entity) {
        return TEXTURE;
    }
}