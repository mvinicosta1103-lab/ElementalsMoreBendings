package com.elementals.morebendings.bending.airsubbendings.gas;

import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.data.SubbendingType;
import com.elementals.morebendings.registry.ModAttachments;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ponto central de lógica da sub-bending Gas. Não estende
 * {@code dev.saperate.elementals.elements.Element} de propósito — ver
 * {@link GasSkillTree} pra explicação. Isso aqui só cola duas coisas:
 * o {@code Bender} do mod base (pra saber se o jogador é Air bender) e o
 * {@link PlayerSubbendingData} do addon (pra saber se ele tem Gas e o
 * progresso da árvore dele).
 */
public final class GasElement {

    public static final String SUBBENDING_NAME = "Gas Bending";

    /**
     * Um jogador só pode usar Gas se: (1) já for bender de Air no mod base,
     * e (2) tiver recebido a sub-bending Gas (via {@code /morebending grant}).
     * Isso é intencional — Gas é tratado como uma especialização de Air, não
     * uma bending independente.
     */
    public static boolean isGasBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        if (bender == null || !bender.hasElement(AirElement.get())) {
            return false;
        }
        PlayerSubbendingData data = player.getData(ModAttachments.SUBBENDINGS);
        return data.has(SubbendingType.GAS);
    }

    /**
     * @return true se o nó existe, o jogador já pode comprá-lo (pai
     * desbloqueado) e tem pontos suficientes — e, nesse caso, já desconta os
     * pontos e marca o nó como comprado. Chame só no servidor.
     */
    public static boolean tryBuyUpgrade(ServerPlayer player, String upgradeName) {
        if (!isGasBender(player)) {
            return false;
        }
        Upgrade upgrade = GasSkillTree.byName(upgradeName);
        if (upgrade == null) {
            return false;
        }
        PlayerSubbendingData data = player.getData(ModAttachments.SUBBENDINGS);
        if (data.hasUpgrade(SubbendingType.GAS, upgradeName)) {
            return false; // já comprado
        }
        if (!canBuy(data, upgrade)) {
            return false; // pai ainda não desbloqueado
        }
        if (!data.spendPoints(SubbendingType.GAS, upgrade.price)) {
            return false; // sem pontos suficientes
        }
        data.unlockUpgrade(SubbendingType.GAS, upgradeName);
        return true;
    }

    /**
     * Mesma regra que {@code Upgrade#canBuy}, mas consultando o
     * {@link PlayerSubbendingData} do addon em vez do
     * {@code HashMap<Upgrade, Boolean>} interno do mod base.
     */
    private static boolean canBuy(PlayerSubbendingData data, Upgrade upgrade) {
        if (upgrade.parent == null || upgrade.parent.parent == null) {
            return true; // gasCloud (filho direto da raiz) é sempre comprável
        }
        return data.hasUpgrade(SubbendingType.GAS, upgrade.parent.name);
    }

    /** Atalho pras abilities: o nó precisa ter sido comprado pelo jogador. */
    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        PlayerSubbendingData data = player.getData(ModAttachments.SUBBENDINGS);
        return data.hasUpgrade(SubbendingType.GAS, upgradeName);
    }

    private GasElement() {
    }
}