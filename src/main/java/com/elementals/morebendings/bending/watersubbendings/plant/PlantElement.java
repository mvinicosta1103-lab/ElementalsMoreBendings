package com.elementals.morebendings.bending.watersubbendings.plant;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.water.WaterElement;

/**
 * Plant Bending — sub-bending de Water, mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.mud.MudElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Water. Duas habilidades raiz, ambas grátis (preço 0), mesmo esquema
 * de Mud (mudSurge + mudTrap):
 *
 *  - vineWhip: chicote de vinhas -- puxa a vítima até o caster (com dano)
 *    se acertar uma entidade, ou puxa o próprio caster até o ponto mirado
 *    (grappling hook) se acertar só um bloco. Ver {@link PlantVineWhipAbility}.
 *  - vineWall: levanta uma parede temporária de folhagem na frente do
 *    caster. Ver {@link PlantVineWallAbility} / {@link PlantVineWallManager}.
 *  - thornVolley: saraivada de espinhos envenenados na direção da mira.
 *    Ver {@link PlantThornVolleyAbility}.
 *  - vineGrasp: agarra uma criatura viva (inclusive jogadores) com vinhas e
 *    dá controle telecinético dela ao caster -- puxar/controlar/mover
 *    (padrão), levantar (olhando pra cima) ou esmagar (agachado). Ver
 *    {@link PlantVineGraspAbility}.
 *  - rootSnare: raízes em área que imobilizam todo mundo por perto. Ver
 *    {@link PlantRootSnareAbility}.
 *
 * Antes disso, Plant vivia só no sistema antigo paralelo
 * ({@code PlayerSubbendingData} / {@code SubbendingType.PLANT}), sem
 * nenhuma habilidade de verdade por trás -- {@code isPlantBender} sempre
 * retornava {@code true} incondicionalmente. Esta classe substitui esse
 * stub.
 */
public class PlantElement extends Element {

    public static final String NAME = "Plant";

    public PlantElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("vineWhip", 0),
                new Upgrade("vineWall", 0),
                new Upgrade("thornVolley", 0),
                new Upgrade("vineGrasp", 0),
                new Upgrade("rootSnare", 0)
        });
        addAbility(new PlantVineWhipAbility(), 0);
        addAbility(new PlantVineWallAbility(), 1);
        addAbility(new PlantThornVolleyAbility(), 2);
        addAbility(new PlantVineGraspAbility(), 3);
        addAbility(new PlantRootSnareAbility(), 4);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new PlantElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Water E já comprou todos os nós da
     * árvore de skills de Water (masterizou o elemento base) — mesma regra
     * de Mud/Crystal/Atmosphere, só que gated atrás de Water em vez de Earth.
     */
    public static boolean canAcquire(Bender bender) {
        Element water = WaterElement.get();
        return bender.hasElement(water) && water.isSkillTreeComplete(bender);
    }

    public static boolean isPlantBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade("vineWhip")
                && bender.getData().canUseUpgrade("vineWall")
                && bender.getData().canUseUpgrade("thornVolley")
                && bender.getData().canUseUpgrade("vineGrasp")
                && bender.getData().canUseUpgrade("rootSnare");
    }
}