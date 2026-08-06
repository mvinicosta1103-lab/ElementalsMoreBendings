package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.earthsubbendings.metal.MetalElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

/**
 * Scroll do Metal Mastery -- mesmo padrão de {@link LavaScrollItem}: não
 * precisa de {@code onGranted}/autoUnlockRoot porque os 3 upgrades de
 * {@link MetalElement} (metalSense, metalSlam, chestplateDevelop) já são
 * raízes soltas de preço 0, sem nó sintético escondendo filhos atrás dele
 * (diferente de Gas/Mist/Plasma/Combustion/Bone).
 */
public class MetalMasteryScrollItem extends AbstractSubbendingScrollItem {

    public MetalMasteryScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return MetalElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return MetalElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.metal_mastery_scroll.tooltip";
    }

    @Override
    String getRequirementMessage() {
        return "You feel a pull, but you have yet to master the way of Metal Bending";
    }
}