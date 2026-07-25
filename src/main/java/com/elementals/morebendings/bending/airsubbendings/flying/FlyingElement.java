package com.elementals.morebendings.bending.airsubbendings.flying;

import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.data.SubbendingType;
import com.elementals.morebendings.registry.ModAttachments;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.air.AirElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ponto central de lógica da sub-bending Flying. Mesmo papel que
 * {@link com.elementals.morebendings.bending.airsubbendings.gas.GasElement}
 * cumpre pro Gas: só confere se o jogador é bender de Air no mod base e se
 * ele tem a sub-bending Flying desbloqueada (via {@code /morebending
 * grant}) — não guarda estado nenhum, isso fica em {@link FlyingAbility}
 * (voando ou não, estamina) e em {@link PlayerSubbendingData} (desbloqueio).
 */
public final class FlyingElement {

    public static final String SUBBENDING_NAME = "Flying";

    /**
     * Um jogador só pode voar se: (1) já for bender de Air no mod base, e
     * (2) tiver recebido a sub-bending Flying. Igual ao Gas, é tratado como
     * uma especialização de Air, não uma bending independente.
     */
    public static boolean isFlyingBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        if (bender == null || !bender.hasElement(AirElement.get())) {
            return false;
        }
        PlayerSubbendingData data = player.getData(ModAttachments.SUBBENDINGS);
        return data.has(SubbendingType.FLYING);
    }

    private FlyingElement() {
    }
}