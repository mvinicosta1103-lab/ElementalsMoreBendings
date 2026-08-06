package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class SandScrollItem extends AbstractSubbendingScrollItem {

    public SandScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return SandElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return SandElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.sand_scroll.tooltip";
    }
}
