package com.elementals.morebendings.bending.earthsubbendings.petrification;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;

/**
 * Petrification Bending — sub-bending de Earth, mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.mud.MudElement}:
 * Element de verdade, registrada no mod base, com duas habilidades raiz --
 * uma ofensiva ({@link PetrifyingTouchAbility}) e uma defensiva ({@link
 * StaticLegsAbility}).
 *
 * REGRA DE AQUISIÇÃO: igual Mud/Crystal/Sand -- exige ter Earth E ter
 * masterizado a árvore de Earth inteira (ver {@link #canAcquire}).
 */
public class PetrificationElement extends Element {

    public static final String NAME = "Petrification";

    public PetrificationElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("petrifyingTouch", 0), // grátis -- ver PetrifyingTouchAbility
                new Upgrade("staticLegs", 0)       // grátis -- ver StaticLegsAbility
        });
        addAbility(new PetrifyingTouchAbility(), 0);
        addAbility(new StaticLegsAbility(), 1);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new PetrificationElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Earth E já comprou todos os nós da
     * árvore de skills de Earth (masterizou o elemento base).
     */
    public static boolean canAcquire(Bender bender) {
        Element earth = EarthElement.get();
        return bender.hasElement(earth) && earth.isSkillTreeComplete(bender);
    }

    public static boolean isPetrificationBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade("petrifyingTouch")
                && bender.getData().canUseUpgrade("staticLegs");
    }
}