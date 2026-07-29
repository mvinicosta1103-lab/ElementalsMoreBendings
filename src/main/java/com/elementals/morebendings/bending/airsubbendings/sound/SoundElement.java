package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;
import net.minecraft.server.level.ServerPlayer;

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

    // ---- novas habilidades (nomes dos nós -- chave de save/lang/canUseUpgrade) ----
    public static final String SONIC_SHOCKWAVE = "sonicShockwave";
    public static final String SONIC_SHOCKWAVE_RADIUS_I = "sonicShockwaveRadiusI";
    public static final String SONIC_SHOCKWAVE_COOLDOWN_I = "sonicShockwaveCooldownI";
    public static final String SONIC_SHOCKWAVE_DISORIENT_I = "sonicShockwaveDisorientI";

    public static final String RESONANCE_PULSE = "resonancePulse";
    public static final String RESONANCE_PULSE_RADIUS_I = "resonancePulseRadiusI";

    public static final String SILENCE_FIELD = "silenceField";
    public static final String SILENCE_FIELD_RADIUS_I = "silenceFieldRadiusI";

    public static final String SONIC_STEP = "sonicStep";
    public static final String SONIC_STEP_DISTANCE_I = "sonicStepDistanceI";
    public static final String SONIC_STEP_COOLDOWN_I = "sonicStepCooldownI";

    public static final String DISCORDANT_SCREAM = "discordantScream";
    public static final String DISCORDANT_SCREAM_RANGE_I = "discordantScreamRangeI";
    public static final String DISCORDANT_SCREAM_DURATION_I = "discordantScreamDurationI";

    public static final String ECHO_SENSE = "echoSense";
    public static final String ECHO_SENSE_RADIUS_I = "echoSenseRadiusI";

    public SoundElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(ECHOING_VOICE, 0),  // grátis
                new Upgrade(RESONANT_PULSE, 0), // grátis
                new Upgrade(SONIC_SHOCKWAVE, new Upgrade[]{
                        new Upgrade(SONIC_SHOCKWAVE_RADIUS_I, 1),
                        new Upgrade(SONIC_SHOCKWAVE_COOLDOWN_I, 1),
                        new Upgrade(SONIC_SHOCKWAVE_DISORIENT_I, 1)
                }, 1),
                new Upgrade(RESONANCE_PULSE, new Upgrade[]{
                        new Upgrade(RESONANCE_PULSE_RADIUS_I, 1)
                }, 1),
                new Upgrade(SILENCE_FIELD, new Upgrade[]{
                        new Upgrade(SILENCE_FIELD_RADIUS_I, 1)
                }, 1),
                new Upgrade(SONIC_STEP, new Upgrade[]{
                        new Upgrade(SONIC_STEP_DISTANCE_I, 1),
                        new Upgrade(SONIC_STEP_COOLDOWN_I, 1)
                }, 1),
                new Upgrade(DISCORDANT_SCREAM, new Upgrade[]{
                        new Upgrade(DISCORDANT_SCREAM_RANGE_I, 1),
                        new Upgrade(DISCORDANT_SCREAM_DURATION_I, 1)
                }, 1),
                new Upgrade(ECHO_SENSE, new Upgrade[]{
                        new Upgrade(ECHO_SENSE_RADIUS_I, 1)
                }, 1)
        });
        addAbility(new EchoingVoiceAbility(), 0);
        addAbility(new ResonantPulseAbility(), 1);
        addAbility(new SonicShockwaveAbility(), 2);
        addAbility(new ResonancePulseAbility(), 3);
        addAbility(new SilenceFieldAbility(), 4);
        addAbility(new SonicStepAbility(), 5);
        addAbility(new DiscordantScreamAbility(), 6);
        addAbility(new EchoSenseAbility(), 7);
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

    /** Atalho pras abilities, que só têm o ServerPlayer em mãos. */
    public static boolean isSoundBender(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender != null && isSoundBender(bender);
    }

    /** Atalho pras abilities: o nó precisa ter sido comprado pelo jogador. */
    public static boolean hasUpgrade(ServerPlayer player, String upgradeName) {
        Bender bender = Bender.getBender(player);
        return bender != null && bender.getData().canUseUpgrade(upgradeName);
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(ECHOING_VOICE)
                && bender.getData().canUseUpgrade(RESONANT_PULSE)
                && bender.getData().canUseUpgrade(SONIC_SHOCKWAVE)
                && bender.getData().canUseUpgrade(RESONANCE_PULSE)
                && bender.getData().canUseUpgrade(SILENCE_FIELD)
                && bender.getData().canUseUpgrade(SONIC_STEP)
                && bender.getData().canUseUpgrade(DISCORDANT_SCREAM)
                && bender.getData().canUseUpgrade(ECHO_SENSE);
    }
}