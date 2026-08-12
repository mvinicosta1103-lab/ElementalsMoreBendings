package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.watersubbendings.ice.IceElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class IceScrollItem extends AbstractSubbendingScrollItem {

    public IceScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return IceElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return IceElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.ice_scroll.tooltip";
    }
}