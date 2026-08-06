package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class SpiritScrollItem extends AbstractSubbendingScrollItem {

    public SpiritScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return SpiritElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return SpiritElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.spirit_scroll.tooltip";
    }
}
