package com.example.elementalmorebendings.mud.abilities;

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
 * MudBallEntityRenderer
 * <p>
 * Cópia quase 1:1 da AirBallEntityRenderer do jar base (dois cubos
 * translúcidos girando em eixos opostos, desenhados via
 * {@code RenderUtils.drawCube}, que lê o sprite direto do atlas de blocos
 * vanilla) — a diferença é a textura usada.
 * <p>
 * A AirBall usa "elementals:block/air_block" (uma textura própria que o jar
 * base consegue costurar no atlas porque existe um Block registrado
 * (lit_air) cujo blockmodel referencia essa textura). Em vez de recriar
 * esse mesmo mecanismo (registrar um Block novo só pra "costurar" uma
 * textura de mud própria no atlas), usamos direto
 * "minecraft:block/mud" — a textura vanilla do bloco Mud já vem costurada
 * no atlas de blocos por padrão, sem precisar de nenhum registro extra.
 * Isso também é o motivo do cubo antigo aparecer com a textura de
 * "faltando" (preto/roxo ou um bloco genérico malformado): a
 * implementação anterior de Mud Ball nem tinha entidade/renderer própria.
 */
public class MudBallEntityRenderer extends EntityRenderer<MudBallEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/mud");

    public static long firstTime = -1;

    public MudBallEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MudBallEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light) {
        if (firstTime == -1) {
            firstTime = System.currentTimeMillis();
        }
        float rot = (float) (System.currentTimeMillis() - firstTime) / 600;

        matrices.pushPose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.translucentMovingBlock());

        matrices.translate(-0.5f, 0, -0.5f);

        Matrix4f mat = new Matrix4f();
        matrices.translate(0.5f, .5f, 0);
        mat.translate(0, 0, .5f);
        mat.rotate(Axis.YP.rotationDegrees((float) Math.toDegrees(rot)));
        mat.rotate(Axis.ZP.rotationDegrees((float) Math.toDegrees(rot)));
        mat.translate(0, 0, -.5f);
        RenderUtils.drawCube(vertexConsumer, matrices, 255,
                1, 1, 1, 1,
                TEXTURE,
                1, mat,
                false, true, true
        );

        Matrix4f mat2 = new Matrix4f();
        mat2.translate(0, 0, .5f);
        mat2.rotate(Axis.YP.rotationDegrees((float) Math.toDegrees(rot * 2)));
        mat2.rotate(Axis.XP.rotationDegrees((float) Math.toDegrees(rot * 2)));
        mat2.translate(0, 0, -.5f);

        matrices.scale(.85f, .85f, .85f);

        RenderUtils.drawCube(vertexConsumer, matrices, 255,
                1, 1, 1, 1,
                TEXTURE,
                1, mat2,
                false, true, true
        );

        RenderSystem.disableBlend();
        matrices.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MudBallEntity entity) {
        return null;
    }
}