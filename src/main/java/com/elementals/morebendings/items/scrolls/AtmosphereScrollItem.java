package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.airsubbendings.atmosphere.AtmosphereElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class AtmosphereScrollItem extends AbstractSubbendingScrollItem {

    public AtmosphereScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return AtmosphereElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return AtmosphereElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.atmosphere_scroll.tooltip";
    }

    @Override
    void onGranted(Bender bender) {
        AtmosphereElement.autoUnlockRoots(bender);
    }
}
