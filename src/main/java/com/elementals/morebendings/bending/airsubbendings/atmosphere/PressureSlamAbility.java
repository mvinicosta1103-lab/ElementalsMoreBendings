package com.elementals.morebendings.bending.airsubbendings.atmosphere;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "pressureSlam" — terceira habilidade raiz da árvore de Atmosphere (ver
 * {@link AtmosphereElement}). Instantânea, igual {@code CurseMinionAbility}:
 * OBRIGATÓRIO liberar {@code currAbility} no final.
 *
 * Diferente de {@code atmospherePressurePoint} (área, quem entra é
 * afetado aos poucos), esta é ofensiva e concentrada num único alvo:
 * raycast na direção olhada, e quem for atingido recebe um golpe de
 * pressão condensada -- dano direto + arremesso pra baixo (o "peso" da
 * pressão colapsando sobre o alvo) + Fraqueza breve, simulando o corpo
 * ficando momentaneamente esmagado.
 *
 * Cooldown-based (mesmo esquema de {@code GasCloudAbility}), com dano e
 * cooldown escalando pelos upgrades de nível:
 *  - pressureSlamDamageI / II → +1.5 de dano cada
 *  - pressureSlamCooldownI    → -20 ticks (1s) de cooldown
 */
public class PressureSlamAbility implements Ability {

    private static final double RANGE = 6.0;
    private static final float BASE_DAMAGE = 4.0f;
    private static final int BASE_COOLDOWN_TICKS = 60; // 3s
    private static final int MIN_COOLDOWN_TICKS = 40;  // 2s, com o nível de cooldown
    private static final float CAST_CHI_COST = 8.0f;

    private static final double DOWNWARD_KNOCKBACK = 0.6;
    private static final double OUTWARD_KNOCKBACK = 0.3;
    private static final int WEAKNESS_DURATION_TICKS = 60; // 3s
    private static final int WEAKNESS_AMPLIFIER = 1;

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

        EntityHitResult hit = SapsUtils.raycastEntity(player, RANGE,
                entity -> entity instanceof LivingEntity living && living != player && living.isAlive());

        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            caster.displayClientMessage(Component.literal("Nenhum alvo encontrado."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        target.hurt(level.damageSources().generic(), getDamage(caster));
        target.push(0, -DOWNWARD_KNOCKBACK, 0);
        target.push((level.random.nextDouble() - 0.5) * OUTWARD_KNOCKBACK, 0,
                (level.random.nextDouble() - 0.5) * OUTWARD_KNOCKBACK);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_DURATION_TICKS, WEAKNESS_AMPLIFIER));

        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0f, 0.5f);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static float getDamage(ServerPlayer player) {
        float damage = BASE_DAMAGE;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.PRESSURE_SLAM_DAMAGE_I)) damage += 1.5f;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.PRESSURE_SLAM_DAMAGE_II)) damage += 1.5f;
        return damage;
    }

    public static int getCooldownTicks(ServerPlayer player) {
        int cooldown = BASE_COOLDOWN_TICKS;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.PRESSURE_SLAM_COOLDOWN_I)) cooldown -= 20;
        return Math.max(cooldown, MIN_COOLDOWN_TICKS);
    }
}