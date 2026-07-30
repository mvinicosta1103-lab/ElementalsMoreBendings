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
 * Renderer de {@link MudBallEntity}. Diferente da receita "espeto alongado"
 * usada por Crystal/Bone (um {@link RenderUtils#drawCube} só, esticado num
 * eixo) e da "estalagmite em 3 segmentos" de {@code MagmaSpikeVisualEntity}
 * -- aqui o formato é um AGLOMERADO de 5 cubinhos de tamanhos e posições
 * ligeiramente diferentes, sobrepostos em torno de um núcleo central, pra
 * ler como uma bola de lama disforme e grudenta, não uma esfera perfeita
 * nem uma lâmina. Cada cubo tem seu próprio pequeno desalinhamento (offset
 * + rotação), fixo por instância -- não recalculado a cada frame -- pra
 * parecer um "amassado" real em vez de tremer.
 *
 * Textura: {@code block/mud} (o bloco de lama de verdade, já usado por
 * {@code MudTrapAbility} pra pintar o chão), com tint levemente variado
 * entre os caroços pra dar profundidade sem precisar de textura própria.
 *
 * Gira lentamente enquanto voa (mesma ideia de {@code LavaShurikenEntityRenderer},
 * só que num eixo diagonal em vez de um giro plano) pra não parecer estática
 * no ar.
 */
public class MudBallEntityRenderer extends EntityRenderer<MudBallEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/mud");

    // Tint base: marrom-lama saturado. Cada caroço aplica um multiplicador levemente diferente.
    private static final float BASE_R = 0.55f, BASE_G = 0.42f, BASE_B = 0.28f, BASE_A = 1.0f;

    /**
     * Cada linha: offsetX, offsetY, offsetZ (em blocos, relativo ao centro),
     * tamanho (lado do cubo), multiplicador de brilho do tint (1.0 = tint base).
     * 1 núcleo central + 4 caroços menores ao redor, deliberadamente
     * assimétricos -- não é uma cruz nem uma grade regular.
     */
    private static final float[][] LUMPS = {
            { 0.00f,  0.00f,  0.00f, 0.30f, 1.00f}, // núcleo
            { 0.13f,  0.07f, -0.05f, 0.19f, 0.85f}, // caroço claro, frente-cima
            {-0.10f, -0.06f,  0.08f, 0.17f, 0.70f}, // caroço escuro, trás-baixo
            {-0.08f,  0.10f,  0.06f, 0.15f, 0.95f}, // caroço médio, cima
            { 0.07f, -0.09f, -0.09f, 0.14f, 0.60f}, // caroço escuro, baixo-trás
    };

    private static final float SPIN_DEGREES_PER_MS = 0.55f;
    private static long firstTime = -1L;

    public MudBallEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MudBallEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (firstTime == -1L) {
            firstTime = System.currentTimeMillis();
        }
        float spin = (System.currentTimeMillis() - firstTime) * SPIN_DEGREES_PER_MS;

        poseStack.pushPose();
        // Giro num eixo diagonal (não puramente Y) -- dá a sensação de uma
        // bola disforme tombando no ar, não um disco girando "de plano".
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin * 0.6f));

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());

        for (float[] lump : LUMPS) {
            float ox = lump[0], oy = lump[1], oz = lump[2], size = lump[3], shade = lump[4];
            Matrix4f rot = new Matrix4f()
                    .scale(size, size, size)
                    .translate(ox / size, oy / size, oz / size);
            RenderUtils.drawCube(vertexConsumer, poseStack, packedLight,
                    BASE_R * shade, BASE_G * shade, BASE_B * shade, BASE_A, TEXTURE,
                    1.0f, rot, false, true, true);
        }

        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MudBallEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}