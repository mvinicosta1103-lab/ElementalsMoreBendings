package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;

/**
 * Mud Bending — sub-bending de Earth, registrada como um {@link Element}
 * de verdade no mod base (não um sistema paralelo). Isso é o que faz ela
 * aparecer no ciclo de elementos (tecla de trocar bending) e usar a mesma
 * árvore de skills / sistema de bind que Air, Water, Earth, Fire, etc.
 *
 * REGRA DE AQUISIÇÃO: só pode ser concedida a quem já tem Earth E já
 * masterizou a árvore de Earth inteira (ver {@link #canAcquire}). Isso é
 * checado no momento da concessão (ver MoreBendingCommand) — o Element em
 * si não impede o jogador de "ter" o elemento por fora, só documentamos a
 * regra aqui pra ficar num lugar só.
 */
public class MudElement extends Element {

    public static final String NAME = "Mud";

    public MudElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("mudSurge", 0) // habilidade única, grátis, por enquanto
        });
        addAbility(new MudSurgeAbility(), 0);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new MudElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Earth E já comprou todos os nós da
     * árvore de skills de Earth (masterizou o elemento base).
     */
    public static boolean canAcquire(Bender bender) {
        Element earth = EarthElement.get();
        return bender.hasElement(earth) && earth.isSkillTreeComplete(bender);
    }

    public static boolean isMudBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this) && bender.getData().canUseUpgrade("mudSurge");
    }
}