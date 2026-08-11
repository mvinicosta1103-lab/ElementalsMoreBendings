package com.elementals.morebendings.bending.avatarstate.moves;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "avatarCyclone" — primeira ability de Ar do {@code AvatarElement}. Um
 * vórtice gigante se forma ao redor do caster, lançando tudo por perto
 * pra cima e pra fora em espiral -- muito maior e mais forte que qualquer
 * AirSuction/AirBlast normal.
 * <p>
 * Empurrão/dano continuam instantâneos (como antes). O funil visual agora
 * de fato SOBE em espiral dupla ao longo de vários ticks (2 hélices
 * entrelaçadas subindo e girando), em vez de um anel achatado desenhado
 * de uma vez -- ver {@link AvatarFxScheduler}.
 */
public class AvatarCycloneAbility implements Ability {

    private static final double RADIUS = 9.0;
    private static final float DAMAGE = 4.0f;
    private static final float CHI_COST = 30.0f;

    private static final int FUNNEL_STEPS = 10;
    private static final double FUNNEL_HEIGHT = 6.0;

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

        AABB area = caster.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity target : nearby) {
            Vec3 outward = target.position().subtract(caster.position());
            double dist = Math.max(0.5, outward.length());
            Vec3 outDir = outward.normalize();
            // Componente tangencial (perpendicular ao raio, no plano XZ) pra dar a sensação de espiral,
            // somado a um empurrão pra fora e pra cima.
            Vec3 tangent = new Vec3(-outDir.z, 0, outDir.x);
            double falloff = Math.max(0.25, 1.0 - dist / RADIUS);
            target.setDeltaMovement(target.getDeltaMovement()
                    .add(outDir.x * 0.5 * falloff + tangent.x * 0.7 * falloff,
                            1.3 * falloff,
                            outDir.z * 0.5 * falloff + tangent.z * 0.7 * falloff));
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE * (float) falloff);
            target.hurtMarked = true;
        }

        double cx = caster.getX();
        double cz = caster.getZ();
        double baseY = caster.getY() + 0.5;

        level.playSound(null, caster.blockPosition(), SoundEvents.PHANTOM_FLAP,
                SoundSource.PLAYERS, 1.6f, 0.6f);
        level.playSound(null, caster.blockPosition(), SoundEvents.ELYTRA_FLYING,
                SoundSource.PLAYERS, 1.2f, 1.0f);

        for (int step = 0; step < FUNNEL_STEPS; step++) {
            final int s = step;
            AvatarFxScheduler.schedule(step, () -> {
                // funil: raio maior embaixo, afunilando pro topo -- e girando mais rápido perto do topo.
                double heightFrac = (double) s / FUNNEL_STEPS;
                double y = baseY + FUNNEL_HEIGHT * heightFrac;
                double funnelRadius = RADIUS * (1.0 - heightFrac * 0.6);
                double spin = s * 0.8;

                // duas hélices entrelaçadas (offset de PI uma da outra)
                for (int helix = 0; helix < 2; helix++) {
                    double baseAngle = spin + helix * Math.PI;
                    for (int i = 0; i < 8; i++) {
                        double angle = baseAngle + i * 0.35;
                        double r = funnelRadius * (0.4 + 0.6 * i / 8.0);
                        double x = cx + r * Math.cos(angle);
                        double z = cz + r * Math.sin(angle);
                        level.sendParticles(ParticleTypes.CLOUD, x, y + i * 0.08, z, 2, 0.15, 0.15, 0.15, 0.02);
                    }
                }
                if (s % 3 == 0) {
                    level.sendParticles(ParticleTypes.GUST, cx, y, cz, 1, funnelRadius * 0.3, 0.1, funnelRadius * 0.3, 0.0);
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}