package com.elementals.morebendings.bending.airsubbendings.voiding;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;

/**
 * Void Bending — sub-bending de Air, mesmo padrão de {@link
 * com.elementals.morebendings.bending.airsubbendings.temperature.TemperatureElement}
 * e {@link com.elementals.morebendings.bending.airsubbendings.sound.SoundElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Air. Duas habilidades raiz, ambas grátis (preço 0), ambas filhas
 * diretas da raiz sintética -- não precisa de autoUnlockRoot.
 *
 *  - voidBall: mira num ponto e cria uma implosão de vácuo ali -- puxa
 *    tudo que estiver perto pro centro e esmaga quem chegar perto o
 *    suficiente. Ver {@link VoidBallAbility}.
 *  - voidStep: "passo pelo vazio" -- teleporte curto na direção olhada,
 *    parando antes de bater em bloco sólido. Ver {@link VoidStepAbility}.
 */
public class VoidElement extends Element {

    public static final String NAME = "Void";

    public static final String VOID_BALL = "voidBall";
    public static final String VOID_STEP = "voidStep";

    public VoidElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(VOID_BALL, 0), // grátis
                new Upgrade(VOID_STEP, 0)  // grátis
        });
        addAbility(new VoidBallAbility(), 0);
        addAbility(new VoidStepAbility(), 1);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new VoidElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Air E já comprou todos os nós da
     * árvore de skills de Air (masterizou o elemento base) -- mesma regra
     * de Gas/Mist/Atmosphere/Sound/Temperature.
     */
    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isVoidBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(VOID_BALL)
                && bender.getData().canUseUpgrade(VOID_STEP);
    }
}