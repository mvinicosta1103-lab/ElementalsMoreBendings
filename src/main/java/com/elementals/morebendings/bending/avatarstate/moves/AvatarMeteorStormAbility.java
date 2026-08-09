package com.elementals.morebendings.bending.avatarstate.moves;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * "avatarMeteorStorm" — segunda ability de Fogo do {@code AvatarElement}.
 * Chove fogo numa área grande ao redor do ponto mirado -- vários impactos
 * simultâneos, cada um com seu próprio raio de dano/ignição. Muito mais
 * destrutivo em área que qualquer FireBlast/CombustionBlast normal.
 */
public class AvatarMeteorStormAbility implements Ability {

    private static final double RANGE = 20.0;
    private static final double AREA_RADIUS = 7.0;
    private static final int IMPACT_COUNT = 6;
    private static final double IMPACT_RADIUS = 2.5;
    private static final float DAMAGE_PER_IMPACT = 5.0f;
    private static final int FIRE_TICKS = 100; // 5s
    private static final float CHI_COST = 36.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        bender.setCurrAbility(null);

        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 center = hit.getLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < IMPACT_COUNT; i++) {
            double angle = rng.nextDouble(0, 2 * Math.PI);
            double dist = rng.nextDouble(0, AREA_RADIUS);
            double ix = center.x + dist * Math.cos(angle);
            double iz = center.z + dist * Math.sin(angle);
            double iy = center.y;

            AABB impactArea = new AABB(ix, iy, iz, ix, iy, iz).inflate(IMPACT_RADIUS, 2.5, IMPACT_RADIUS);
            List<LivingEntity> hitEntities = level.getEntitiesOfClass(LivingEntity.class, impactArea, LivingEntity::isAlive);
            for (LivingEntity target : hitEntities) {
                target.hurt(level.damageSources().playerAttack(caster), DAMAGE_PER_IMPACT);
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), FIRE_TICKS));
                target.hurtMarked = true;
            }

            level.sendParticles(ParticleTypes.EXPLOSION, ix, iy + 0.5, iz, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.FLAME, ix, iy + 0.5, iz, 30, IMPACT_RADIUS * 0.4, 0.6, IMPACT_RADIUS * 0.4, 0.04);
            level.sendParticles(ParticleTypes.LAVA, ix, iy + 3.0, iz, 4, 0.2, 0.2, 0.2, 0.0);
            level.playSound(null, ix, iy, iz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.0f + rng.nextFloat() * 0.3f);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}