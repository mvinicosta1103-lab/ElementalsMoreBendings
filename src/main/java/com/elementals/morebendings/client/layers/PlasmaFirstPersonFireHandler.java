package com.elementals.morebendings.client.layers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.neoforge.client.event.RenderArmEvent;

/**
 * Extensão em PRIMEIRA pessoa da {@link PlasmaHandFlameLayer}.
 *
 * Sem isso, o fogo só aparecia olhando pro personagem de fora (terceira
 * pessoa) ou sendo visto por outro jogador -- porque em primeira pessoa o
 * jogo desenha os braços por um caminho totalmente separado
 * (ItemInHandRenderer), que não passa pelo PlayerRenderer nem pelas
 * RenderLayer normais. {@link RenderArmEvent} é o hook do NeoForge feito
 * exatamente pra plugar nesse caminho separado, disparado logo antes do
 * braço em si ser desenhado na tela.
 */
public final class PlasmaFirstPersonFireHandler {

    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        boolean boosted = ClientPlasmaBoostCache.isActive(player.getUUID());
        boolean clawsFlash = ClientPlasmaClawsFxCache.isActive(player.getUUID());
        if (!boosted && !clawsFlash) return;

        HumanoidModel<?> model = getPlayerModel(player);
        if (model == null) return;

        PlasmaFireRenderer.renderHand(event.getPoseStack(), event.getMultiBufferSource(), model, event.getArm());
    }

    private static HumanoidModel<?> getPlayerModel(AbstractClientPlayer player) {
        EntityRenderer<? super AbstractClientPlayer> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (renderer instanceof PlayerRenderer playerRenderer) {
            return playerRenderer.getModel();
        }
        return null;
    }

    private PlasmaFirstPersonFireHandler() {
    }
}