package com.elementals.morebendings.bending.earthsubbendings.mud;

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
 * Renderer de {@link MudSpikeVisualEntity}. {@code MagmaSpikeVisualEntityRenderer}
 * desenha uma estalagmite lisa (3 segmentos empilhados retos, afinando pra
 * cima) -- pra Mud isso ficaria errado: lama não solidifica em ponta lisa,
 * ela racha e brota em grumos tortos. Em vez disso, cada instância desenha
 * um CLUSTER de 4 farpas curtas e grossas, cada uma com seu próprio yaw,
 * inclinação e altura (derivados do {@code seed}, então sincronizados —
 * todo cliente vê exatamente o mesmo grumo), brotando em ângulos
 * ligeiramente diferentes ao redor de um ponto central, em vez de uma
 * coluna única e simétrica -- mesma ideia de "orgânico" que {@code
 * MagmaSpikeVisualEntityRenderer} já busca com heightMult/widthMult, só
 * que aplicada a MÚLTIPLAS farpas por instância, não a uma coluna só.
 *
 * Textura: {@code block/mud}, tint variando de marrom escuro na base pra um
 * marrom mais claro/amarelado na ponta (lama seca rachando ao sol), mesma
 * receita de degradê base->ponta usada em Lava, só com paleta diferente.
 *
 * Mesma animação de crescer/encolher de {@code MagmaSpikeVisualEntityRenderer}.
 */
public class MudSpikeVisualEntityRenderer extends EntityRenderer<MudSpikeVisualEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/mud");

    // Base de cada farpa: marrom-lama escuro e úmido.
    private static final float BASE_R = 0.30f, BASE_G = 0.22f, BASE_B = 0.14f, BASE_A = 1.0f;
    // Ponta de cada farpa: marrom mais claro/amarelado, como lama secando.
    private static final float TIP_R = 0.62f, TIP_G = 0.49f, TIP_B = 0.32f, TIP_A = 1.0f;

    private static final float BASE_HEIGHT = 0.55f;
    private static final float BASE_WIDTH = 0.20f;

    private static final int GROW_TICKS = 4;
    private static final int RETRACT_TICKS = 6;

    /** Yaw fixo de cada farpa do cluster (graus), espaçadas ao redor do centro -- assimétrico de propósito. */
    private static final float[] SPIKE_YAWS = {20f, 130f, 210f, 300f};

    public MudSpikeVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MudSpikeVisualEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int seed = entity.getSeed();
        float age = entity.tickCount + partialTick;
        int lifetime = entity.getLifetimeTicks();

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

        poseStack.pushPose();

        for (int i = 0; i < SPIKE_YAWS.length; i++) {
            // Variação determinística por farpa a partir do seed + índice -- cada farpa do
            // MESMO grumo tem altura/inclinação/leve giro de yaw diferentes, mas idênticas
            // em todo cliente (nada de Math.random() aqui).
            int spikeSeed = seed + i * 7919; // primo grande só pra espalhar bem os bits
            float heightMult = 0.7f + 0.6f * ((spikeSeed % 100) / 100.0f); // ~0.7..1.3
            float widthMult = 0.85f + 0.35f * (((spikeSeed / 100) % 100) / 100.0f); // ~0.85..1.2
            float yawJitter = ((spikeSeed / 10_000) % 40) - 20; // +-20 graus em cima do yaw base
            float leanAngle = 8f + 14f * (((spikeSeed / 3_700) % 100) / 100.0f); // sempre inclinada pra fora, 8..22 graus
            // Deslocamento radial pequeno pra fora do centro -- reforça a leitura de "grumo", não coluna única.
            float radialOffset = 0.10f * widthMult;

            float yaw = SPIKE_YAWS[i] + yawJitter;
            float height = BASE_HEIGHT * heightMult * scale;
            float width = BASE_WIDTH * widthMult;

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.translate(0.0, 0.0, radialOffset);
            poseStack.mulPose(Axis.XP.rotationDegrees(leanAngle));

            drawSegment(vertexConsumer, poseStack, packedLight, 0.00f, height * 0.55f, width, BASE_R, BASE_G, BASE_B, BASE_A);
            drawSegment(vertexConsumer, poseStack, packedLight, height * 0.55f, height * 0.45f, width * 0.55f, TIP_R, TIP_G, TIP_B, TIP_A);

            poseStack.popPose();
        }

        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    /** Um segmento cúbico de uma farpa: começa em {@code yStart} (relativo ao chão) e sobe {@code segHeight}. */
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
    public ResourceLocation getTextureLocation(MudSpikeVisualEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}