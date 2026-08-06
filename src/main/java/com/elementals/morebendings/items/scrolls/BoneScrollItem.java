package com.elementals.morebendings.items.scrolls;

import com.elementals.morebendings.bending.earthsubbendings.bone.BoneElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;

public class BoneScrollItem extends AbstractSubbendingScrollItem {

    public BoneScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    Element getElement() {
        return BoneElement.get();
    }

    @Override
    boolean canAcquire(Bender bender) {
        return BoneElement.canAcquire(bender);
    }

    @Override
    String getTranslatable() {
        return "item.elementalsmorebendings.bone_scroll.tooltip";
    }

    @Override
    String getRequirementMessage() {
        return "You feel a pull, but you have never crossed paths with a Blood bender";
    }

    @Override
    void onGranted(Bender bender) {
        BoneElement.autoUnlockRoot(bender);
    }
}
