package com.elementals.morebendings.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sangramento -- dano periódico ao longo da duração, independente de
 * "overstay" (diferente de {@link CrushedStatusEffect}, que só começa a
 * bater depois de um tempo parado dentro de uma zona). Usado pelo burst
 * defensivo do Avatar perto de morrer (ver {@code AvatarNearDeathGuardian}):
 * os arcos de Ar e Água aplicam isso em quem acertarem, simulando um corte
 * profundo/perfurante que continua sangrando depois do golpe inicial.
 * <p>
 * Amplifier aumenta o dano por tique (amplifier 0 = 1 coração a cada
 * {@link #DAMAGE_INTERVAL_TICKS}, amplifier 1 = 2 corações, etc.) -- nenhum
 * chamador atual usa amplifier > 0, mas fica disponível pra futuros usos.
 */
public class BleedingStatusEffect extends MobEffect {

    private static final float DAMAGE_PER_INTERVAL = 1.0f; // meio coração * 2 = 1 coração
    private static final int DAMAGE_INTERVAL_TICKS = 20; // a cada 1s

    public BleedingStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x9E1B1B);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        int elapsed = getElapsedTicks(entity) + 1;
        setElapsedTicks(entity, elapsed);

        if (elapsed % DAMAGE_INTERVAL_TICKS == 0) {
            float damage = DAMAGE_PER_INTERVAL * (amplifier + 1);
            entity.hurt(entity.damageSources().generic(), damage);
        }
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        setElapsedTicks(entity, 0);
    }

    @Override
    public void onMobRemoved(LivingEntity entity, int amplifier, LivingEntity.RemovalReason reason) {
        super.onMobRemoved(entity, amplifier, reason);
        clearElapsedTicks(entity);
    }

    // --- bookkeeping (mesmo esquema de CrushedStatusEffect#OVERSTAY_KEY) ---

    private static final String ELAPSED_KEY = "elementalsmorebendings_bleeding_elapsed";

    private int getElapsedTicks(LivingEntity entity) {
        return entity.getPersistentData().getInt(ELAPSED_KEY);
    }

    private void setElapsedTicks(LivingEntity entity, int ticks) {
        entity.getPersistentData().putInt(ELAPSED_KEY, ticks);
    }

    private void clearElapsedTicks(LivingEntity entity) {
        entity.getPersistentData().remove(ELAPSED_KEY);
    }
}
