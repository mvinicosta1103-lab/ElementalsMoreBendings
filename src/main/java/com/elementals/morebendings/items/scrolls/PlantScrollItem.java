package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.watersubbendings.plant.PlantElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class PlantScrollItem extends AbstractSubbendingScrollItem {

    public PlantScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return PlantElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return PlantElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.plant_scroll.tooltip";
    }
}
