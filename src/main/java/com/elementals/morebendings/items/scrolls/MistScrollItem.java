package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class MistScrollItem extends AbstractSubbendingScrollItem {

    public MistScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return MistElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return MistElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.mist_scroll.tooltip";
    }

    @Override
    void onGranted(Bender bender) {
        MistElement.autoUnlockRoot(bender);
    }
}
