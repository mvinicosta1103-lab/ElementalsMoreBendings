package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.airsubbendings.sound.SoundElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class SoundScrollItem extends AbstractSubbendingScrollItem {

    public SoundScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return SoundElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return SoundElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.sound_scroll.tooltip";
    }
}
