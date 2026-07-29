package com.elementals.morebendings.bending.airsubbendings.atmosphere;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * "atmospherePressurePoint" — primeira habilidade raiz da árvore de
 * Atmosphere (ver {@link AtmosphereElement}). Cria um campo de pressão
 * fixo no ponto onde o caster está no momento do cast (não segue o
 * jogador). Quem entra no raio começa a rastejar (efeito aplicado tick a
 * tick pelo próprio {@link PressureZoneState}, sem precisar de um
 * MobEffect novo) e, se ficar tempo demais, passa a levar dano.
 *
 * Mesmo esquema de {@code MudTrapAbility}/{@code SandTornadoAbility}:
 * Manager+State dirigido por ServerTickEvent.Post, não uma Entity no
 * mundo — assim a zona continua existindo/contando o tempo mesmo que o
 * onTick da Ability pare de rodar por qualquer motivo.
 *
 * Raio e duração escalam com os upgrades de nível:
 *  - pressureRadiusI / II    → +0.75 bloco de raio cada
 *  - pressureDurationI / II  → +60 ticks (3s) de duração cada
 */
public class PressurePointAbility implements Ability {

    private static final double BASE_RADIUS = 4.0;
    private static final int BASE_DURATION_TICKS = 20 * 10; // 10s
    private static final float CAST_CHI_COST = 10.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (PressureZoneManager.hasActiveZone(caster)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        PressureZoneManager.startZone(level, caster, getRadius(caster), getDurationTicks(caster));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);

        bender.setCurrAbility(null); // instantâneo -- a zona vive sozinha via o Manager
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static double getRadius(ServerPlayer player) {
        double radius = BASE_RADIUS;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.PRESSURE_RADIUS_I)) radius += 0.75;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.PRESSURE_RADIUS_II)) radius += 0.75;
        return radius;
    }

    public static int getDurationTicks(ServerPlayer player) {
        int duration = BASE_DURATION_TICKS;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.PRESSURE_DURATION_I)) duration += 60;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.PRESSURE_DURATION_II)) duration += 60;
        return duration;
    }
}