package com.elementals.morebendings.bending.airsubbendings.temperature;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;

/**
 * Temperature Bending — sub-bending de Air, mesmo padrão de {@link
 * com.elementals.morebendings.bending.airsubbendings.sound.SoundElement}
 * e {@link com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Air. Duas habilidades raiz, ambas grátis (preço 0), ambas filhas
 * diretas da raiz sintética -- não precisa de autoUnlockRoot (mesmo
 * motivo do Sound: não existe um único nó raiz "escondendo" os outros).
 *
 *  - totalZero: rajada de frio absoluto ao redor do jogador -- dano de
 *    congelamento, Lentidão pesada, congela o jogador (visual de gelo),
 *    apaga fogo e transforma água próxima em gelo. Ver {@link
 *    TotalZeroAbility}.
 *  - scorchingWave: onda de calor extremo ao redor do jogador -- incendeia
 *    quem for pego, derrete gelo/neve próximos de volta em água/ar e dá
 *    Resistência a Fogo pro próprio caster por um tempo. Ver {@link
 *    ScorchingWaveAbility}.
 */
public class TemperatureElement extends Element {

    public static final String NAME = "Temperature";

    public static final String TOTAL_ZERO = "totalZero";
    public static final String SCORCHING_WAVE = "scorchingWave";

    public TemperatureElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(TOTAL_ZERO, 0),      // grátis
                new Upgrade(SCORCHING_WAVE, 0)   // grátis
        });
        addAbility(new TotalZeroAbility(), 0);
        addAbility(new ScorchingWaveAbility(), 1);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new TemperatureElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Air E já comprou todos os nós da
     * árvore de skills de Air (masterizou o elemento base) -- mesma regra
     * de Gas/Mist/Atmosphere/Sound.
     */
    public static boolean canAcquire(Bender bender) {
        Element air = AirElement.get();
        return bender.hasElement(air) && air.isSkillTreeComplete(bender);
    }

    public static boolean isTemperatureBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(TOTAL_ZERO)
                && bender.getData().canUseUpgrade(SCORCHING_WAVE);
    }
}