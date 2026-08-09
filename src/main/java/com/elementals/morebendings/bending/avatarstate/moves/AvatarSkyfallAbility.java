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

/**
 * "avatarSkyfall" — segunda ability de Ar do {@code AvatarElement}. O
 * caster arremessa uma rajada de vento comprimido na área mirada, que
 * "explode" numa onda de choque radial ao impactar -- empurra e machuca
 * tudo pra fora do centro, com alcance e força muito maiores que qualquer
 * AirBlast normal.
 */
public class AvatarSkyfallAbility implements Ability {

    private static final double RANGE = 20.0;
    private static final double RADIUS = 8.0;
    private static final float DAMAGE = 6.0f;
    private static final float PUSH_STRENGTH = 2.0f;
    private static final float CHI_COST = 32.0f;

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

        AABB area = new AABB(center, center).inflate(RADIUS, 3.5, RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);

        for (LivingEntity target : nearby) {
            Vec3 outward = target.position().subtract(center);
            double dist = Math.max(0.5, outward.length());
            double falloff = Math.max(0.2, 1.0 - dist / RADIUS);
            Vec3 outDir = outward.normalize();
            target.setDeltaMovement(target.getDeltaMovement()
                    .add(outDir.x * PUSH_STRENGTH * falloff, 0.5 * falloff, outDir.z * PUSH_STRENGTH * falloff));
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE * (float) falloff);
            target.hurtMarked = true;
        }

        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                80, RADIUS * 0.5, 0.6, RADIUS * 0.5, 0.15);
        level.sendParticles(ParticleTypes.GUST, center.x, center.y, center.z, 6, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.6f, 0.8f);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}