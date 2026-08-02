package com.elementals.morebendings.bending.watersubbendings.plant;

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
 * Renderer do "modelo" do cipó de {@code vineGrasp} (ver {@link
 * PlantVineGraspVisualEntity}). Mesma receita de {@code
 * PlantThornVolleyEntityRenderer}/{@code CrystalShardEntityRenderer}: um
 * "caixote" texturizado com uma sprite do atlas de blocos, rotacionado pra
 * apontar da entidade (mão do caster) até a vítima -- só que aqui a escala
 * no eixo Z (comprimento) é DINÂMICA, lida de {@link
 * PlantVineGraspVisualEntity#getLength()} a cada frame, em vez de uma
 * constante fixa, porque a vinha estica/encolhe conforme os dois pontos se
 * movem. A seção transversal é bem mais fina que a farpa de thornVolley
 * (0.05 em vez de 0.08) pra parecer um cipó fino, não um caule grosso.
 * <p>
 * Textura: {@code block/vine} (sprite vanilla do atlas de blocos, textura
 * de vinha de verdade -- diferente do cactus_side reaproveitado pelo
 * thornVolley), com tint verde-escuro por cima pra ficar mais saturado.
 * <p>
 * NOTA: mesma ressalva de {@code CrystalShardEntityRenderer} -- não consigo
 * abrir o jogo pra conferir visualmente a rotação. Se o cipó aparecer
 * apontando pro lado errado, inverte o sinal do yRot/xRot abaixo.
 */
public class PlantVineGraspVisualEntityRenderer extends EntityRenderer<PlantVineGraspVisualEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/vine");

    // Tint verde-escuro saturado sobre a textura de vinha.
    private static final float R = 0.25f, G = 0.55f, B = 0.2f, A = 1.0f;

    /** Espessura da seção transversal do cipó (largura/altura do "caixote"). */
    private static final float THICKNESS = 0.05f;

    public PlantVineGraspVisualEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PlantVineGraspVisualEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float length = entity.getLength();
        if (length < 0.05f) {
            return; // captura acabou de nascer/vai sumir nesse frame -- nada pra desenhar ainda
        }

        poseStack.pushPose();

        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // Igual CrystalShardEntityRenderer: alinha o eixo Z local (comprimento)
        // com a direção caster->vítima, depois achata a seção transversal e
        // estica pelo comprimento de verdade (não uma constante -- ver javadoc
        // da classe). translate(0,0,-length) porque a entidade "vive" na PONTA
        // de origem (mão do caster), não no meio do cipó -- diferente da farpa
        // de thornVolley, que centraliza o espeto na própria entidade.
        Matrix4f rot = new Matrix4f()
                .rotateY((float) Math.toRadians(-yRot))
                .rotateX((float) Math.toRadians(xRot))
                .scale(THICKNESS, THICKNESS, length)
                .translate(0.0f, 0.0f, -1.0f);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucentMovingBlock());
        RenderUtils.drawCube(vertexConsumer, poseStack, packedLight, R, G, B, A, TEXTURE,
                1.0f, rot, false, true, true);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(PlantVineGraspVisualEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}