package com.elementals.morebendings.bending.earthsubbendings.glass;

import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;

/**
 * Glass Bending — sub-bending de Earth, mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.sand.SandElement}:
 * Element de verdade, registrada no mod base, com sua própria árvore de
 * skills e sistema de bind.
 *
 * REGRA DE AQUISIÇÃO: diferente de Mud/Crystal/Sand (que exigem Earth
 * masterizado), Glass só exige que o jogador já tenha (não precisa ter
 * masterizado) Sand Bending — ver {@link #canAcquire}. Só pode ser
 * concedida via comando (/morebending grant), nunca automaticamente.
 */
public class GlassElement extends Element {

    public static final String NAME = "Glass";

    public GlassElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("glassShards", 0) // grátis -- ver GlassShardsAbility
        });
        addAbility(new GlassShardsAbility(), 0);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new GlassElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Sand Bending (basta ter obtido, não
     * precisa ter masterizado a árvore de Sand).
     */
    public static boolean canAcquire(Bender bender) {
        return SandElement.isSandBender(bender);
    }

    public static boolean isGlassBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this) && bender.getData().canUseUpgrade("glassShards");
    }
}