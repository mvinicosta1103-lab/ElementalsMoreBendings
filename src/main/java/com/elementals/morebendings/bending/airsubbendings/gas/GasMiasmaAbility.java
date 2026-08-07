package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GasMiasmaAbility implements Ability {

    private static final int DURATION_TICKS = 240;
    private static final int COOLDOWN_TICKS = 180;
    private static final float CHI_COST = 7.0f;
    private static final double BASE_RADIUS = 4.5;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!GasElement.hasUpgrade(caster, GasElement.GAS_MIASMA)) {
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

        AreaEffectCloud cloud = new AreaEffectCloud(level, caster.getX(), caster.getY(), caster.getZ());
        cloud.setOwner(caster);
        cloud.setRadius((float) BASE_RADIUS);
        cloud.setDuration(DURATION_TICKS);
        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));

        level.addFreshEntity(cloud);
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}