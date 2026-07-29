package com.elementals.morebendings.bending.airsubbendings.sound;

/**
 * SoundTreeHook — TODO: adapte cada chamada abaixo pro seu registry/tree
 * real. A estrutura segue o mesmo racional documentado no addon Obsidian
 * Wake: localizar o elemento "Air", achar a raiz do subbending "sound"
 * (já existente, conforme sua screenshot de package), e encaixar os novos
 * nós de upgrade como filhos dela.
 *
 * Se o seu Element/UpgradeTree usa outra API (ex.: builder pattern,
 * registro via JSON de datapack, etc.), essa é a única classe que precisa
 * mudar — as 6 classes de Ability em si não dependem disso.
 */
public final class SoundTreeHook {

    private SoundTreeHook() {}

    public static void extendSoundTree() {
        // Object airElement = ElementRegistry.find("Air");
        // if (airElement == null) {
        //     LOGGER.warn("[SoundExpansion] Elemento Air não encontrado — addon não será aplicado.");
        //     return;
        // }
        //
        // Object soundRoot = airElement.getSubbendingRoot("sound");
        // if (soundRoot == null) {
        //     LOGGER.warn("[SoundExpansion] Subbending 'sound' não encontrado — addon não será aplicado.");
        //     return;
        // }

        registerAbilityNode("sonicShockwave", "sonicShockwaveRadiusI", "sonicShockwaveCooldownI", "sonicShockwaveDisorientI");
        registerAbilityNode("resonancePulse", "resonancePulseRadiusI");
        registerAbilityNode("silenceField", "silenceFieldRadiusI");
        registerAbilityNode("sonicStep", "sonicStepDistanceI", "sonicStepCooldownI");
        registerAbilityNode("discordantScream", "discordantScreamRangeI", "discordantScreamDurationI");
        registerAbilityNode("echoSense", "echoSenseRadiusI");

        // LOGGER.info("[SoundExpansion] 6 novas abilities registradas no subbending Sound.");
    }

    private static void registerAbilityNode(String abilityId, String... upgradeIds) {
        // soundRoot.addChild(abilityId);
        // for (String upgradeId : upgradeIds) {
        //     soundRoot.getChild(abilityId).addUpgrade(upgradeId);
        // }
    }
}