package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * "echoSense" — toggle passivo. Enquanto ativo, revela periodicamente a
 * posição de entidades vivas próximas (mesmo através de paredes) via
 * partículas visíveis só para o bender -- ver {@link EchoSenseManager},
 * que dirige o pulso periódico tick a tick (registrado em
 * ElementalsMoreBendingsMod, mesmo esquema de {@code SilenceFieldManager}).
 * Não causa dano nem efeitos negativos.
 *
 *  - echoSenseRadiusI -> +6.0 de raio
 */
public class EchoSenseAbility implements Ability {

    static final double BASE_RADIUS = 12.0;
    static final int PULSE_INTERVAL_TICKS = 80; // 4s

    private static final Set<UUID> ACTIVE = new HashSet<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        UUID id = caster.getUUID();
        boolean nowActive;
        if (ACTIVE.contains(id)) {
            ACTIVE.remove(id);
            nowActive = false;
        } else {
            ACTIVE.add(id);
            nowActive = true;
            EchoSenseManager.resetTimer(id);
        }

        level.playSound(null, caster.blockPosition(),
                nowActive ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_BLOCK_BREAK,
                SoundSource.PLAYERS, 0.8f, 1.4f);

        bender.setCurrAbility(null); // toggle instantâneo -- o pulso periódico vive no Manager
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
        Player player = bender.player;
        if (player != null) {
            deactivate(player.getUUID());
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.contains(playerId);
    }

    public static void deactivate(UUID playerId) {
        ACTIVE.remove(playerId);
        EchoSenseManager.clearTimer(playerId);
    }
}
