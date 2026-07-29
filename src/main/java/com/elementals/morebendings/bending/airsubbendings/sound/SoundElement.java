package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;

/**
 * Sound Bending — sub-bending de Air, mesmo padrão de {@link
 * com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Air. Duas habilidades raiz, ambas grátis (preço 0), ambos filhos
 * diretos da raiz sintética — então, diferente de Gas/Mist/Plasma/
 * Combustion, NÃO precisa de autoUnlockRoot (não existe um único nó raiz
 * "escondendo" os outros; os dois já ficam disponíveis assim que o
 * jogador recebe o elemento).
 *
 *  - echoingVoice: grito sônico em cone à frente do jogador -- dano,
 *    empurrão e atordoamento (Náusea + Lentidão) em quem for pego, além
 *    de apagar fogo próximo. Ver {@link EchoingVoiceAbility}.
 *  - resonantPulse: pulso de eco esférico que atravessa blocos -- toda
 *    criatura viva pega fica Brilhando por alguns segundos (eco-
 *    localização). Ver {@link ResonantPulseAbility}.
 */
public class SoundElement extends Element {

    public static final String NAME = "Sound";

    public static final String ECHOING_VOICE = "echoingVoice";
    public static final String RESONANT_PULSE = "resonantPulse";

    public SoundElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(ECHOING_VOICE, 0),  // grátis
                new Upgrade(RESONANT_PULSE, 0)  // grátis
        });
        addAbility(new EchoingVoiceAbility(), 0);
        addAbility(new ResonantPulseAbility(), 1);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new SoundElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Air E já comprou todos os nós da
     * árvore de skills de Air (masterizou o elemento base) -- mesma regra
     * de Gas/Mist/Atmosphere.
     */
    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isSoundBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(ECHOING_VOICE)
                && bender.getData().canUseUpgrade(RESONANT_PULSE);
    }
}