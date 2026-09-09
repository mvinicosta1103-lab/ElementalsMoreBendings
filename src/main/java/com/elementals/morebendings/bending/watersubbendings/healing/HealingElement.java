package com.elementals.morebendings.bending.watersubbendings.healing;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.water.WaterElement;

/**
 * Healing Bending — sub-bending de Water, mesmo padrão de {@link
 * com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Water. Três habilidades raiz, todas grátis (preço 0), cada uma com
 * uma mecânica diferente:
 *
 *  - healingTouch: cura instantânea num alvo mirado (ou no próprio caster,
 *    se nada for mirado). Ver {@link HealingTouchAbility}.
 *  - risingTide: aplica Regeneração ao longo do tempo num alvo mirado (ou
 *    no próprio caster). Ver {@link RisingTideAbility}.
 *  - sanctuaryPulse: pulso de cura em área ao redor do caster, cura todo
 *    mundo que não seja um mob hostil. Ver {@link SanctuaryPulseAbility}.
 *  - bloomingGround: aplica o mesmo efeito de farinha de osso (bonemeal)
 *    numa área ao redor do ponto mirado. Ver {@link BloomingGroundAbility}.
 *  - witheringTouch: aninhada dentro do ramo de healingTouch -- o
 *    oposto dela. Toque de curto alcance que amaldiçoa a vítima com
 *    Veneno + Fome reaplicados sem parar e dano periódico, até matar --
 *    SEM duração, só reverte se o MESMO caster tocar a vítima de novo.
 *    Ver {@link WitheringTouchAbility} / {@link WitheringTouchManager}.
 */
public class HealingElement extends Element {

    public static final String NAME = "Healing";

    public HealingElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("healingTouch", new Upgrade[]{
                        new Upgrade("witheringTouch", 0)
                }, 0),                               // grátis
                new Upgrade("risingTide", 0),         // grátis
                new Upgrade("sanctuaryPulse", 0),     // grátis
                new Upgrade("bloomingGround", 0)      // grátis
        });
        addAbility(new HealingTouchAbility(), 0);
        addAbility(new RisingTideAbility(), 1);
        addAbility(new SanctuaryPulseAbility(), 2);
        addAbility(new BloomingGroundAbility(), 3);
        addAbility(new WitheringTouchAbility(), 4);

        // Sem isso, Element#getKeybindSlotForUpgrade() sobe a árvore, não
        // acha witheringTouch em upgradeKeybinds e cai pro índice do RAMO
        // da raiz (healingTouch, 0) em vez do índice real da ability (4) --
        // mesmo fix que PlantElement/rootSnare já usa.
        registerUpgradeKeybind("healingTouch", 0);
        registerUpgradeKeybind("risingTide", 1);
        registerUpgradeKeybind("sanctuaryPulse", 2);
        registerUpgradeKeybind("bloomingGround", 3);
        registerUpgradeKeybind("witheringTouch", 4);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new HealingElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Water E já comprou todos os nós da
     * árvore de skills de Water (masterizou o elemento base) -- mesma
     * regra de Plant/Spirit, gated atrás de Water.
     */
    public static boolean canAcquire(Bender bender) {
        Element water = WaterElement.get();
        return bender.hasElement(water) && water.isSkillTreeComplete(bender);
    }

    public static boolean isHealingBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade("healingTouch")
                && bender.getData().canUseUpgrade("risingTide")
                && bender.getData().canUseUpgrade("sanctuaryPulse")
                && bender.getData().canUseUpgrade("bloomingGround")
                && bender.getData().canUseUpgrade("witheringTouch");
    }
}