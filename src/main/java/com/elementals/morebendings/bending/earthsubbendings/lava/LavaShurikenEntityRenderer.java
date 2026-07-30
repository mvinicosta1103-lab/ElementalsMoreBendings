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
 * === REDESENHO #2 (pedido: "shuriken de verdade, lâminas horizontais num
 * eixo só, não espigões espalhados de qualquer jeito") ===
 * A versão anterior (bola de espigões com pitch variado, apontando em
 * várias direções tipo estilhaço 3D) ficou parecendo uma bola de espinhos
 * aleatória, não uma shuriken -- reportado com screenshot pelo usuário.
 *
 * Troquei o layout inteiro: agora TODAS as lâminas têm pitch = 0, ou seja,
 * vivem no mesmo plano horizontal (plano XZ) e só variam em yaw --
 * exatamente como uma estrela ninja de verdade, que é um disco fino com
 * pontas, não uma esfera de espinhos. O giro continua em torno do eixo Y
 * ({@code Axis.YP}), que é o eixo certo pra isso: como as lâminas estão
 * todas no plano horizontal, girar em Y faz elas girarem "de plano" (igual
 * um shuriken/frisbee de verdade), em vez de girar uma bola em torno de um
 * eixo arbitrário.
 *
 * Cada lâmina também ficou achatada (fina em Y, larga em X) em vez de ter
 * seção quadrada, pra realmente ler como "lâmina" e não "espigão" quando
 * vista de perto -- e ainda tem 2 segmentos (base larga escura perto do
 * núcleo, ponta fina clara na extremidade) pro degradê vinho->laranja.
 *
 * Continua sem model/.json próprio: só chamadas de
 * {@link RenderUtils#drawCube} com a sprite do atlas de blocos
 * (block/magma_block).
 */
public class LavaShurikenEntityRenderer extends EntityRenderer<LavaShurikenEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/magma_block");

    // Tint do núcleo: mais escuro/avermelhado, pra parecer a "casca" da farpa.
    private static final float CORE_R = 0.45f, CORE_G = 0.12f, CORE_B = 0.05f, CORE_A = 1.0f;
    // Tint das pontas: laranja quente, mais claro que o núcleo (a parte incandescente).
    private static final float BLADE_R = 1.0f, BLADE_G = 0.55f, BLADE_B = 0.15f, BLADE_A = 1.0f;

    // Núcleo achatado (disco fino), não mais um cubo "redondo".
    private static final float CORE_WIDTH = 0.22f;
    private static final float CORE_HEIGHT = 0.09f;

    // Lâminas: largura (eixo X, "grossura" vista de cima) x altura (eixo Y, achatada) x comprimento (eixo Z, radial).
    private static final float BLADE_LENGTH = 0.62f;
    private static final float BLADE_WIDTH = 0.16f;
    private static final float BLADE_HEIGHT = 0.075f;
    private static final float BLADE_INNER_RADIUS = 0.06f;

    /** Fração do comprimento que é a base (larga, escura) -- o resto é a ponta (fina, clara). */
    private static final float BASE_SEGMENT_FRACTION = 0.5f;
    /** Quanto a ponta afina em relação à base (dá o formato triangular/pontudo de lâmina). */
    private static final float TIP_WIDTH_FACTOR = 0.45f;

    /** Graus de giro por milissegundo -- giro rápido de shuriken girando no ar. */
    private static final float SPIN_DEGREES_PER_MS = 1.1f;

    /**
     * yaw (graus) e multiplicador de comprimento de cada lâmina -- SEM pitch,
     * todas no mesmo plano horizontal. 8 pontas alternando longa/curta (igual
     * shuriken de 4 ou 8 pontas clássica), espaçadas uniformemente a cada 45°
     * -- aqui a simetria é intencional (é uma shuriken de verdade, não um
     * estilhaço caótico), a leitura de "shuriken" vem do formato plano e
     * achatado das lâminas, não de aleatoriedade.
     */
    private static final float[][] BLADES = {
            {  0f, 1.00f}, { 45f, 0.62f}, { 90f, 1.00f}, {135f, 0.62f},
            {180f, 1.00f}, {225f, 0.62f}, {270f, 1.00f}, {315f, 0.62f},
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
        // Eixo único de giro: Y. Como toda a geometria abaixo vive no plano
        // horizontal (pitch = 0), isso gira a shuriken "de plano", igual uma
        // shuriken real arremessada.
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDegrees));

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());

        // Núcleo: disco fino centralizado na origem.
        Matrix4f coreRot = new Matrix4f()
                .scale(CORE_WIDTH, CORE_HEIGHT, CORE_WIDTH)
                .translate(0.0f, 0.0f, -0.5f);
        RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, CORE_R, CORE_G, CORE_B, CORE_A, TEXTURE,
                1.0f, coreRot, false, true, true);

        // Lâminas: todas no plano horizontal (só yaw, pitch sempre 0), cada uma com
        // base larga+escura perto do núcleo e ponta fina+clara na extremidade.
        for (float[] blade : BLADES) {
            float yaw = blade[0];
            float length = BLADE_LENGTH * blade[1];

            float baseLength = length * BASE_SEGMENT_FRACTION;
            float tipLength = length - baseLength;
            float tipWidth = BLADE_WIDTH * TIP_WIDTH_FACTOR;

            // Base: de BLADE_INNER_RADIUS até BLADE_INNER_RADIUS + baseLength.
            float baseCenter = BLADE_INNER_RADIUS + baseLength / 2.0f;
            float tzBase = baseCenter / baseLength;
            Matrix4f baseRot = new Matrix4f()
                    .rotateY((float) Math.toRadians(yaw))
                    .scale(BLADE_WIDTH, BLADE_HEIGHT, baseLength)
                    .translate(0.0f, 0.0f, tzBase);
            RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, CORE_R, CORE_G, CORE_B, CORE_A, TEXTURE,
                    1.0f, baseRot, false, true, true);

            // Ponta: continua de onde a base parou, mais fina e mais clara -- dá o bico da lâmina.
            float tipCenter = BLADE_INNER_RADIUS + baseLength + tipLength / 2.0f;
            float tzTip = tipCenter / tipLength;
            Matrix4f tipRot = new Matrix4f()
                    .rotateY((float) Math.toRadians(yaw))
                    .scale(tipWidth, BLADE_HEIGHT, tipLength)
                    .translate(0.0f, 0.0f, tzTip);
            RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, BLADE_R, BLADE_G, BLADE_B, BLADE_A, TEXTURE,
                    1.0f, tipRot, false, true, true);
        }

        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(LavaShurikenEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}