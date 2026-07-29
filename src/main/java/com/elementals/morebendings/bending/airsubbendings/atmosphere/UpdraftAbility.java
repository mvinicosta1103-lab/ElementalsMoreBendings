package com.elementals.morebendings.bending.airsubbendings.atmosphere;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "updraft" — quarta habilidade raiz da árvore de Atmosphere (ver {@link
 * AtmosphereElement}). Instantânea, igual {@code VoidStepAbility}: de
 * mobilidade, não ofensiva.
 *
 * Cria uma coluna de baixa pressão sob o próprio caster, erguendo ele no
 * ar com um impulso vertical forte. Pra não punir o jogador com dano de
 * queda depois (que seria estranho pra uma ability de mobilidade),
 * concede Queda Lenta por alguns segundos junto -- tempo suficiente pro
 * jogador descer com controle ou pousar em algo.
 *
 * Cooldown-based (mesmo esquema de {@code VoidStepAbility}), com força
 * do impulso e cooldown escalando pelos upgrades de nível:
 *  - updraftHeightI / II → +0.3 de velocidade vertical cada
 *  - updraftCooldownI    → -30 ticks (1.5s) de cooldown
 */
public class UpdraftAbility implements Ability {

    private static final double BASE_VELOCITY = 1.0;
    private static final int SLOW_FALLING_DURATION_TICKS = 100; // 5s
    private static final int BASE_COOLDOWN_TICKS = 100; // 5s
    private static final int MIN_COOLDOWN_TICKS = 70;   // 3.5s, com o nível de cooldown
    private static final float CAST_CHI_COST = 6.0f;

    /** Cooldown por jogador. Fica em memória só -- não precisa persistir entre logins. */
    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
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

        double velocity = getVelocity(caster);
        caster.setDeltaMovement(caster.getDeltaMovement().x, velocity, caster.getDeltaMovement().z);
        caster.hasImpulse = true;
        caster.fallDistance = 0;
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_DURATION_TICKS, 0));

        level.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY(), caster.getZ(),
                20, 0.4, 0.1, 0.4, 0.02);
        level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 1.4f);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static double getVelocity(ServerPlayer player) {
        double velocity = BASE_VELOCITY;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.UPDRAFT_HEIGHT_I)) velocity += 0.3;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.UPDRAFT_HEIGHT_II)) velocity += 0.3;
        return velocity;
    }

    public static int getCooldownTicks(ServerPlayer player) {
        int cooldown = BASE_COOLDOWN_TICKS;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.UPDRAFT_COOLDOWN_I)) cooldown -= 30;
        return Math.max(cooldown, MIN_COOLDOWN_TICKS);
    }
}