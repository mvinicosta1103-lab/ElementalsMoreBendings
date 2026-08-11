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
 * "avatarGaleBlade" — 3º nível de Ar do {@code AvatarElement}, filho de
 * {@code avatarSkyfall}. Diferente de {@code avatarCyclone} (radial, ao
 * redor do caster) e {@code avatarSkyfall} (área num ponto mirado), esta
 * é uma lâmina FINA de vento comprimido que corta um corredor estreito e
 * muito longo à frente do caster -- a mais rápida e a de maior alcance
 * das 12, quase instantânea (a "trilha" visual é só pra vender a
 * velocidade, não pra dar suspense como o Meteor Storm).
 * <p>
 * Dano/knockback são aplicados instantaneamente no cast -- a trilha da
 * lâmina corre ao longo de só ~6 ticks (bem mais rápido que o Tidal
 * Wave/Colossus Fist) via {@link AvatarFxScheduler}, dando a sensação de
 * corte veloz em vez de onda se arrastando.
 */
public class AvatarGaleBladeAbility implements Ability {

    private static final double RANGE = 24.0;
    private static final double HALF_WIDTH = 1.2;
    private static final float DAMAGE = 9.0f;
    private static final float PUSH_STRENGTH = 2.4f;
    private static final float CHI_COST = 34.0f;
    private static final int TRAIL_TICKS = 6;

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
                    .add(look.x * PUSH_STRENGTH, 0.3, look.z * PUSH_STRENGTH));
            target.hurtMarked = true;
        }

        level.playSound(null, caster.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST,
                SoundSource.PLAYERS, 1.5f, 1.6f);

        for (int step = 1; step <= TRAIL_TICKS; step++) {
            final int s = step;
            final boolean lastStep = step == TRAIL_TICKS;
            AvatarFxScheduler.schedule(step - 1, () -> {
                // cada passo cobre um trecho maior que o Tidal Wave -- lâmina rápida, não onda lenta
                double segmentEnd = RANGE * s / TRAIL_TICKS;
                double segmentStart = RANGE * (s - 1) / TRAIL_TICKS;
                for (double d = segmentStart; d <= segmentEnd; d += 1.0) {
                    double x = origin.x + look.x * d;
                    double z = origin.z + look.z * d;
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, origin.y + 0.7, z, 1, 0.0, 0.0, 0.0, 0.0);
                    level.sendParticles(ParticleTypes.CLOUD, x, origin.y + 0.6, z, 3, HALF_WIDTH * 0.4, 0.15, HALF_WIDTH * 0.4, 0.03);
                }
                if (lastStep) {
                    double ex = origin.x + look.x * RANGE;
                    double ez = origin.z + look.z * RANGE;
                    level.sendParticles(ParticleTypes.GUST, ex, origin.y + 0.6, ez, 4, 0.3, 0.3, 0.3, 0.0);
                    level.playSound(null, ex, origin.y, ez, SoundEvents.PLAYER_ATTACK_SWEEP,
                            SoundSource.PLAYERS, 1.2f, 1.4f);
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}