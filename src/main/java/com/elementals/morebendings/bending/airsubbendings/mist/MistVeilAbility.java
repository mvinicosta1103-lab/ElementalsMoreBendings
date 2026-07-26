package com.elementals.morebendings.bending.airsubbendings.mist;

import net.minecraft.server.level.ServerPlayer;

/**
 * Ramo de especialização "mistVeil" (ver {@link MistElement}) — não
 * aplica nenhum efeito em entidades; só estende a duração da névoa em
 * si. Por isso, ao contrário de {@link MistChokeAbility}/{@link
 * MistFreezeAbility}, não é chamada tick a tick por {@link
 * MistCloudState} — {@link HeavyFogAbility#onCall} já resolve a duração
 * final uma única vez, no momento do cast, via {@link #getDuration}.
 * <p>
 * Duração longa de propósito (a névoa não é pra ser um burst curto tipo
 * Gas Cloud), mas nunca infinita -- o teto de {@link #getDuration} é o
 * limite absoluto. Quem quiser encerrar antes disso pode: o próprio
 * caster pode clicar com o botão direito na névoa pra dissipá-la mais
 * rápido (ver {@link MistCloudState#accelerate()}/{@link
 * MistFogEntity#interact}).
 * <p>
 * Duração escala com:
 *  - base                → 600 ticks (30s)
 *  - mistVeil (o nó em si)          → +200 ticks (10s)
 *  - mistVeilDurationI              → +200 ticks (10s)
 * Máximo possível: 1000 ticks (50s).
 */
public class MistVeilAbility {

    private static final int BASE_DURATION_TICKS = 600; // 30s
    private static final int VEIL_BONUS_TICKS = 200;     // 10s
    private static final int VEIL_DURATION_I_BONUS_TICKS = 200; // 10s

    public static int getDuration(ServerPlayer player) {
        int duration = BASE_DURATION_TICKS;
        if (MistElement.hasUpgrade(player, MistElement.MIST_VEIL)) duration += VEIL_BONUS_TICKS;
        if (MistElement.hasUpgrade(player, MistElement.MIST_VEIL_DURATION_I)) duration += VEIL_DURATION_I_BONUS_TICKS;
        return duration;
    }

    private MistVeilAbility() {
    }
}