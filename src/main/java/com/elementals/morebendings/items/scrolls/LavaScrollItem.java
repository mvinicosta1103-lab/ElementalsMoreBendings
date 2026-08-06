package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.earthsubbendings.lava.LavaElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class LavaScrollItem extends AbstractSubbendingScrollItem {

    public LavaScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return LavaElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return LavaElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.lava_scroll.tooltip";
    }
}
