package com.example.elementalmorebendings.plant.abilities;

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
 * OvergrowthSpikesAbility ("overgrowthSpikes")
 * <p>
 * Ataque em área: faz espinhos de raiz brotarem do chão num raio em volta
 * do ponto mirado, empurrando pra cima e causando dano a todos os
 * inimigos pegos. Cada espinho é desenhado como uma coluna vertical de
 * partículas, distribuídos num anel — bem mais chamativo que os antigos
 * spawnRing "achatados" das habilidades originais.
 */
public class OvergrowthSpikesAbility implements Ability {

    private static final float BASE_COST = 26.0f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "overgrowthSpikes", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double radius = bender.plrData.canUseUpgrade("overgrowthSpikesRadiusI") ? 4.5 : 3.0;
        float damage = bender.plrData.canUseUpgrade("overgrowthSpikesPowerI") ? 7.0f : 4.5f;

        HitResult hit = player.pick(7.0, 0.0f, false);
        Vec3 center = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(7.0))
                : hit.getLocation();

        ServerLevel world = player.serverLevel();

        int spikeCount = 14;
        for (int i = 0; i < spikeCount; i++) {
            double angle = 2.0 * Math.PI * i / spikeCount;
            double dist = radius * (0.35 + 0.65 * ((i % 3) / 2.0));
            Vec3 base = center.add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            AbilitySupport.spawnSpikeColumn(world, base, 1.1 + (i % 3) * 0.2, AbilitySupport.LEAF_DUST, 8);
        }
        AbilitySupport.spawnRing(world, center, radius, ParticleTypes.HAPPY_VILLAGER, 30);

        List<LivingEntity> targets = AbilitySupport.entitiesAround(player, center, radius);
        for (LivingEntity target : targets) {
            target.hurt(world.damageSources().playerAttack(player), damage);
            target.push(0, 0.55, 0);
            target.hurtMarked = true;
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    target.getX(), target.getY(0.4), target.getZ(), 10, 0.3, 0.1, 0.3, 0.03);
        }

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
