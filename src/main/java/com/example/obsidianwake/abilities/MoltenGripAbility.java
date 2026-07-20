package com.example.obsidianwake.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;

/**
 * MoltenGripAbility ("moltenGrip")
 * Grabs the nearest entity in front of the player with a tendril of magma
 * and yanks it toward the caster, dealing minor damage and a brief burn.
 */
public class MoltenGripAbility implements Ability {

    private static final float BASE_COST = 12.0f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "moltenGrip", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double range = bender.plrData.canUseUpgrade("moltenGripRangeII") ? 12.0
                : (bender.plrData.canUseUpgrade("moltenGripRangeI") ? 9.0 : 6.0);
        float damage = bender.plrData.canUseUpgrade("moltenGripPowerI") ? 4.0f : 2.0f;

        List<LivingEntity> candidates = AbilitySupport.entitiesInFront(player, range, 1.5);
        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (target != null) {
            AbilitySupport.pullToward(player.position(), target, 1.6, 0.3);
            target.hurt(player.serverLevel().damageSources().magic(), damage);
            target.igniteForSeconds(2.0f);
        }

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
