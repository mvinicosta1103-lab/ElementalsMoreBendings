package com.elementals.morebendings.bending.avatarstate.moves;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * "avatarTidalWave" — primeira ability de Água do {@code AvatarElement}.
 * Uma onda gigante se forma na frente do caster e varre um cone enorme,
 * empurrando e machucando tudo em seu caminho -- muito além do alcance e
 * força de qualquer WaterWave normal.
 * <p>
 * Dano/empurrão continuam instantâneos (detecção de alvo no cone acontece
 * toda no momento do cast, como antes) -- só a crista da onda agora
 * viaja de verdade pra frente ao longo de vários ticks em vez de desenhar
 * o caminho inteiro de uma vez, com a largura oscilando como uma crista
 * real -- ver {@link AvatarFxScheduler}.
 */
public class AvatarTidalWaveAbility implements Ability {

    private static final double RANGE = 14.0;
    private static final double HALF_WIDTH = 6.0;
    private static final float DAMAGE = 6.0f;
    private static final float PUSH_STRENGTH = 1.6f;
    private static final float CHI_COST = 32.0f;

    private static final DustParticleOptions WAVE_DUST =
            new DustParticleOptions(new Vector3f(0.15f, 0.45f, 0.95f), 2.6f);
    private static final DustParticleOptions FOAM_DUST =
            new DustParticleOptions(new Vector3f(0.75f, 0.9f, 1.0f), 1.6f);

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
        // perpendicular à direção olhada, no plano XZ -- usado pra desenhar a crista da onda
        Vec3 side = new Vec3(-look.z, 0, look.x);

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
                    .add(look.x * PUSH_STRENGTH, 0.4, look.z * PUSH_STRENGTH));
            target.hurtMarked = true;
        }

        level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_SPLASH,
                SoundSource.PLAYERS, 1.6f, 0.6f);

        int steps = (int) RANGE;
        for (int step = 1; step <= steps; step++) {
            final int s = step;
            AvatarFxScheduler.schedule(step - 1, () -> {
                double cx = origin.x + look.x * s;
                double cz = origin.z + look.z * s;
                // crista: pico no meio da largura, baixando pras bordas -- desenhada como um
                // arco perpendicular à direção do avanço, não um bloco quadrado parado.
                int crestPoints = 9;
                for (int i = -crestPoints / 2; i <= crestPoints / 2; i++) {
                    double lateralOffset = (HALF_WIDTH * i) / (crestPoints / 2.0);
                    double wx = cx + side.x * lateralOffset;
                    double wz = cz + side.z * lateralOffset;
                    double crestHeight = 0.5 + 0.9 * (1.0 - Math.abs(lateralOffset) / HALF_WIDTH);
                    level.sendParticles(WAVE_DUST, wx, origin.y + crestHeight, wz, 3, 0.15, 0.15, 0.15, 0.0);
                    if (i % 2 == 0) {
                        level.sendParticles(FOAM_DUST, wx, origin.y + crestHeight + 0.1, wz,
                                2, 0.15, 0.1, 0.15, 0.0);
                    }
                }
                level.sendParticles(ParticleTypes.SPLASH, cx, origin.y + 0.3, cz,
                        6, HALF_WIDTH * 0.3, 0.2, HALF_WIDTH * 0.3, 0.05);

                if (s == steps) {
                    // arrebentação final: a onda "quebra" no fim do alcance
                    level.sendParticles(ParticleTypes.SPLASH, cx, origin.y + 0.5, cz,
                            30, HALF_WIDTH * 0.6, 0.6, HALF_WIDTH * 0.6, 0.15);
                    level.sendParticles(FOAM_DUST, cx, origin.y + 0.6, cz,
                            20, HALF_WIDTH * 0.5, 0.3, HALF_WIDTH * 0.5, 0.0);
                    level.playSound(null, cx, origin.y, cz, SoundEvents.GENERIC_SPLASH,
                            SoundSource.PLAYERS, 1.4f, 1.1f);
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}