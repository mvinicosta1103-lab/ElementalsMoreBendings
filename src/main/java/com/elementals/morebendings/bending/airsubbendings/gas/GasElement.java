package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;
import net.minecraft.server.level.ServerPlayer;

public class GasElement extends Element {

    public static final String NAME = "Gas";

    public static final String GAS_CLOUD = "gasCloud";
    public static final String GAS_CLOUD_SIZE_I = "gasCloudSizeI";
    public static final String GAS_CLOUD_SIZE_II = "gasCloudSizeII";
    public static final String GAS_VENT_I = "gasVentI";
    public static final String GAS_VENT_II = "gasVentII";

    public static final String GAS_SUFFOCATE = "gasSuffocate";
    public static final String GAS_SUFFOCATE_DAMAGE_I = "gasSuffocateDamageI";
    public static final String GAS_SUFFOCATE_DAMAGE_II = "gasSuffocateDamageII";

    public static final String GAS_LEAK = "gasLeak";
    public static final String GAS_LEAK_DURATION_I = "gasLeakDurationI";

    public static final String GAS_IGNITE = "gasIgnite";

    public static final String GAS_JET = "gasJet";
    public static final String GAS_JET_FORCE_I = "gasJetForceI";

    public static final String GAS_MIASMA = "gasMiasma";
    public static final String GAS_MIASMA_RADIUS_I = "gasMiasmaRadiusI";

    public static final String GAS_CORROSIVE = "gasCorrosiveMist";

    public GasElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(GAS_CLOUD, new Upgrade[]{
                        new Upgrade(GAS_CLOUD_SIZE_I, new Upgrade[]{
                                new Upgrade(GAS_CLOUD_SIZE_II, 1)
                        }, 1),
                        new Upgrade(GAS_VENT_I, new Upgrade[]{
                                new Upgrade(GAS_VENT_II, 1)
                        }, 1),
                        new Upgrade(GAS_SUFFOCATE, new Upgrade[]{
                                new Upgrade(GAS_SUFFOCATE_DAMAGE_I, new Upgrade[]{
                                        new Upgrade(GAS_SUFFOCATE_DAMAGE_II, 1)
                                }, 1),
                                new Upgrade(GAS_LEAK, new Upgrade[]{
                                        new Upgrade(GAS_LEAK_DURATION_I, 1)
                                }, 1)
                        }, 1),
                        new Upgrade(GAS_IGNITE, new Upgrade[]{
                                new Upgrade(GAS_JET, new Upgrade[]{
                                        new Upgrade(GAS_JET_FORCE_I, 1)
                                }, 1)
                        }, 1),
                        new Upgrade(GAS_MIASMA, new Upgrade[]{
                                new Upgrade(GAS_MIASMA_RADIUS_I, 1)
                        }, 1),
                        new Upgrade(GAS_CORROSIVE, 1)
                }, 0)
        });

        // Keybind slots individuais (0 = Key 1, 1 = Key 2, 2 = Key 3, etc.)
        addAbility(new GasCloudAbility(), 0);
        addAbility(new GasSuffocateAbility(), 1);
        addAbility(new GasLeakAbility(), 2);
        addAbility(new GasIgniteAbility(), 3);
        addAbility(new GasPropulsionAbility(), 4);
        addAbility(new GasMiasmaAbility(), 5);
        addAbility(new CorrosiveGasAbility(), 6);
    }

    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new GasElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isGasBender(Bender bender) {
        return bender.hasElement(get());
    }

    public static boolean isGasBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender != null && isGasBender(bender);
    }

    public static void autoUnlockRoot(Bender bender) {
        Element gas = get();
        if (gas != null && gas.root != null && gas.root.children != null) {
            for (Upgrade rootUpgrade : gas.root.children) {
                bender.getData().upgrades.put(rootUpgrade, true);
            }
        }
    }

    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        Bender bender = Bender.getBender(player);
        return bender != null && bender.getData().canUseUpgrade(upgradeName);
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        if (!bender.hasElement(this)) {
            return false;
        }
        return bender.getData().canUseUpgrade(GAS_CLOUD_SIZE_II)
                && bender.getData().canUseUpgrade(GAS_VENT_II)
                && bender.getData().canUseUpgrade(GAS_SUFFOCATE_DAMAGE_II)
                && bender.getData().canUseUpgrade(GAS_LEAK_DURATION_I)
                && bender.getData().canUseUpgrade(GAS_IGNITE)
                && bender.getData().canUseUpgrade(GAS_JET_FORCE_I)
                && bender.getData().canUseUpgrade(GAS_MIASMA_RADIUS_I)
                && bender.getData().canUseUpgrade(GAS_CORROSIVE);
    }
}