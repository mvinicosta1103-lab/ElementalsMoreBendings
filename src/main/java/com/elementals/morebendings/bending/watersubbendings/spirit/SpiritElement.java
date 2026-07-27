package com.elementals.morebendings.bending.watersubbendings.spirit;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.water.WaterElement;

/**
 * Spirit Bending — sub-bending de Water, mesmo padrão de {@link
 * com.elementals.morebendings.bending.watersubbendings.plant.PlantElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Water. Duas habilidades raiz, ambas grátis (preço 0):
 *
 *  - purifyingWater: purifica mobs pegos em água próxima -- mortos-vivos
 *    "menores" (Skeleton, Zombie bebê, Husk, Blaze) se dissolvem em
 *    partículas espirituais; formas corrompidas (Witch, Zombie Villager
 *    bebê, Pillager, Vindicator) voltam a ser Villager normal; Iron Golem
 *    danificado é totalmente consertado; Wither Skeleton vira um Snow
 *    Golem. Ver {@link PurifyingWaterAbility} / {@link PurifyingWaterManager}.
 *  - curseMinion: amaldiçoa uma criatura para atacar o caster e qualquer
 *    outro mob/player por perto, alternando alvo periodicamente. Ver
 *    {@link CurseMinionAbility} / {@link CurseMinionManager}.
 */
public class SpiritElement extends Element {

    public static final String NAME = "Spirit";

    public SpiritElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("purifyingWater", 0), // grátis
                new Upgrade("curseMinion", 0)      // grátis
        });
        addAbility(new PurifyingWaterAbility(), 0);
        addAbility(new CurseMinionAbility(), 1);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new SpiritElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Water E já comprou todos os nós da
     * árvore de skills de Water (masterizou o elemento base) -- mesma
     * regra de Plant/Mud/Crystal, só que Spirit também é gated atrás de
     * Water (é uma sub-bending de Water, igual Plant).
     */
    public static boolean canAcquire(Bender bender) {
        Element water = WaterElement.get();
        return bender.hasElement(water) && water.isSkillTreeComplete(bender);
    }

    public static boolean isSpiritBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade("purifyingWater")
                && bender.getData().canUseUpgrade("curseMinion");
    }
}