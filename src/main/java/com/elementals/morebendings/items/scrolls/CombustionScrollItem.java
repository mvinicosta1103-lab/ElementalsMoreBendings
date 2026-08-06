package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class CombustionScrollItem extends AbstractSubbendingScrollItem {

    public CombustionScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return CombustionElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return CombustionElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.combustion_scroll.tooltip";
    }

    @Override
    void onGranted(Bender bender) {
        CombustionElement.autoUnlockRoot(bender);
    }
}
