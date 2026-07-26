package com.elementals.morebendings.bending.airsubbendings.mist;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "mistCloud" — nó raiz da árvore do {@link MistElement}. Diferente de
 * {@code GasCloudAbility} (burst instantâneo), planta uma névoa FIXA no
 * ponto onde o caster está no momento do cast (não segue o jogador,
 * mesmo esquema de {@code PressurePointAbility}) que fica ativa por um
 * tempo. Quem entra no raio recebe Cegueira + Escuridão reaplicadas tick
 * a tick (ver {@link MistCloudState}) — o próprio caster NUNCA é afetado,
 * mesma regra do Gas. Cegueira já reduz alcance de mira/detecção de mobs
 * no vanilla, cobrindo esse pedido sem precisar mexer em IA de mob.
 *
 * Raio e cooldown escalam com os upgrades de crescimento, mesma fórmula
 * do Gas:
 *  - mistCloudSizeI / II → +0.75 bloco de raio cada
 *  - mistVentI / II      → -20 ticks (1s) de cooldown cada
 *
 * A duração da névoa em si (quanto tempo o {@link MistCloudState} fica de
 * pé) é calculada por {@link MistVeilAbility#getDuration}, já que só a
 * especialização "mistVeil" mexe nisso.
 */
public class HeavyFogAbility implements Ability {

    private static final double BASE_RADIUS = 4.0;
    private static final int BASE_COOLDOWN_TICKS = 140; // 7s
    private static final int MIN_COOLDOWN_TICKS = 100;  // 5s, com os 2 níveis de vent
    private static final float CAST_CHI_COST = 6.0f;    // ajustar conforme balanceamento

    /** Cooldown por jogador. Fica em memória só — não precisa persistir entre logins. */
    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        // Só uma névoa por vez por caster -- evita empilhar zonas do mesmo
        // jogador enquanto uma anterior ainda está de pé.
        if (MistCloudManager.hasActiveZone(caster)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -100000L);
        int cooldown = getCooldownTicks(caster);
        if (now - last < cooldown) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        double radius = getRadius(caster);
        int duration = MistVeilAbility.getDuration(caster);

        level.sendParticles(ParticleTypes.CLOUD,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                (int) (30 * (radius / BASE_RADIUS)), radius * 0.5, 0.6, radius * 0.5, 0.01);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.5f);

        MistCloudManager.startZone(level, caster, radius, duration);

        bender.setCurrAbility(null); // instantâneo -- a névoa vive sozinha via o Manager
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static double getRadius(ServerPlayer player) {
        double radius = BASE_RADIUS;
        if (MistElement.hasUpgrade(player, MistElement.MIST_CLOUD_SIZE_I)) radius += 0.75;
        if (MistElement.hasUpgrade(player, MistElement.MIST_CLOUD_SIZE_II)) radius += 0.75;
        return radius;
    }

    public static int getCooldownTicks(ServerPlayer player) {
        int cooldown = BASE_COOLDOWN_TICKS;
        if (MistElement.hasUpgrade(player, MistElement.MIST_VENT_I)) cooldown -= 20;
        if (MistElement.hasUpgrade(player, MistElement.MIST_VENT_II)) cooldown -= 20;
        return Math.max(cooldown, MIN_COOLDOWN_TICKS);
    }
}
