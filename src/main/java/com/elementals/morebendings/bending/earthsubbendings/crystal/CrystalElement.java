package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;

/**
 * Crystal Bending — sub-bending de Earth, mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.mud.MudElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Earth.
 */
public class CrystalElement extends Element {

    public static final String NAME = "Crystal";

    public CrystalElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("crystalShard", 0)
        });
        addAbility(new CrystalShardAbility(), 0);
    }

    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new CrystalElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    public static boolean canAcquire(Bender bender) {
        Element earth = EarthElement.get();
        return bender.hasElement(earth) && earth.isSkillTreeComplete(bender);
    }

    public static boolean isCrystalBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this) && bender.getData().canUseUpgrade("crystalShard");
    }
}