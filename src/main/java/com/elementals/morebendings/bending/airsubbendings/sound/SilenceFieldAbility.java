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
 * "silenceField" — toggle. Enquanto ativo, abafa o som ao redor do bender
 * (ver {@link SilenceFieldManager}, que aplica Lentidão em quem entra na
 * zona a cada tick -- registrado em ElementalsMoreBendingsMod, mesmo
 * esquema de {@code MistCloudManager}/{@code CurseMinionManager}).
 *
 * Como esta é uma instância ÚNICA compartilhada entre todos os jogadores
 * (mesmo padrão de {@code EchoingVoiceAbility}), o estado "ligado/desligado"
 * é guardado por UUID aqui, e o {@link SilenceFieldManager} é quem
 * efetivamente lê esse estado tick a tick.
 *
 *  - silenceFieldRadiusI -> +2.0 de raio
 */
public class SilenceFieldAbility implements Ability {

    static final double BASE_RADIUS = 5.0;

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
        }

        level.playSound(null, caster.blockPosition(),
                nowActive ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 1.0f, 1.4f);

        bender.setCurrAbility(null); // toggle instantâneo -- o efeito contínuo vive no Manager
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
        Player player = bender.player;
        if (player != null) {
            ACTIVE.remove(player.getUUID());
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.contains(playerId);
    }

    public static void deactivate(UUID playerId) {
        ACTIVE.remove(playerId);
    }
}
