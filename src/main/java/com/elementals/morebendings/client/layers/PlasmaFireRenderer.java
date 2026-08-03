package com.elementals.morebendings.client.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
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

    // Escala LARGURA/PROFUNDIDADE (X/Z) -- fina, do tamanho de um
    // antebraço, não de um bloco inteiro. Um pouco maior que a largura real
    // do braço (~0.19-0.25 dependendo do modelo Steve/Alex) de propósito,
    // pra "vestir" o braço e sobressair BEM de leve, não engolir ele.
    private static final float FIRE_WIDTH_SCALE = 0.26F;

    // Escala ALTURA (Y) -- mais alta que larga, pra virar uma "coluna" de
    // fogo em volta do braço em vez de um blob quase cúbico. Reduzida em
    // relação à versão anterior (era 1.05) pra não passar tanto do
    // cotovelo.
    private static final float FIRE_HEIGHT_SCALE = 0.40F;

    // Quanto descer a partir do OMBRO (pivô do braço) até o ponto onde o
    // "punho de fogo" deve ficar centrado -- em unidades de bloco (1.0 =
    // comprimento total do braço no modelo). ~0.62 cai perto do
    // pulso/antebraço.
    private static final float DROP_FROM_SHOULDER = 0.62F;

    // Graus de matiz (hue) percorridos por milissegundo -- controla a
    // velocidade do ciclo de cores do arco-íris.
    private static final float HUE_DEGREES_PER_MS = 360.0F / 3000.0F; // uma volta completa a cada 3s

    public static void renderBothHands(PoseStack poseStack, MultiBufferSource buffer, HumanoidModel<?> model) {
        renderHand(poseStack, buffer, model, HumanoidArm.RIGHT);
        renderHand(poseStack, buffer, model, HumanoidArm.LEFT);
    }

    public static void renderHand(PoseStack poseStack, MultiBufferSource buffer, HumanoidModel<?> model, HumanoidArm arm) {
        poseStack.pushPose();

        // Ancora DIRETO no bone do braço (pivô no ombro, já rotacionado
        // pra pose atual: balanço andando, mirando, etc.) -- NÃO no espaço
        // de "item segurado" que o translateToHand usa. Esse espaço de
        // item projeta o objeto pra FORA da mão, na convenção usada pra
        // itens/blocos empunhados, o que deixava o fogo flutuando torto
        // "na frente do peito" em vez de grudado no braço.
        //
        // Usa os campos PÚBLICOS rightArm/leftArm em vez de getArm(...)
        // (esse é protected em HumanoidModel -- não dá pra chamar de fora
        // de uma subclasse/do mesmo pacote).
        ModelPart armPart = (arm == HumanoidArm.LEFT) ? model.leftArm : model.rightArm;
        armPart.translateAndRotate(poseStack);

        // No espaço cru do bone (convenção antiga do formato de modelo do
        // Minecraft), +Y local é "descendo o braço" (ombro -> mão) -- é
        // por isso que braços/pernas são desenhados nesse sentido a
        // partir do pivô. (Antes eu tinha usado NEGATIVO aqui, achando
        // que seria o contrário -- por isso o fogo subiu na direção
        // errada e foi parar acima da cabeça.)
        poseStack.translate(0, DROP_FROM_SHOULDER, 0);

        // Agora inverte esse eixo Y (rotação de 180° em X) ANTES de
        // desenhar o bloco: sem isso, o "topo" do modelo de fogo (que
        // deveria apontar pra cima, saindo da mão em direção ao cotovelo)
        // ficaria apontando pra baixo, enfiado no chão. Com essa rotação,
        // o +Y do bloco passa a apontar de volta na direção do ombro --
        // ou seja, "pra cima" de verdade, saindo da mão.
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

        // Escala NÃO uniforme: fino em X/Z (largura/profundidade do
        // antebraço), alto em Y (comprimento do "manguito" de fogo). O
        // scale uniforme antigo criava um cubo grosso; esse aqui estica o
        // mesmo modelo de bloco numa coluna fina e vertical.
        poseStack.scale(FIRE_WIDTH_SCALE, FIRE_HEIGHT_SCALE, FIRE_WIDTH_SCALE);

        // Centraliza só X/Z. Y fica em 0 (SEM subtrair 0.5) de propósito:
        // isso deixa a base do bloco de fogo (y=0, o "núcleo" da chama)
        // exatamente no ponto-âncora (a mão), esticando pra cima a partir
        // dali -- em vez de centralizar o bloco inteiro em volta da mão
        // (que deixava metade da chama enterrada "dentro" da mão/braço).
        poseStack.translate(-0.5, 0, -0.5);

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