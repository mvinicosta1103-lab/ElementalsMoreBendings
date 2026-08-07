package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;
import net.minecraft.server.level.ServerPlayer;

/**
 * Gas Bending — sub-bending de Air.
 *
 * Estrutura atualizada:
 *  - Slot 0: Gas Cloud (gasCloud)
 *  - Slot 1: Gas Suffocate (gasSuffocate)
 *  - Slot 2: Gas Leak (gasLeak)
 *  - Slot 3: Gas Ignite (gasIgnite)
 */
public class GasElement extends Element {

    public static final String NAME = "Gas";

    // ---- Nomes dos nós ----
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

    public GasElement() {
        super(NAME, new Upgrade[]{
                // Ramo 1: Gas Cloud (Habilidade base + Utilitário)
                new Upgrade(GAS_CLOUD, new Upgrade[]{
                        new Upgrade(GAS_CLOUD_SIZE_I, new Upgrade[]{
                                new Upgrade(GAS_CLOUD_SIZE_II, 1)
                        }, 1),
                        new Upgrade(GAS_VENT_I, new Upgrade[]{
                                new Upgrade(GAS_VENT_II, 1)
                        }, 1)
                }, 0),

                // Ramo 2: Gas Suffocate (Dano de Burst)
                new Upgrade(GAS_SUFFOCATE, new Upgrade[]{
                        new Upgrade(GAS_SUFFOCATE_DAMAGE_I, new Upgrade[]{
                                new Upgrade(GAS_SUFFOCATE_DAMAGE_II, 1)
                        }, 1)
                }, 0),

                // Ramo 3: Gas Leak (Nuvem Residual + Terra Infértil)
                new Upgrade(GAS_LEAK, new Upgrade[]{
                        new Upgrade(GAS_LEAK_DURATION_I, 1)
                }, 0),

                // Ramo 4: Gas Ignite (Incêndio de Área)
                new Upgrade(GAS_IGNITE, 0)
        });

        // Registro independente em cada slot
        addAbility(new GasCloudAbility(), 0);
        addAbility(new GasSuffocateAbility(), 1);
        addAbility(new GasLeakAbility(), 2);
        addAbility(new GasIgniteAbility(), 3);
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

    /**
     * Auto-desbloqueia os nós raiz das habilidades (custo 0) para que o jogador
     * consiga visualizar e comprar os sub-upgrades das 4 árvores.
     */
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
                && bender.getData().canUseUpgrade(GAS_IGNITE);
    }
}