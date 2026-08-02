package com.elementals.morebendings.bending.firesubbendings.combustion;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer do {@link CombustionBoltEntity} -- e ele DE PROPÓSITO não
 * desenha nada. Igual ao tiro de P'Li/Combustion Man, o projétil em voo é
 * invisível: nenhum modelo, nenhum cubo, nenhum brilho seguindo o alvo
 * pelo ar. A entidade ainda existe no mundo (pra colisão/homing), só não
 * é renderizada -- o único feedback visual que o jogo dá é o clarão no
 * disparo e a explosão no impacto (ambos fora desta classe).
 *
 * Mantido como classe própria (em vez de simplesmente não registrar um
 * renderer) porque o NeoForge exige um {@link EntityRenderer} registrado
 * pra qualquer entidade cliente-visível -- este aqui só cumpre a
 * exigência sem desenhar nada.
 */
public class CombustionBoltEntityRenderer extends EntityRenderer<CombustionBoltEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");

    public CombustionBoltEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CombustionBoltEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Sem corpo de propósito -- o bolt é invisível em pleno voo.
    }

    @Override
    public ResourceLocation getTextureLocation(CombustionBoltEntity entity) {
        return TEXTURE;
    }
}