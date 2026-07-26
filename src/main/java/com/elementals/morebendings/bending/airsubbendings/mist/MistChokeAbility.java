package com.elementals.morebendings.bending.airsubbendings.mist;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ramo de especialização "mistChoke" (ver {@link MistElement}) — dano
 * contínuo por permanência dentro da névoa, chamado tick a tick por
 * {@link MistCloudState} pra cada alvo dentro do raio.
 * <p>
 * Dano é aplicado a cada {@link #DAMAGE_INTERVAL_TICKS} usando a contagem
 * compartilhada da névoa (não por-alvo) — mesma simplificação usada em
 * {@code PressureZoneState}. Escala com:
 *  - mistChokeDamageI  → +0.5 de dano
 *  - mistChokeDamageII → +0.5 de dano
 */
public class MistChokeAbility {

    private static final int DAMAGE_INTERVAL_TICKS = 20; // a cada 1s
    private static final float BASE_DAMAGE = 1.0f;

    public static void applyTick(ServerLevel level, ServerPlayer caster, LivingEntity target, int ticksElapsed) {
        if (!MistElement.hasUpgrade(caster, MistElement.MIST_CHOKE)) {
            return;
        }
        if (ticksElapsed % DAMAGE_INTERVAL_TICKS != 0) {
            return;
        }
        float damage = getDamage(caster);
        target.hurt(level.damageSources().indirectMagic(caster, caster), damage);
    }

    public static float getDamage(ServerPlayer player) {
        float damage = BASE_DAMAGE;
        if (MistElement.hasUpgrade(player, MistElement.MIST_CHOKE_DAMAGE_I)) damage += 0.5f;
        if (MistElement.hasUpgrade(player, MistElement.MIST_CHOKE_DAMAGE_II)) damage += 0.5f;
        return damage;
    }

    private MistChokeAbility() {
    }
}
