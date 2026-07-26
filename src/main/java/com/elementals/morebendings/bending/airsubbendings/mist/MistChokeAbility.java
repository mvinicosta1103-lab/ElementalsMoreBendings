package com.elementals.morebendings.bending.airsubbendings.mist;

import com.elementals.morebendings.bending.airsubbendings.common.SpecializationCycle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ramo de especialização "mistChoke" (ver {@link MistElement}) — o mais
 * agressivo dos três (pedido do usuário: "vai sufocar muito rápido e dar
 * dano de veneno"). Enquanto o alvo estiver dentro da névoa:
 *  - Envenenamento reaplicado a cada tick (ver {@link
 *    #POISON_DURATION_TICKS}), pra nunca deixar o efeito cair;
 *  - Dano direto extra a cada {@link #DAMAGE_INTERVAL_TICKS} (bem mais
 *    frequente que antes -- 0.5s em vez de 1s, daí o "muito rápido").
 * <p>
 * Chamado tick a tick por {@link MistCloudState} pra cada alvo dentro do
 * raio. Comprar não é mais exclusivo com Veil/Freeze (ver {@link
 * SpecializationCycle}); só faz efeito se for a especialização
 * ATUALMENTE ATIVA pro jogador.
 * <p>
 * Dano escala com:
 *  - mistChokeDamageI  → +0.5 de dano
 *  - mistChokeDamageII → +0.5 de dano
 */
public class MistChokeAbility {

    private static final int DAMAGE_INTERVAL_TICKS = 10; // a cada 0.5s
    private static final float BASE_DAMAGE = 1.0f;
    private static final int POISON_DURATION_TICKS = 30; // reforçado todo tick, então só precisa cobrir a folga entre chamadas
    private static final int POISON_AMPLIFIER = 1;

    public static void applyTick(ServerLevel level, ServerPlayer caster, LivingEntity target, int ticksElapsed) {
        if (!MistElement.hasUpgrade(caster, MistElement.MIST_CHOKE)
                || !SpecializationCycle.isMistActive(caster, MistElement.MIST_CHOKE)) {
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.POISON,
                POISON_DURATION_TICKS, POISON_AMPLIFIER, false, false, true));

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