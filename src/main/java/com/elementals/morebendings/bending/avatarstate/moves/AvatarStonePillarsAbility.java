package com.elementals.morebendings.bending.avatarstate.moves;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "avatarStonePillars" — segunda ability de Terra do {@code
 * AvatarElement}. Raycast até o ponto mirado e arremessa vários "pilares"
 * de pedra pra cima naquela área (dano/knockup em impacto), muito mais
 * amplo e destrutivo que qualquer EarthSpike normal.
 */
public class AvatarStonePillarsAbility implements Ability {

    private static final double RANGE = 18.0;
    private static final double IMPACT_RADIUS = 6.0;
    private static final float DAMAGE = 7.0f;
    private static final float CHI_COST = 30.0f;

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

        AABB area = new AABB(center, center).inflate(IMPACT_RADIUS, 4.0, IMPACT_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);
        for (LivingEntity target : nearby) {
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.9, 0.0));
            target.hurtMarked = true;
        }

        for (int i = 0; i < 10; i++) {
            double angle = (2 * Math.PI * i) / 10;
            double dist = IMPACT_RADIUS * 0.6;
            double x = center.x + dist * Math.cos(angle);
            double z = center.z + dist * Math.sin(angle);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    x, center.y + 1.5, z, 24, 0.3, 1.4, 0.3, 0.2);
        }
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.6f, 0.5f);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_BIG_FALL, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}