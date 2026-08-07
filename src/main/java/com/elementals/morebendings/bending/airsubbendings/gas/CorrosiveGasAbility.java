package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CorrosiveGasAbility implements Ability {

    private static final int DURATION_TICKS = 200;
    private static final int COOLDOWN_TICKS = 200;
    private static final float CHI_COST = 8.0f;
    private static final double BASE_RADIUS = 4.0;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!GasElement.hasUpgrade(caster, GasElement.GAS_CORROSIVE)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        if (now - lastUse.getOrDefault(caster.getUUID(), -100000L) < COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        // Derrete durabilidade dos equipamentos do inimigo
        AABB area = caster.getBoundingBox().inflate(BASE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != caster && e.isAlive());

        for (LivingEntity target : targets) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.isArmor()) {
                    ItemStack armor = target.getItemBySlot(slot);
                    if (!armor.isEmpty() && armor.isDamageableItem()) {
                        armor.hurtAndBreak(4, target, slot);
                    }
                }
            }
        }

        // Nuvem corrosiva
        AreaEffectCloud cloud = new AreaEffectCloud(level, caster.getX(), caster.getY(), caster.getZ());
        cloud.setOwner(caster);
        cloud.setRadius((float) BASE_RADIUS);
        cloud.setDuration(DURATION_TICKS);
        cloud.setParticle(ParticleTypes.WARPED_SPORE);
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        cloud.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 2));

        level.addFreshEntity(cloud);
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}