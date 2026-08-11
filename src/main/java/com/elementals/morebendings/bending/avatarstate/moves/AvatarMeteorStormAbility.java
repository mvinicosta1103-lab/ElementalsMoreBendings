package com.elementals.morebendings.bending.avatarstate.moves;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
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
 * Chove fogo numa área grande ao redor do ponto mirado -- vários impactos,
 * cada um com seu próprio raio de dano/ignição. Muito mais destrutivo em
 * área que qualquer FireBlast/CombustionBlast normal.
 * <p>
 * ATENÇÃO -- única mudança de timing entre as 8 abilities do Avatar: os
 * pontos de impacto são sorteados no cast (igual antes), mas cada meteoro
 * agora VISIVELMENTE cai do céu (trilha de partículas descendo) antes de
 * bater -- o dano daquele meteoro específico só acontece quando a trilha
 * chega ao chão, então o cast todo passa a durar uns 15-20 ticks (~0.75-1s)
 * em vez de ser 100% instantâneo. É proposital (dá tempo de quem está por
 * perto perceber e se mexer, e fica muito mais "chuva de meteoros" de
 * verdade) mas se preferir manter instantâneo é só remover os {@code
 * AvatarFxScheduler.schedule(...)} abaixo e aplicar dano/partículas
 * direto no loop, como nas outras abilities.
 */
public class AvatarMeteorStormAbility implements Ability {

    private static final double RANGE = 20.0;
    private static final double AREA_RADIUS = 7.0;
    private static final int IMPACT_COUNT = 6;
    private static final double IMPACT_RADIUS = 2.5;
    private static final float DAMAGE_PER_IMPACT = 5.0f;
    private static final int FIRE_TICKS = 100; // 5s
    private static final float CHI_COST = 36.0f;

    private static final double FALL_HEIGHT = 14.0;
    private static final int FALL_TICKS = 8;
    private static final int STAGGER_TICKS = 3;

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

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.5f);

        for (int i = 0; i < IMPACT_COUNT; i++) {
            double angle = rng.nextDouble(0, 2 * Math.PI);
            double dist = rng.nextDouble(0, AREA_RADIUS);
            double ix = center.x + dist * Math.cos(angle);
            double iz = center.z + dist * Math.sin(angle);
            double iy = center.y;

            int startDelay = i * STAGGER_TICKS;

            // aviso instantâneo no chão -- marca onde o meteoro vai cair antes de começar a descer
            AvatarFxScheduler.schedule(startDelay, () -> {
                level.sendParticles(ParticleTypes.LAVA, ix, iy + 0.1, iz, 6, 0.4, 0.05, 0.4, 0.0);
                level.sendParticles(ParticleTypes.SMALL_FLAME, ix, iy + 0.1, iz, 10, 0.5, 0.05, 0.5, 0.02);
                level.playSound(null, ix, iy, iz, SoundEvents.BLAZE_HURT, SoundSource.PLAYERS,
                        0.8f, 1.6f);
            });

            // trilha descendo do céu, um passo por tick
            for (int f = 0; f < FALL_TICKS; f++) {
                final double fy = iy + FALL_HEIGHT * (1.0 - (double) f / FALL_TICKS);
                AvatarFxScheduler.schedule(startDelay + f, () -> {
                    level.sendParticles(ParticleTypes.FLAME, ix, fy, iz, 6, 0.15, 0.15, 0.15, 0.02);
                    level.sendParticles(ParticleTypes.LAVA, ix, fy, iz, 1, 0.05, 0.05, 0.05, 0.0);
                });
            }

            // impacto -- dano/ignição acontecem aqui, quando a trilha chega ao chão
            AvatarFxScheduler.schedule(startDelay + FALL_TICKS, () -> {
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
                level.playSound(null, ix, iy, iz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                        1.2f, 1.0f + rng.nextFloat() * 0.3f);
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}