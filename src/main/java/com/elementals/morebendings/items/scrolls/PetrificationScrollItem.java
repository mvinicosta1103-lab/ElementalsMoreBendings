package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.earthsubbendings.petrification.PetrificationElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class PetrificationScrollItem extends AbstractSubbendingScrollItem {

    public PetrificationScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return PetrificationElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return PetrificationElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.petrification_scroll.tooltip";
    }
}
