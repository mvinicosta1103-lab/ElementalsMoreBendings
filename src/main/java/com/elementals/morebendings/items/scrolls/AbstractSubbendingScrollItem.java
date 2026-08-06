package com.elementals.morebendings.items.scrolls;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.StateDataSaverAndLoader;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.network.packets.common.SyncLevelPacket;
import dev.saperate.elementals.network.packets.common.SyncUpgradeListPacket;
import dev.saperate.elementals.utils.SapsUtils;
import commonnetwork.api.Network;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Base comum pros scrolls de sub-bending do addon (Gas, Mud, Bone, etc.).
 * Mesmo papel que {@code AbstractScrollItem} cumpre no mod base, mas
 * delegando a regra de elegibilidade pro {@code canAcquire(Bender)} de
 * cada sub-bending Element -- cada uma já sabe sua própria regra especial
 * (ex: Glass exige Sand, Bone exige já ter cruzado com um Blood bender)
 * em vez de só checar isSkillTreeComplete de um elemento pai fixo.
 * <br><br>
 * IMPORTANTE sobre Bone: o scroll usa o {@code canAcquire} DE VERDADE,
 * sem o bypass que {@code /morebending grant} aplica (que marca a flag de
 * "já cruzou com um Blood bender" automaticamente). Um item que qualquer
 * jogador pode usar sozinho não deve conseguir pular esse requisito.
 */
public abstract class AbstractSubbendingScrollItem extends Item {

    public AbstractSubbendingScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player user, @NotNull InteractionHand hand) {
        if (!level.isClientSide) {
            ServerPlayer player = (ServerPlayer) user;
            Bender bender = Bender.getBender(player);
            Element element = getElement();

            if (!bender.hasElement(element)) {
                if (!canAcquire(bender)) {
                    SapsUtils.showActionBarTitle(player,
                            Component.literal(getRequirementMessage()).withColor(0xFFC22106));
                    return super.use(level, user, hand);
                }

                bender.addElement(element, true);
                onGranted(bender);
                syncAndPersist(bender, player);

                user.getInventory().removeItem(user.getItemInHand(hand));
            }
        }
        return super.use(level, user, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        tooltip.add(Component.translatable(getTranslatable()));
    }

    /** A sub-bending que este scroll concede. */
    abstract Element getElement();

    /** Delega pra regra de elegibilidade própria da sub-bending (ex: {@code GasElement.canAcquire}). */
    abstract boolean canAcquire(Bender bender);

    /** Chave de tradução do tooltip do item. */
    abstract String getTranslatable();

    /**
     * Mensagem mostrada quando o jogador ainda não atende ao requisito.
     * Sobrescreva se a sub-bending tiver uma regra que valha a pena deixar
     * mais específica (ex: Bone/Glass).
     */
    String getRequirementMessage() {
        return "You feel as if you still have things to learn";
    }

    /**
     * Passo extra pós-addElement pras sub-bendings cujo nó raiz tem preço 0
     * (senão a árvore fica destravada mas com aparência de travada -- ver
     * {@code GasElement#autoUnlockRoot}). Sem efeito por padrão; sobrescrito
     * pelas sub-bendings que precisam (Atmosphere, Gas, Mist, Plasma,
     * Combustion, Bone).
     */
    void onGranted(Bender bender) {
    }

    /**
     * Mesma sincronização + persistência que {@code MoreBendingCommand} faz
     * depois de conceder uma sub-bending por comando -- necessário pra que
     * qualquer nó desbloqueado por {@link #onGranted} (via autoUnlockRoot)
     * chegue no cliente e sobreviva a um reload.
     */
    private static void syncAndPersist(Bender bender, ServerPlayer target) {
        Network.getNetworkHandler().sendToClient(SyncUpgradeListPacket.createFromBender(bender), target);
        Network.getNetworkHandler().sendToClient(SyncLevelPacket.createFromBender(bender), target);
        StateDataSaverAndLoader.getServerState(target.getServer()).setDirty();
    }
}