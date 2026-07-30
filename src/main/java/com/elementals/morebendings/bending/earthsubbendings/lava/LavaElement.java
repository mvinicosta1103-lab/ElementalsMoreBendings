package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;

/**
 * Lava Bending — sub-bending de Earth, registrada como um {@link Element}
 * de verdade no mod base, mesmo padrão que {@link
 * com.elementals.morebendings.bending.earthsubbendings.mud.MudElement},
 * {@link com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement}
 * e {@link com.elementals.morebendings.bending.earthsubbendings.sand.SandElement}
 * já usam: entra no ciclo de elementos e usa a mesma árvore de skills /
 * sistema de bind que qualquer elemento nativo.
 *
 * REGRA DE AQUISIÇÃO: mesma de Mud/Crystal/Sand/Petrification — só pode ser
 * concedida a quem já tem Earth E já masterizou a árvore de Earth inteira
 * (ver {@link #canAcquire}).
 *
 * Três habilidades raiz, todas grátis (preço 0), mesmo esquema de Mud/
 * Petrification/Bone (mais de um nó raiz sem custo):
 *  - lavaPool: controle de área -- derrete o chão numa poça de lava de
 *    verdade que esfria sozinha. Ver {@link LavaPoolAbility}.
 *  - lavaJet: ofensiva direta -- jato de lava em linha reta, dano +
 *    ignição + empurrão. Ver {@link LavaJetAbility}.
 *  - magmaSpike: AoE de curto alcance -- erupção pontual de espinhos de
 *    magma que desmancham pouco depois. Ver {@link MagmaSpikeAbility}.
 */
public class LavaElement extends Element {

    public static final String NAME = "Lava";

    public static final String LAVA_POOL = "lavaPool";
    public static final String LAVA_JET = "lavaJet";
    public static final String MAGMA_SPIKE = "magmaSpike";

    public LavaElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(LAVA_POOL, 0),   // grátis -- ver LavaPoolAbility
                new Upgrade(LAVA_JET, 0),    // grátis -- ver LavaJetAbility
                new Upgrade(MAGMA_SPIKE, 0)  // grátis -- ver MagmaSpikeAbility
        });
        addAbility(new LavaPoolAbility(), 0);
        addAbility(new LavaJetAbility(), 1);
        addAbility(new MagmaSpikeAbility(), 2);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new LavaElement();
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

    public static boolean isLavaBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(LAVA_POOL)
                && bender.getData().canUseUpgrade(LAVA_JET)
                && bender.getData().canUseUpgrade(MAGMA_SPIKE);
    }
}