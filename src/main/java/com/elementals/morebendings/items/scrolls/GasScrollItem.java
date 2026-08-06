package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class GasScrollItem extends AbstractSubbendingScrollItem {

    public GasScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return GasElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return GasElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.gas_scroll.tooltip";
    }

    @Override
    void onGranted(Bender bender) {
        GasElement.autoUnlockRoot(bender);
    }
}
