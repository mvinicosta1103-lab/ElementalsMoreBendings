package com.elementals.morebendings.bending.airsubbendings.atmosphere;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;

/**
 * Atmosphere Bending — sub-bending de Air, mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Air. Só benders de Air podem obter.
 */
public class AtmosphereElement extends Element {

    public static final String NAME = "Atmosphere";

    public AtmosphereElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("atmospherePressurePoint", 0),
                new Upgrade("atmosphericDome", 1)
        });
        addAbility(new PressurePointAbility(), 0);
        addAbility(new AtmosphericDomeAbility(), 1);
    }

    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new AtmosphereElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isAtmosphereBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this) && bender.getData().canUseUpgrade("atmosphericDome");
    }
}