package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class MudScrollItem extends AbstractSubbendingScrollItem {

    public MudScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return MudElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return MudElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.mud_scroll.tooltip";
    }
}
