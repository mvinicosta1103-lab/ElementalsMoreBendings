package com.elementals.morebendings.bending.avatarstate.moves;

import com.elementals.morebendings.bending.avatarstate.AvatarBendingGrantAbility;
import com.elementals.morebendings.bending.avatarstate.AvatarBendingRemoveAbility;
import com.elementals.morebendings.bending.avatarstate.AvatarBendingSelection;
import com.elementals.morebendings.bending.avatarstate.AvatarStateManager;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;

/**
 * "Energy Element" — só existe enquanto o jogador está no Avatar State
 * (concedido/revogado por {@link AvatarStateManager}, nunca adquirido do
 * jeito normal). Representa a energybending: a capacidade de CONCEDER ou
 * REMOVER a dobra-base de outro jogador pela vontade do Avatar.
 * <p>
 * As duas abilities ({@link AvatarBendingGrantAbility}/{@link
 * AvatarBendingRemoveAbility}) já existiam antes desta classe (chamadas
 * direto pelas packets de tecla dedicada, {@code
 * CastAvatarBendingGrantPacket}/{@code CastAvatarBendingRemovePacket|},
 * ambas já gated por {@code AvatarStateManager#isActive}) -- esta classe
 * só as formaliza como um Element de verdade, pra aparecerem na árvore de
 * skills / HUD do Avatar State como qualquer outra bending. Qual dos 4
 * elementos-base é concedido/removido continua sendo escolhido por
 * {@link AvatarBendingSelection} (tecla de ciclar, independente da
 * árvore).
 */
public class EnergyElement extends Element {

    public static final String NAME = "Energy";

    public EnergyElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("energyGrant", 0),
                new Upgrade("energyStrip", 0),
        });
        addAbility(new AvatarBendingGrantAbility(), 0);
        addAbility(new AvatarBendingRemoveAbility(), 1);

        registerUpgradeKeybind("energyGrant", 0);
        registerUpgradeKeybind("energyStrip", 1);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new EnergyElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    public static boolean isEnergyBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade("energyGrant")
                && bender.getData().canUseUpgrade("energyStrip");
    }
}