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
import org.joml.Matrix4f;

/**
 * Renderer de {@link MudSurgeChunkEntity}. Diferente do {@code
 * MudBallEntityRenderer} (aglomerado arredondado de caroços, lama fresca e
 * grudenta) -- aqui o formato é deliberadamente ANGULOSO: 3 blocos
 * retangulares irregulares, com cantos e arestas duras, pra ler como um
 * fragmento de lama JÁ ENDURECIDA/rachada se despedaçando, tipo cascalho
 * de barro seco, não uma gota. Tint mais claro e acinzentado que o mudBall
 * (mesma paleta "base->ponta" que {@code MudSpikeVisualEntityRenderer} usa
 * pra lama secando ao sol, só aplicada aos 3 blocos inteiros em vez de um
 * degradê por segmento).
 * <p>
 * Cada pedaço nasce com {@link MudSurgeChunkEntity#setYRot} apontando pra
 * direção de avanço (ver construtor) -- por isso, diferente do mudBall (que
 * gira num eixo diagonal solto no ar), aqui a rotação de base segue o yaw
 * da entidade e só a INCLINAÇÃO de "tombo pra frente" é animada, pra parecer
 * um pedaço de entulho rolando no chão conforme avança, não flutuando/girando
 * no ar sem rumo.
 */
public class MudSurgeChunkEntityRenderer extends EntityRenderer<MudSurgeChunkEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/mud");

    // Base de cada pedaço: marrom acinzentado, seco -- mais claro/dessaturado que o mudBall (lama fresca).
    private static final float BASE_R = 0.52f, BASE_G = 0.44f, BASE_B = 0.34f, BASE_A = 1.0f;

    /**
     * Cada linha: offsetX, offsetY, offsetZ (relativo ao centro), largura,
     * altura, comprimento, multiplicador de tint. 3 blocos irregulares
     * sobrepostos formando um fragmento anguloso, não uma grade regular.
     */
    private static final float[][] CHUNKS = {
            { 0.00f,  0.00f,  0.00f, 0.28f, 0.20f, 0.34f, 1.00f}, // corpo principal, alongado no sentido do avanço
            { 0.10f,  0.09f, -0.08f, 0.16f, 0.14f, 0.16f, 0.80f}, // lasca menor, em cima
            {-0.09f, -0.05f,  0.07f, 0.14f, 0.11f, 0.18f, 0.62f}, // lasca menor, embaixo/atrás
    };

    private static final float ROLL_DEGREES_PER_BLOCK = 220f;

    public MudSurgeChunkEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MudSurgeChunkEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());

        // Distância percorrida desde o nascimento (aprox.) determina o tombo --
        // rola "pra frente" conforme avança, em vez de girar solto no tempo.
        double speed = entity.getDeltaMovement().length();
        float roll = (float) ((entity.tickCount + partialTick) * speed * ROLL_DEGREES_PER_BLOCK) % 360f;

        poseStack.pushPose();
        // Orienta o pedaço na direção de avanço (yaw fixo, definido no spawn),
        // depois aplica o tombo animado num eixo perpendicular ao avanço.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));

        for (float[] chunk : CHUNKS) {
            float ox = chunk[0], oy = chunk[1], oz = chunk[2];
            float w = chunk[3], h = chunk[4], l = chunk[5], shade = chunk[6];
            Matrix4f rot = new Matrix4f()
                    .scale(w, h, l)
                    .translate(ox / w, oy / h, oz / l);
            RenderUtils.drawCube(vertexConsumer, poseStack, packedLight,
                    BASE_R * shade, BASE_G * shade, BASE_B * shade, BASE_A, TEXTURE,
                    1.0f, rot, false, true, true);
        }

        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MudSurgeChunkEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}