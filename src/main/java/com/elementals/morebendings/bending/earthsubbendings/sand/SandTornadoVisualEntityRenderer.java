package com.elementals.morebendings.bending.earthsubbendings.sand;

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
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renderer de {@link SandTornadoVisualEntity} -- não é um espeto, é um
 * FUNIL: várias camadas horizontais de blocos de areia (e areia
 * vermelha, pra dar textura/variação) empilhadas do chão até o topo,
 * cada camada girando ao redor do eixo vertical e alargando conforme
 * sobe -- igual um funil de tornado de verdade, estreito perto do chão
 * e mais largo no topo. As camadas de baixo giram mais rápido que as de
 * cima ({@link #BASE_SPIN_SPEED} vs {@link #TOP_SPIN_SPEED}), então o
 * funil não parece um cilindro rígido rodando inteiro junto, e sim um
 * vórtice de verdade com camadas se "adiantando" umas às outras.
 * <p>
 * Toda a geometria (fase de giro por camada, escolha de textura por
 * bloco, leve variação de tamanho) é derivada de {@link
 * SandTornadoVisualEntity#getSeed()} via {@link #hash01} -- mesmo funil,
 * pixel a pixel, em todo cliente conectado, em vez de cada um sortear
 * uma variação diferente.
 * <p>
 * Diferente de {@code MagmaSpikeVisualEntityRenderer}, não tem retração
 * por tempo de vida fixo: o funil não encolhe sozinho enquanto vivo, só
 * nasce (cresce nos primeiros {@link #GROW_TICKS} ticks, um "sugar" do
 * chão pra cima) e depois some de uma vez quando {@code
 * SandTornadoState#release()} descarta a entidade diretamente -- porque
 * a duração real varia com o jogador segurando agachado, não é fixa.
 */
public class SandTornadoVisualEntityRenderer extends EntityRenderer<SandTornadoVisualEntity> {

    private static final ResourceLocation SAND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/sand");
    private static final ResourceLocation RED_SAND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/red_sand");

    private static final float TOTAL_HEIGHT = 6.4f;
    private static final float BASE_RADIUS = 0.18f;   // raio bem perto do chão -- funil "beliscando" o solo
    private static final float TOP_RADIUS = 1.55f;    // raio no topo -- boca larga do funil
    private static final int LAYER_COUNT = 16;

    /** Graus/tick de giro na base do funil; o topo gira mais devagar (efeito vórtice, não cilindro rígido). */
    private static final float BASE_SPIN_SPEED = 11.0f;
    private static final float TOP_SPIN_SPEED = 5.0f;

    private static final int GROW_TICKS = 12;

    public SandTornadoVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SandTornadoVisualEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int seed = entity.getSeed();
        float age = entity.tickCount + partialTick;
        // Cresce do chão pra cima nos primeiros GROW_TICKS -- não usa mín. 0 pra nunca zerar o tamanho do bloco.
        float growScale = Mth.clamp(age / GROW_TICKS, 0.05f, 1.0f);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());

        poseStack.pushPose();

        // Fase inicial por instância -- dois tornados próximos não giram em sincronia.
        float phaseOffset = hash01(seed, 0) * 360.0f;

        for (int layer = 0; layer < LAYER_COUNT; layer++) {
            float t = layer / (float) (LAYER_COUNT - 1); // 0 (chão) .. 1 (topo)
            float y = TOTAL_HEIGHT * t * growScale;

            // Alarga conforme sobe -- curva côncava (t^0.75) pra não ficar um cone reto/artificial.
            float radius = (BASE_RADIUS + (TOP_RADIUS - BASE_RADIUS) * (float) Math.pow(t, 0.75)) * growScale;
            if (radius < 0.05f) {
                continue;
            }

            float spinSpeed = Mth.lerp(t, BASE_SPIN_SPEED, TOP_SPIN_SPEED);
            float layerPhase = hash01(seed, layer + 1) * 360.0f;
            float spin = phaseOffset + layerPhase + spinSpeed * age;

            // Camada estreita perto do chão precisa de poucos blocos; conforme o raio cresce,
            // mais blocos pra manter a circunferência preenchida sem ficar espaçada demais.
            int clumpCount = Math.max(3, Math.round(3 + radius * 3.2f));
            float alpha = Mth.lerp(t, 1.0f, 0.55f); // topo mais disperso/poeirento, tipo areia se desfazendo

            for (int i = 0; i < clumpCount; i++) {
                float clumpJitter = (hash01(seed, layer * 97 + i * 13 + 3) - 0.5f) * 18.0f;
                float angleDeg = spin + i * (360.0f / clumpCount) + clumpJitter;
                float angleRad = angleDeg * Mth.DEG_TO_RAD;

                float sizeJitter = 0.75f + hash01(seed, layer * 53 + i * 7 + 11) * 0.5f; // ~0.75..1.25
                float size = Mth.clamp(0.22f + radius * 0.14f, 0.16f, 0.55f) * sizeJitter * growScale;

                float x = Mth.cos(angleRad) * radius;
                float z = Mth.sin(angleRad) * radius;

                boolean useRedSand = hash01(seed, layer * 29 + i * 17 + 5) < 0.22f;
                ResourceLocation texture = useRedSand ? RED_SAND_TEXTURE : SAND_TEXTURE;

                drawClump(vertexConsumer, poseStack, packedLight, x, y, z, size, alpha, texture);
            }
        }

        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    /** Um "punhado" de areia do funil: cubo posicionado em (x, y, z) local, já escalado. */
    private void drawClump(VertexConsumer vertexConsumer, PoseStack poseStack, int packedLight,
                           float x, float y, float z, float size, float alpha, ResourceLocation texture) {
        Matrix4f mat = new Matrix4f()
                .scale(size, size, size)
                .translate(x / size, y / size, z / size);
        RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, 1.0f, 1.0f, 1.0f, alpha, texture,
                1.0f, mat, false, true, true);
    }

    /** Hash determinístico 0..1 a partir de {@code seed} + um "sal" (índice de camada/bloco). */
    private static float hash01(int seed, int salt) {
        int h = seed * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= (h >>> 16);
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    @Override
    public ResourceLocation getTextureLocation(SandTornadoVisualEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}