package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class PlasmaScrollItem extends AbstractSubbendingScrollItem {

    public PlasmaScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return PlasmaElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return PlasmaElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.plasma_scroll.tooltip";
    }

    @Override
    void onGranted(Bender bender) {
        PlasmaElement.autoUnlockRoot(bender);
    }
}
