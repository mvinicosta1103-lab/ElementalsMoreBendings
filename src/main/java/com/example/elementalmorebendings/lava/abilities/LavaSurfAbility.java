package com.example.elementalmorebendings.lava.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * LavaSurfAbility ("lavaSurf")
 * Toggle. While active and standing on/in lava, the player is immune to
 * fire/lava damage and gets a speed boost, as long as they keep having chi
 * to spend each tick.
 */
public class LavaSurfAbility implements Ability {

    private static final float TICK_COST = 0.4f;

    public void onCall(Bender bender, long deltaT) {
        if (!AbilitySupport.isUnlocked(bender, "lavaSurf")) {
            bender.setCurrAbility(null);
            return;
        }

        if (bender.isAbilityInBackground(this)) {
            bender.removeAbilityFromBackground(this);
        } else {
            bender.addBackgroundAbility(this, null);
        }
        bender.setCurrAbility(null);
    }

    public void onBackgroundTick(Bender bender, Object data) {
        Player player = bender.player;

        if (!AbilitySupport.isUnlocked(bender, "lavaSurf") || !AbilitySupport.spendChiPerTick(bender, TICK_COST)) {
            bender.removeAbilityFromBackground(this);
            return;
        }

        BlockPos below = player.blockPosition().below();
        boolean onLava = player.level().getBlockState(below).is(Blocks.LAVA)
                || player.level().getBlockState(player.blockPosition()).is(Blocks.LAVA);

        if (onLava) {
            int speedAmp = bender.plrData.canUseUpgrade("lavaSurfSpeedI") ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, speedAmp, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 0, false, false, true));
            player.fallDistance = 0;
        }
    }

    public void onRemove(Bender bender) {
        bender.removeAbilityFromBackground(this);
    }
}