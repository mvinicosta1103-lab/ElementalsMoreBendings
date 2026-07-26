package com.elementals.morebendings.bending.airsubbendings.mist;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.saperate.elementals.client.entities.utils.RenderUtils;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Renderer de {@link MistFogEntity}. Mesma receita de {@code
 * CrystalShardEntityRenderer}/{@code BoneSpikeEntityRenderer} (cubo vanilla
 * texturizado + tint, via {@code RenderUtils.drawCube}), só que em vez de
 * um único espeto alongado, desenha várias "golfadas" (puffs) translúcidas
 * espalhadas ao redor do centro da névoa -- sobrepostas, cada uma girando
 * devagar num raio/altura/tamanho levemente diferente, o que cria uma
 * ilusão de neblina "respirando" sem precisar de bone animation/GeckoLib
 * (que este addon não usa em nenhum outro lugar).
 * <p>
 * NOTA: assim como nos outros renderers deste addon, eu não consigo abrir
 * o jogo pra conferir o resultado visual -- se as golfadas ficarem grandes
 * ou pequenas demais, ou muito "vazadas" (transparência excessiva por
 * causa do blending de várias camadas translúcidas sobrepostas), ajuste
 * PUFF_A e PUFF_COUNT primeiro; é o de maior efeito visual com menor
 * risco de quebrar algo.
 */
public class MistFogEntityRenderer extends EntityRenderer<MistFogEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/white_concrete");

    // Tint cinza bem claro, quase branco, e bem translúcido -- várias
    // golfadas sobrepostas compensam a transparência individual baixa.
    private static final float PUFF_R = 0.90f, PUFF_G = 0.92f, PUFF_B = 0.95f, PUFF_A = 0.32f;

    private static final int PUFF_COUNT = 7;
    private static final double ROTATION_SPEED = 0.006; // rad/tick -- bem devagar
    private static final double BOB_SPEED = 0.03;
    private static final double BOB_AMPLITUDE = 0.15;

    public MistFogEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MistFogEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float radius = entity.getRadius();
        double t = entity.tickCount + partialTick;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());

        for (int i = 0; i < PUFF_COUNT; i++) {
            poseStack.pushPose();

            // Cada golfada tem seu próprio ângulo-base (distribuídas em
            // círculo), distância do centro, altura e tamanho -- os
            // fatores abaixo são só "sementes" arbitrárias pra variar
            // puff a puff sem precisar de um gerador aleatório sincronizado
            // entre cliente e servidor (a névoa não pode "piscar" trocando
            // de forma a cada frame recalculado).
            double baseAngle = (Math.PI * 2 * i) / PUFF_COUNT;
            double distFactor = 0.35 + 0.5 * ((i * 37) % 100) / 100.0;   // ~0.35..0.85 do raio
            double sizeFactor = 0.35 + 0.25 * ((i * 53) % 100) / 100.0;  // ~0.35..0.60 do raio
            double heightBase = 0.6 + 1.6 * ((i * 71) % 100) / 100.0;    // varia a altura dentro da névoa

            double angle = baseAngle + t * ROTATION_SPEED * (i % 2 == 0 ? 1 : -1); // metade gira num sentido, metade no outro
            double dist = radius * distFactor;
            double bob = Math.sin(t * BOB_SPEED + i) * BOB_AMPLITUDE;

            double px = Math.cos(angle) * dist;
            double pz = Math.sin(angle) * dist;
            double py = heightBase + bob;

            float size = (float) (radius * sizeFactor);
            float selfSpin = (float) (t * ROTATION_SPEED * 40.0 * (i % 3 == 0 ? 1 : -1));

            Matrix4f rot = new Matrix4f()
                    .translate((float) px, (float) py, (float) pz)
                    .rotateY((float) Math.toRadians(selfSpin))
                    .scale(size, size * 0.7f, size); // achatada -- fica mais "nuvem" que "bola"

            RenderUtils.drawCube(vertexConsumer, poseStack, packedLight,
                    PUFF_R, PUFF_G, PUFF_B, PUFF_A, TEXTURE, 1.0f, rot, false, true, true);

            poseStack.popPose();
        }

        RenderSystem.disableBlend();
    }

    @Override
    public ResourceLocation getTextureLocation(MistFogEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}