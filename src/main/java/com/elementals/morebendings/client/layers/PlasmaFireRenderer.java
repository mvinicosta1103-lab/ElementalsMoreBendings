package com.elementals.morebendings.client.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.Color;

/**
 * Lógica compartilhada de desenho do "fogo de plasma" nas mãos -- usada
 * tanto em terceira pessoa ({@link PlasmaHandFlameLayer}) quanto em
 * primeira pessoa ({@link PlasmaFirstPersonFireHandler}), pra manter
 * exatamente o mesmo visual/posição/cor nos dois casos.
 *
 * Continua usando o próprio modelo de bloco de fogo do Minecraft
 * (minecraft:fire) -- a animação (fire_0/fire_1 alternando sozinha) é
 * 100% vanilla. A única parte "customizada" é envolver o
 * MultiBufferSource num wrapper que multiplica a cor de cada vértice pela
 * cor atual do ciclo arco-íris, pra colorir esse fogo (que normalmente
 * sai laranja) com a cor do momento.
 */
public final class PlasmaFireRenderer {

    // Fogo normal do Minecraft. Troque para Blocks.SOUL_FIRE se preferir a
    // base azul (soul fire) por baixo do tingimento arco-íris.
    private static final BlockState FIRE_STATE = Blocks.FIRE.defaultBlockState();

    // Fogo sempre "brilha" independente da luz do ambiente.
    private static final int FULL_BRIGHT = 15728880; // LightTexture.FULL_BRIGHT

    // Escala do fogo em relação a um bloco inteiro (1.0 = tamanho normal de bloco).
    private static final float FIRE_SCALE = 0.55F;

    // Graus de matiz (hue) percorridos por milissegundo -- controla a
    // velocidade do ciclo de cores do arco-íris.
    private static final float HUE_DEGREES_PER_MS = 360.0F / 3000.0F; // uma volta completa a cada 3s

    public static void renderBothHands(PoseStack poseStack, MultiBufferSource buffer, HumanoidModel<?> model) {
        renderHand(poseStack, buffer, model, HumanoidArm.RIGHT);
        renderHand(poseStack, buffer, model, HumanoidArm.LEFT);
    }

    public static void renderHand(PoseStack poseStack, MultiBufferSource buffer, HumanoidModel<?> model, HumanoidArm arm) {
        poseStack.pushPose();

        // Move o poseStack pra posição/rotação exata da mão (mesmo truque
        // que o próprio jogo usa pra posicionar itens segurados).
        model.translateToHand(arm, poseStack);

        // Reorienta o espaço "de item segurado" (que aponta pra baixo) de
        // volta pro espaço "de bloco" (Y pra cima).
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        boolean isLeft = arm == HumanoidArm.LEFT;
        // Puxado mais perto do punho (em vez de na ponta dos dedos, como um
        // item) pra "abraçar" a mão em vez de flutuar do lado dela.
        poseStack.translate((isLeft ? -1 : 1) / 16.0F, 0.05F, -0.35F);

        // Centraliza e reduz o bloco de fogo (que ocupa um bloco inteiro por
        // padrão) pra caber envolvendo a mão.
        poseStack.scale(FIRE_SCALE, FIRE_SCALE, FIRE_SCALE);
        poseStack.translate(-0.5, -0.5, -0.5);

        MultiBufferSource tinted = tinted(buffer, currentRainbowColor());

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                FIRE_STATE, poseStack, tinted, FULL_BRIGHT, OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }

    /** Cor atual do ciclo arco-íris, baseada no relógio do sistema (fica
     * suave e fluida independente de tick rate/lag do servidor). */
    private static int currentRainbowColor() {
        float hue = (System.currentTimeMillis() * HUE_DEGREES_PER_MS % 360.0F) / 360.0F;
        return Color.HSBtoRGB(hue, 1.0F, 1.0F);
    }

    private static MultiBufferSource tinted(MultiBufferSource delegate, int tintRgb) {
        int r = (tintRgb >> 16) & 0xFF;
        int g = (tintRgb >> 8) & 0xFF;
        int b = tintRgb & 0xFF;
        return renderType -> new TintingVertexConsumer(delegate.getBuffer(renderType), r, g, b);
    }

    /** Encaminha tudo pro VertexConsumer real, só multiplicando a cor de
     * cada vértice pela cor do arco-íris atual -- o brilho/forma/UV do fogo
     * vanilla continuam intocados. */
    private static final class TintingVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int tintR;
        private final int tintG;
        private final int tintB;

        TintingVertexConsumer(VertexConsumer delegate, int tintR, int tintG, int tintB) {
            this.delegate = delegate;
            this.tintR = tintR;
            this.tintG = tintG;
            this.tintB = tintB;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            int r = (red * tintR) / 255;
            int g = (green * tintG) / 255;
            int b = (blue * tintB) / 255;
            delegate.setColor(r, g, b, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            delegate.setNormal(normalX, normalY, normalZ);
            return this;
        }
    }

    private PlasmaFireRenderer() {
    }
}