package com.elementals.morebendings.bending.avatarstate.moves;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "avatarColossusFist" — 3º nível de Terra do {@code AvatarElement}, filho
 * de {@code avatarStonePillars}. Diferente de {@code avatarEarthquake}
 * (radial, ao redor do caster) e {@code avatarStonePillars} (área num
 * ponto mirado), este é um soco de pedra numa LINHA RETA à frente do
 * caster -- um corredor estreito e comprido, com espigões de pedra
 * erupcionando ao longo do caminho conforme o "soco" avança, terminando
 * num impacto maior no fim do alcance.
 * <p>
 * Dano/knockback são aplicados instantaneamente no cast (como as outras
 * abilities do Avatar) -- só a animação do soco avançando é que corre ao
 * longo de vários ticks, via {@link AvatarFxScheduler}.
 */
public class AvatarColossusFistAbility implements Ability {

    private static final double RANGE = 16.0;
    private static final double HALF_WIDTH = 2.5;
    private static final float DAMAGE = 11.0f;
    private static final float PUSH_STRENGTH = 1.8f;
    private static final float CHI_COST = 38.0f;

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

        Vec3 look = caster.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 origin = caster.position();

        AABB area = caster.getBoundingBox().inflate(RANGE, 3.0, RANGE);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity target : nearby) {
            Vec3 toTarget = target.position().subtract(origin);
            double forward = toTarget.dot(look);
            if (forward < 0 || forward > RANGE) {
                continue;
            }
            Vec3 lateral = toTarget.subtract(look.scale(forward));
            if (lateral.length() > HALF_WIDTH) {
                continue;
            }
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE);
            target.setDeltaMovement(target.getDeltaMovement()
                    .add(look.x * PUSH_STRENGTH, 0.5, look.z * PUSH_STRENGTH));
            target.hurtMarked = true;
        }

        level.playSound(null, caster.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.PLAYERS, 1.6f, 0.4f);

        BlockState stone = level.getBlockState(caster.blockPosition().below());
        if (stone.isAir()) {
            stone = Blocks.STONE.defaultBlockState();
        }
        final BlockState punchBlock = stone;

        int steps = (int) RANGE;
        for (int step = 1; step <= steps; step++) {
            final int s = step;
            AvatarFxScheduler.schedule(step - 1, () -> {
                double cx = origin.x + look.x * s;
                double cz = origin.z + look.z * s;
                // corredor estreito de destroços -- o "punho" avançando
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, punchBlock),
                        cx, origin.y + 0.4, cz, 10, HALF_WIDTH * 0.5, 0.3, HALF_WIDTH * 0.5, 0.15);
                if (s % 2 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, cx, origin.y + 0.6, cz,
                            4, HALF_WIDTH * 0.3, 0.2, HALF_WIDTH * 0.3, 0.1);
                }
                if (s == steps) {
                    // impacto final -- estrondo maior no fim do alcance
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, punchBlock),
                            cx, origin.y + 0.5, cz, 40, HALF_WIDTH, 0.6, HALF_WIDTH, 0.25);
                    level.sendParticles(ParticleTypes.EXPLOSION, cx, origin.y + 0.5, cz, 1, 0.0, 0.0, 0.0, 0.0);
                    level.playSound(null, cx, origin.y, cz, SoundEvents.GENERIC_EXPLODE.value(),
                            SoundSource.PLAYERS, 1.4f, 0.6f);
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}