package com.elementals.morebendings.bending.earthsubbendings.sand;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;

/**
 * Sand Bending — sub-bending de Earth, registrada como {@link Element} de
 * verdade no mod base (mesmo padrão que {@link
 * com.elementals.morebendings.bending.earthsubbendings.mud.MudElement} e
 * {@link com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement}
 * já usam): entra no ciclo de elementos e usa a mesma árvore de skills /
 * sistema de bind que qualquer elemento nativo.
 *
 * REGRA DE AQUISIÇÃO: mesma de Mud/Crystal — só pode ser concedida a quem
 * já tem Earth E já masterizou a árvore de Earth inteira (ver
 * {@link #canAcquire}).
 */
public class SandElement extends Element {

    public static final String NAME = "Sand";

    public SandElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("sandTornado", 0) // grátis -- ver SandTornadoAbility
        });
        addAbility(new SandTornadoAbility(), 0);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new SandElement();
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

    public static boolean isSandBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this) && bender.getData().canUseUpgrade("sandTornado");
    }
}