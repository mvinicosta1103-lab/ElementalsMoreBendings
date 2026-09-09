package com.elementals.morebendings.bending.watersubbendings.healing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as maldições de {@code witheringTouch} ativas no servidor
 * -- uma por vítima (chaveada pelo UUID dela). Dirigido por {@link
 * ServerTickEvent.Post}, registrado em {@link
 * com.elementals.morebendings.ElementalsMoreBendingsMod}, mesmo esquema
 * de {@code CurseMinionManager}/{@code AvatarBendingFreezeManager}.
 * <p>
 * Diferente de um debuff normal com timer: aqui NÃO existe duração --
 * {@link #onServerTick} reaplica Veneno + Fome na vítima a cada
 * {@link #REFRESH_INTERVAL_TICKS}, indefinidamente, e a cada {@link
 * #DAMAGE_INTERVAL_TICKS} desfere um dano direto (o Veneno sozinho nunca
 * mata, ele para em 1 de vida -- esse dano direto é o que de fato leva a
 * maldição até a morte, como pedido). A ÚNICA forma de reverter é tocar a
 * vítima de novo com a mesma ability (ver {@link
 * HealingTouchAbility}/{@link WitheringTouchAbility} -- é ele quem chama
 * {@link #isAfflicted}/{@link #cure} antes de decidir se amaldiçoa ou
 * cura), ou a vítima morrer, ou desconectar.
 */
public final class WitheringTouchManager {

    private static final int REFRESH_INTERVAL_TICKS = 30; // 1.5s -- bem menor que a duração aplicada, nunca "some" sozinho
    private static final int EFFECT_DURATION_TICKS = 80; // 4s -- só precisa sobreviver até o próximo refresh
    private static final int POISON_AMPLIFIER = 0; // Veneno I
    private static final int HUNGER_AMPLIFIER = 0; // Fome I

    private static final int DAMAGE_INTERVAL_TICKS = 40; // 2s
    private static final float DAMAGE_PER_TICK = 1.0f; // meio coração a cada 2s -- "aos poucos", não instantâneo

    private static final Map<UUID, Affliction> ACTIVE = new HashMap<>();

    private WitheringTouchManager() {
    }

    public static boolean isAfflicted(LivingEntity victim) {
        return ACTIVE.containsKey(victim.getUUID());
    }

    /** Começa a maldição -- primeiro toque num alvo saudável. */
    public static void afflict(ServerLevel level, LivingEntity victim) {
        ACTIVE.put(victim.getUUID(), new Affliction(level));
        applyEffects(victim);
    }

    /** Reverte a maldição -- segundo toque na mesma vítima. */
    public static void cure(LivingEntity victim) {
        ACTIVE.remove(victim.getUUID());
        victim.removeEffect(MobEffects.POISON);
        victim.removeEffect(MobEffects.HUNGER);
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Affliction>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Affliction> entry = it.next();
            Affliction affliction = entry.getValue();

            LivingEntity victim = findEntity(affliction.level, entry.getKey());
            if (victim == null || !victim.isAlive()) {
                it.remove(); // morreu ou saiu do mundo carregado -- maldição não persiste depois disso
                continue;
            }

            affliction.ticksUntilRefresh--;
            if (affliction.ticksUntilRefresh <= 0) {
                affliction.ticksUntilRefresh = REFRESH_INTERVAL_TICKS;
                applyEffects(victim);
            }

            affliction.ticksUntilDamage--;
            if (affliction.ticksUntilDamage <= 0) {
                affliction.ticksUntilDamage = DAMAGE_INTERVAL_TICKS;
                victim.hurt(affliction.level.damageSources().magic(), DAMAGE_PER_TICK);
            }
        }
    }

    private static void applyEffects(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(MobEffects.POISON, EFFECT_DURATION_TICKS, POISON_AMPLIFIER));
        victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, EFFECT_DURATION_TICKS, HUNGER_AMPLIFIER));
    }

    private static LivingEntity findEntity(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof LivingEntity living ? living : null;
    }

    private static final class Affliction {
        final ServerLevel level;
        int ticksUntilRefresh = REFRESH_INTERVAL_TICKS;
        int ticksUntilDamage = DAMAGE_INTERVAL_TICKS;

        Affliction(ServerLevel level) {
            this.level = level;
        }
    }
}