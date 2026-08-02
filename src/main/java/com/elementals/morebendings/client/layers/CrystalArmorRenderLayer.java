package com.elementals.morebendings.client.layers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.saperate.elementals.client.entities.utils.RenderUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * "Armadura" da crystalArmor -- cobre o corpo INTEIRO sem brecha: cada
 * ModelPart (cabeça, tronco, braços, pernas) ganha uma casca sólida um
 * pouco maior que a própria hitbox da parte, com folga extra nas pontas
 * pra "morder" a parte vizinha e fechar a costura (ombro-tronco,
 * quadril-perna, pescoço-tronco-cabeça). Por cima da casca, alguns
 * espinhos angulares (ombros + acentos no tronco) dão o acabamento
 * facetado de cristal, mesma técnica/paleta de
 * {@code CrystalSpikeVisualEntityRenderer} (via {@link RenderUtils#drawCube}).
 *
 * Os números abaixo são chute inicial calculado a partir das dimensões
 * padrão do modelo Steve (em blocos: corpo 0.5x0.75x0.25, braço/perna
 * 0.25x0.75x0.25, cabeça 0.5x0.5x0.5) com folga de ~0.08-0.12 em cada
 * borda. Ajuste em jogo (F5 terceira pessoa) até não sobrar nenhum pixel
 * de pele/roupa à mostra nas juntas.
 */
public class CrystalArmorRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/amethyst_block");

    // Casca: tom médio, sólido, bem visível -- não é a farpa clara/escura, é a couraça em si.
    private static final float SHELL_R = 0.55f, SHELL_G = 0.34f, SHELL_B = 0.72f, SHELL_A = 1.0f;
    // Espinhos de acabamento: base escura -> ponta clara, mesma paleta do resto de Crystal.
    private static final float BASE_R = 0.40f, BASE_G = 0.22f, BASE_B = 0.55f, BASE_A = 1.0f;
    private static final float TIP_R = 0.82f, TIP_G = 0.62f, TIP_B = 0.98f, TIP_A = 1.0f;

    /** Casca sólida: largura/profundidade, altura, e onde o centro dela fica (y relativo ao pivot da parte). */
    private record Shell(float width, float depth, float height, float centerY) {
    }

    // Tronco: real 0.5x0.75x0.25 (pivot no topo/pescoço) -- folga pra fechar
    // com a cabeça em cima e as pernas embaixo.
    private static final Shell BODY_SHELL = new Shell(0.62f, 0.40f, 0.95f, 0.42f);
    // Braços: real 0.25x0.75x0.25 (pivot no ombro) -- folga extra na largura
    // pra morder o tronco e fechar a costura do ombro.
    private static final Shell ARM_SHELL = new Shell(0.36f, 0.36f, 0.90f, 0.40f);
    // Pernas: real 0.25x0.75x0.25 (pivot no quadril) -- folga pra morder o
    // tronco por cima.
    private static final Shell LEG_SHELL = new Shell(0.36f, 0.36f, 0.92f, 0.40f);
    // Cabeça: real 0.5x0.5x0.5 (pivot na base do pescoço) -- cobre o crânio
    // inteiro. Se quiser deixar os olhos livres em vez de cobrir tudo, é só
    // remover esta linha/chamada -- ver comentário em render().
    private static final Shell HEAD_SHELL = new Shell(0.58f, 0.58f, 0.62f, -0.31f);

    /** Espinho de acabamento: posição relativa ao pivot, ângulo, tamanho. */
    private record Spike(float x, float y, float z, float yaw, float lean, float height, float width) {
    }

    private static final Spike[] SHOULDER_SPIKES = {
            new Spike(0.10f, -0.18f, 0f, 90f, -55f, 0.34f, 0.18f),
            new Spike(-0.10f, -0.18f, 0f, -90f, -55f, 0.34f, 0.18f),
    };

    private static final Spike[] BODY_ACCENT_SPIKES = {
            new Spike(0f, -0.02f, 0.22f, 0f, -18f, 0.26f, 0.16f), // peito
            new Spike(0f, -0.02f, -0.22f, 180f, -18f, 0.26f, 0.16f), // costas
    };

    public CrystalArmorRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (!ClientCrystalArmorCache.isActive(player.getUUID())) return;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());
        PlayerModel<AbstractClientPlayer> model = getParentModel();

        // Cascas sólidas -- cobertura total, sem brecha.
        drawShell(poseStack, vertexConsumer, packedLight, model.body, BODY_SHELL);
        drawShell(poseStack, vertexConsumer, packedLight, model.rightArm, ARM_SHELL);
        drawShell(poseStack, vertexConsumer, packedLight, model.leftArm, ARM_SHELL);
        drawShell(poseStack, vertexConsumer, packedLight, model.rightLeg, LEG_SHELL);
        drawShell(poseStack, vertexConsumer, packedLight, model.leftLeg, LEG_SHELL);
        // Cabeça coberta por completo -- remova esta linha se preferir
        // deixar o rosto/olhos livres como na primeira versão.
        drawShell(poseStack, vertexConsumer, packedLight, model.head, HEAD_SHELL);

        // Espinhos de acabamento, por cima da casca -- só estética.
        drawSpikes(poseStack, vertexConsumer, packedLight, model.rightArm, SHOULDER_SPIKES);
        drawSpikes(poseStack, vertexConsumer, packedLight, model.leftArm, SHOULDER_SPIKES);
        drawSpikes(poseStack, vertexConsumer, packedLight, model.body, BODY_ACCENT_SPIKES);

        RenderSystem.disableBlend();
    }

    private void drawShell(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                           ModelPart part, Shell shell) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        poseStack.translate(0f, shell.centerY(), 0f);
        poseStack.scale(shell.width(), shell.height(), shell.depth());
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        Matrix4f identity = new Matrix4f(); // shell já foi escalada pelo poseStack acima
        RenderUtils.drawCube(buffer, poseStack, packedLight, SHELL_R, SHELL_G, SHELL_B, SHELL_A, TEXTURE,
                1.0f, identity, false, true, true);
        poseStack.popPose();
    }

    private void drawSpikes(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                            ModelPart part, Spike[] spikes) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        for (Spike spike : spikes) {
            poseStack.pushPose();
            poseStack.translate(spike.x(), spike.y(), spike.z());
            poseStack.mulPose(Axis.YP.rotationDegrees(spike.yaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(spike.lean()));
            drawSegment(buffer, poseStack, packedLight, 0f, spike.height() * 0.55f, spike.width(),
                    BASE_R, BASE_G, BASE_B, BASE_A);
            drawSegment(buffer, poseStack, packedLight, spike.height() * 0.55f, spike.height() * 0.45f,
                    spike.width() * 0.35f, TIP_R, TIP_G, TIP_B, TIP_A);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private void drawSegment(VertexConsumer buffer, PoseStack poseStack, int packedLight,
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
        RenderUtils.drawCube(buffer, poseStack, packedLight, r, g, b, a, TEXTURE,
                1.0f, rot, false, true, true);
    }
}