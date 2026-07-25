package com.elementals.morebendings.bending.earthsubbendings.bone;

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
 * Renderer da farpa de osso. Mesma receita da {@link
 * com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardEntityRenderer}
 * (que reaproveita a textura vanilla de ametista num "espeto" alongado) --
 * aqui só troca a sprite pra {@code block/bone_block_side} e o tint pra um
 * branco-marfim, já que osso não tem bloco "quebrado em estilhaço" próprio.
 *
 * NOTA: mesma ressalva da CrystalShardEntityRenderer -- não consigo abrir o
 * jogo pra conferir a rotação visualmente. Se a farpa aparecer de lado ou
 * apontando pro lado errado, inverte o sinal do yRot/xRot abaixo.
 */
public class BoneSpikeEntityRenderer extends EntityRenderer<BoneSpikeEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/bone_block_side");

    // Tint marfim/osso -- levemente amarelado, bem mais claro que o bloco cru.
    private static final float R = 0.92f, G = 0.90f, B = 0.78f, A = 1.0f;

    public BoneSpikeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BoneSpikeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        Matrix4f rot = new Matrix4f()
                .rotateY((float) Math.toRadians(-yRot))
                .rotateX((float) Math.toRadians(xRot))
                .scale(0.08f, 0.08f, 0.55f)
                .translate(0.0f, 0.0f, -0.5f);

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
    public ResourceLocation getTextureLocation(BoneSpikeEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");
    }
}