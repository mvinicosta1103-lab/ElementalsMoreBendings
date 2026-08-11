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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * "avatarEarthquake" — primeira ability de Terra do {@code AvatarElement}
 * (ver {@code AvatarElement}). Versão MUITO mais forte que o EarthKick
 * normal do mod base: o chão inteiro ao redor do caster treme num raio
 * gigante, lançando todo mundo por perto pro alto e causando dano de
 * impacto -- só utilizável no Avatar State (o Element inteiro só existe
 * enquanto ele está ativo, ver {@code AvatarStateManager}).
 * <p>
 * Dano/knockup continuam 100% instantâneos (igual antes) -- só a "casca"
 * visual virou uma sequência: em vez de um anel de partículas tudo de uma
 * vez, uma rachadura corre pra fora em ondas (ver {@link AvatarFxScheduler}),
 * com destroços estourando pra cima ao longo do caminho, terminando num
 * estrondo maior na borda.
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

        double cx = caster.getX();
        double cy = caster.getY();
        double cz = caster.getZ();
        double y0 = caster.getY() + 0.2;
        BlockState groundState = level.getBlockState(caster.blockPosition().below());
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // impacto imediato debaixo do caster, antes da rachadura correr pra fora
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                cx, y0, cz, 20, 0.6, 0.3, 0.6, 0.15);
        level.playSound(null, caster.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.PLAYERS, 1.4f, 0.5f);

        int rings = 0;
        for (int ring = 1; ring <= (int) RADIUS; ring += 2) {
            final int r = ring;
            final boolean lastRing = ring + 2 > RADIUS;
            AvatarFxScheduler.schedule(rings, () -> {
                if (!level.isLoaded(caster.blockPosition())) {
                    return;
                }
                int points = 16;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = cx + r * Math.cos(angle);
                    double z = cz + r * Math.sin(angle);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                            x, y0, z, 4, 0.3, 0.4, 0.3, 0.12);
                }
                // destroços estourando pra cima em alguns pontos ao longo desse anel --
                // a "pilar de entulho" que dá o soco visual da rachadura passando.
                int rubblePoints = 4;
                for (int i = 0; i < rubblePoints; i++) {
                    double angle = rng.nextDouble(0, 2 * Math.PI);
                    double x = cx + r * Math.cos(angle);
                    double z = cz + r * Math.sin(angle);
                    for (double h = 0.0; h <= 1.3; h += 0.35) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                                x, y0 + h, z, 3, 0.15, 0.1, 0.15, 0.05 + h * 0.05);
                    }
                }
                float pitch = Math.max(0.35f, 0.75f - r * 0.03f);
                level.playSound(null, cx, cy, cz, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.1f, pitch);

                if (lastRing) {
                    level.sendParticles(ParticleTypes.EXPLOSION, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
                    level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(),
                            SoundSource.PLAYERS, 1.6f, 0.6f);
                }
            });
            rings++;
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}