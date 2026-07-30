package com.elementals.morebendings.bending.earthsubbendings.lava;

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
 * Renderer do {@link LavaShurikenEntity}.
 *
 * === REDESENHO (pedido: "igual ao Water Blade, uma Shuriken que fica
 * girando com partes pontiagudas") ===
 * A versão anterior desenhava um único "espeto" fino apontado na direção
 * do voo (mesmo esquema de CrystalShardEntityRenderer/GlassShardEntityRenderer
 * neste addon). Troquei por uma farpa composta: um núcleo cúbico no centro
 * + {@link #SPIKES} espigões finos radiando em várias direções (não só
 * num plano -- alternam entre "pra cima" e "pra baixo" a cada 45° de yaw,
 * pra dar volume 3D de estilha, como na referência
 * assets/elementalssubbending/textures/entity/lava_shuriken.png do addon
 * ElementalsSubbending anexado).
 *
 * Copiei o truque de giro contínuo de
 * {@code dev.saperate.elementals.client.entities.water.WaterBladeEntityRenderer}
 * (o Water Blade do mod base): em vez de derivar a rotação do yaw/pitch da
 * entidade (que só atualiza por tick e trava quando ela está parada sob
 * controle), gira em cima de {@code System.currentTimeMillis()}, então a
 * farpa fica girando sem parar mesmo enquanto está "presa" na mão do
 * jogador esperando o arremesso -- igual o Water Blade original.
 *
 * Continua sem model/.json próprio: cada espigão e o núcleo são só
 * chamadas de {@link RenderUtils#drawCube} com uma sprite do atlas de
 * blocos (block/magma_block, igual antes), só que agora várias por
 * entidade em vez de uma.
 */
public class LavaShurikenEntityRenderer extends EntityRenderer<LavaShurikenEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/magma_block");

    // Tint do núcleo: mais escuro/avermelhado, pra parecer a "casca" da farpa.
    private static final float CORE_R = 0.45f, CORE_G = 0.12f, CORE_B = 0.05f, CORE_A = 1.0f;
    // Tint dos espigões: laranja quente, mais claro que o núcleo (a "ponta" incandescente).
    private static final float SPIKE_R = 1.0f, SPIKE_G = 0.55f, SPIKE_B = 0.15f, SPIKE_A = 1.0f;

    private static final float CORE_SIZE = 0.20f;
    private static final float SPIKE_LENGTH = 0.42f;
    private static final float SPIKE_THICKNESS = 0.085f;
    private static final float SPIKE_INNER_RADIUS = 0.08f;

    /** Graus de giro por milissegundo -- mesma ideia do {@code rot * 20.0f} do Water Blade (mas em ms, não em ticks). */
    private static final float SPIN_DEGREES_PER_MS = 0.35f;

    /** yaw/pitch (graus) de cada espigão -- alterna pitch pra formar uma "bola de estilhaços" em vez de um leque plano. */
    private static final float[][] SPIKES = {
            {0f, 0f}, {45f, 35f}, {90f, 0f}, {135f, -35f},
            {180f, 0f}, {225f, 35f}, {270f, 0f}, {315f, -35f},
    };

    private static long firstTime = -1L;

    public LavaShurikenEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LavaShurikenEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (firstTime == -1L) {
            firstTime = System.currentTimeMillis();
        }
        float spinDegrees = (System.currentTimeMillis() - firstTime) * SPIN_DEGREES_PER_MS;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());

        // Núcleo: cubo pequeno centralizado na origem (translate -0.5 então scale, igual o
        // "espeto" original -- ver javadoc da classe).
        Matrix4f coreRot = new Matrix4f()
                .scale(CORE_SIZE, CORE_SIZE, CORE_SIZE)
                .translate(0.0f, 0.0f, -0.5f);
        RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, CORE_R, CORE_G, CORE_B, CORE_A, TEXTURE,
                1.0f, coreRot, false, true, true);

        // Espigões: cada um parte de SPIKE_INNER_RADIUS (perto do núcleo) e se estende
        // SPIKE_LENGTH blocos pra fora, apontando na direção definida por SPIKES.
        float tz = SPIKE_INNER_RADIUS / SPIKE_LENGTH;
        for (float[] spike : SPIKES) {
            Matrix4f spikeRot = new Matrix4f()
                    .rotateY((float) Math.toRadians(spike[0]))
                    .rotateX((float) Math.toRadians(spike[1]))
                    .scale(SPIKE_THICKNESS, SPIKE_THICKNESS, SPIKE_LENGTH)
                    .translate(0.0f, 0.0f, tz);
            RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, SPIKE_R, SPIKE_G, SPIKE_B, SPIKE_A, TEXTURE,
                    1.0f, spikeRot, false, true, true);
        }

        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(LavaShurikenEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}