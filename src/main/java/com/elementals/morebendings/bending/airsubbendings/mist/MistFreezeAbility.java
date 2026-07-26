package com.elementals.morebendings.bending.airsubbendings.mist;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ramo de especialização "mistFreeze" (ver {@link MistElement}) — nó
 * terminal (3 pontos, sem upgrades de melhoria, igual {@code
 * GasIgniteAbility}). Aplica Lentidão pesada por cima da Cegueira +
 * Escuridão do efeito-base, reforçada a cada tick enquanto o alvo
 * permanecer dentro da névoa.
 */
public class MistFreezeAbility {

    private static final int SLOWNESS_AMPLIFIER = 3; // bem lento, mas ainda anda
    private static final int EFFECT_REFRESH_TICKS = 10;

    public static void applyTick(ServerPlayer caster, LivingEntity target) {
        if (!MistElement.hasUpgrade(caster, MistElement.MIST_FREEZE)) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                EFFECT_REFRESH_TICKS, SLOWNESS_AMPLIFIER, false, false, true));
    }

    private MistFreezeAbility() {
    }
}