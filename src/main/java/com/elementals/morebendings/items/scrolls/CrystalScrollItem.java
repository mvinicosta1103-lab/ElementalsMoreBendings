package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class CrystalScrollItem extends AbstractSubbendingScrollItem {

    public CrystalScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return CrystalElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return CrystalElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.crystal_scroll.tooltip";
    }
}
