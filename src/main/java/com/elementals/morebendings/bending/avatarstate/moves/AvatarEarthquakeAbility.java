package com.elementals.morebendings.bending.avatarstate.moves;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "avatarEarthquake" — primeira ability de Terra do {@code AvatarElement}
 * (ver {@code AvatarElement}). Versão MUITO mais forte que o EarthKick
 * normal do mod base: o chão inteiro ao redor do caster treme num raio
 * gigante, lançando todo mundo por perto pro alto e causando dano de
 * impacto -- só utilizável no Avatar State (o Element inteiro só existe
 * enquanto ele está ativo, ver {@code AvatarStateManager}).
 */
public class AvatarEarthquakeAbility implements Ability {

    private static final double RADIUS = 10.0;
    private static final float DAMAGE = 9.0f;
    private static final double LAUNCH_UP = 1.1;
    private static final float CHI_COST = 35.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        bender.setCurrAbility(null); // instantânea

        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            return;
        }

        AABB area = caster.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity target : nearby) {
            double distance = Math.max(0.5, target.position().distanceTo(caster.position()));
            double falloff = Math.max(0.15, 1.0 - (distance / RADIUS));
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE * (float) falloff);
            Vec3 push = target.position().subtract(caster.position()).normalize();
            target.setDeltaMovement(target.getDeltaMovement()
                    .add(push.x * 0.6 * falloff, LAUNCH_UP * falloff, push.z * 0.6 * falloff));
            target.hurtMarked = true;
        }

        BlockState groundState = level.getBlockState(caster.blockPosition().below());
        for (int ring = 1; ring <= (int) RADIUS; ring += 2) {
            int points = 16;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i) / points;
                double x = caster.getX() + ring * Math.cos(angle);
                double z = caster.getZ() + ring * Math.sin(angle);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                        x, caster.getY() + 0.2, z, 4, 0.3, 0.4, 0.3, 0.12);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, caster.getX(), caster.getY(), caster.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.6f, 0.6f);
        level.playSound(null, caster.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.PLAYERS, 1.4f, 0.5f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}