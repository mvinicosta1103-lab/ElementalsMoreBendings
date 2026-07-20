package com.example.obsidianwake.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * EruptionAbility ("eruption")
 * Strikes the ground in front of the player, dealing damage, knockback and
 * fire to everything caught in the blast radius. Instant, offensive AoE.
 */
public class EruptionAbility implements Ability {

    private static final float BASE_COST = 30.0f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "eruption", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double radius = bender.plrData.canUseUpgrade("eruptionRadiusII") ? 5.0
                : (bender.plrData.canUseUpgrade("eruptionRadiusI") ? 4.0 : 3.0);
        float damage = bender.plrData.canUseUpgrade("eruptionPowerI") ? 8.0f : 5.0f;

        HitResult hit = player.pick(6.0, 0.0f, false);
        Vec3 center = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(6.0))
                : hit.getLocation();

        ServerLevel world = player.serverLevel();
        List<LivingEntity> targets = AbilitySupport.entitiesAround(player, center, radius);
        for (LivingEntity target : targets) {
            target.hurt(world.damageSources().magic(), damage);
            AbilitySupport.pushAway(center, target, 1.3, 0.6);
            target.igniteForSeconds(4.0f);
        }

        world.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.2, center.z,
                40, radius * 0.5, 0.3, radius * 0.5, 0.05);
        world.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.2, center.z,
                30, radius * 0.5, 0.2, radius * 0.5, 0.02);

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
