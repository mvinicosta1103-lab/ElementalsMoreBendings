package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.airsubbendings.temperature.TemperatureElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class TemperatureScrollItem extends AbstractSubbendingScrollItem {

    public TemperatureScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return TemperatureElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return TemperatureElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.temperature_scroll.tooltip";
    }
}
