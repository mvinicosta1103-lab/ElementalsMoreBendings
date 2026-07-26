package com.elementals.morebendings.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Applied by Pressure Point. Forces the target into a crawling-like state
 * (heavily reduced speed + a crouched/prone pose) and, once the target has
 * been "crushed" for CRUSH_THRESHOLD_TICKS, starts dealing periodic damage.
 * <p>
 * Amplifier 0 = just slowed/crawling. Amplifier increases the longer the
 * caller wants to ramp the effect (not required, PressureZoneEntity currently
 * always applies amplifier 0 and tracks overstay time itself).
 */
public class CrushedStatusEffect extends MobEffect {

    // How long (in ticks) a target can stay affected before it starts taking damage.
    public static final int CRUSH_THRESHOLD_TICKS = 200; // 10s, matches the zone's own lifetime
    private static final float DAMAGE_PER_TICK = 1.0f;
    private static final int DAMAGE_INTERVAL_TICKS = 10; // every 0.5s once past the threshold

    public CrushedStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x777777);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath("elementalsmorebendings", "crushed_speed"),
                -0.85, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH,
                ResourceLocation.fromNamespaceAndPath("elementalsmorebendings", "crushed_jump"),
                -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Force the crawl pose whenever the entity is grounded so it visibly
        // struggles under the pressure instead of just walking slowly.
        if (entity.onGround() && entity.getPose() != Pose.SWIMMING) {
            entity.setPose(Pose.SWIMMING);
        }

        int overstay = getOverstayTicks(entity);
        setOverstayTicks(entity, overstay + 1);

        if (overstay >= CRUSH_THRESHOLD_TICKS && overstay % DAMAGE_INTERVAL_TICKS == 0) {
            entity.hurt(entity.damageSources().generic(), DAMAGE_PER_TICK);
        }
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        setOverstayTicks(entity, 0);
    }

    @Override
    public void onMobRemoved(LivingEntity entity, int amplifier, LivingEntity.RemovalReason reason) {
        super.onMobRemoved(entity, amplifier, reason);
        clearOverstayTicks(entity);
    }

    // --- overstay bookkeeping -------------------------------------------------
    // Keyed off a persistent-data-style tag on the entity so this survives
    // amplifier changes/re-applications while the target stays inside the zone.

    private static final String OVERSTAY_KEY = "elementalsmorebendings_crushed_overstay";

    private int getOverstayTicks(LivingEntity entity) {
        return entity.getPersistentData().getInt(OVERSTAY_KEY);
    }

    private void setOverstayTicks(LivingEntity entity, int ticks) {
        entity.getPersistentData().putInt(OVERSTAY_KEY, ticks);
    }

    private void clearOverstayTicks(LivingEntity entity) {
        entity.getPersistentData().remove(OVERSTAY_KEY);
    }
}