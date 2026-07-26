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
 * Duração escala com:
 *  - mistVeil (comprar o nó em si) → +100 ticks (5s)
 *  - mistVeilDurationI             → +100 ticks (5s)
 */
public class MistVeilAbility {

    private static final int BASE_DURATION_TICKS = 160; // 8s
    private static final int VEIL_BONUS_TICKS = 100;     // 5s
    private static final int VEIL_DURATION_I_BONUS_TICKS = 100; // 5s

    public static int getDuration(ServerPlayer player) {
        int duration = BASE_DURATION_TICKS;
        if (MistElement.hasUpgrade(player, MistElement.MIST_VEIL)) duration += VEIL_BONUS_TICKS;
        if (MistElement.hasUpgrade(player, MistElement.MIST_VEIL_DURATION_I)) duration += VEIL_DURATION_I_BONUS_TICKS;
        return duration;
    }

    private MistVeilAbility() {
    }
}
