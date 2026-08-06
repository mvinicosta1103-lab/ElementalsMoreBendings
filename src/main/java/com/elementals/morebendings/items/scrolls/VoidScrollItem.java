package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.airsubbendings.voiding.VoidElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class VoidScrollItem extends AbstractSubbendingScrollItem {

    public VoidScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return VoidElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return VoidElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.void_scroll.tooltip";
    }
}
