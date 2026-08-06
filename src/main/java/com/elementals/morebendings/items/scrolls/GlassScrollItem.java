package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.earthsubbendings.glass.GlassElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class GlassScrollItem extends AbstractSubbendingScrollItem {

    public GlassScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return GlassElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return GlassElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.glass_scroll.tooltip";
    }

    @Override
    String getRequirementMessage() {
        return "You need to master Sand Bending before you can learn this";
    }
}
