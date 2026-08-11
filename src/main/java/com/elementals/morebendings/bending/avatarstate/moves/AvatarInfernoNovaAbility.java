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
 * "avatarInfernoNova" — primeira ability de Fogo do {@code AvatarElement}.
 * O caster libera uma explosão de fogo em todas as direções ao redor de
 * si -- dano alto + ignição de tudo por perto, sem se machucar. Muito mais
 * forte que qualquer FireBlast normal.
 * <p>
 * Dano/ignição continuam instantâneos (como antes). Visualmente, em vez
 * de um {@code EXPLOSION_EMITTER} sozinho: primeiro sobe um anel de
 * pilares de chama ao redor do caster, que estoura pra fora em 3 ondas de
 * fogo se expandindo -- ver {@link AvatarFxScheduler}.
 */
public class AvatarInfernoNovaAbility implements Ability {

    private static final double RADIUS = 8.0;
    private static final float DAMAGE = 10.0f;
    private static final int FIRE_TICKS = 160; // 8s -- usa setRemainingFireTicks, não setSecondsOnFire (1.21.1)
    private static final float CHI_COST = 34.0f;

    private static final int FLAME_PILLARS = 10;
    private static final int NOVA_WAVES = 3;

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
            double distance = Math.max(0.5, target.position().distanceTo(caster.position()));
            double falloff = Math.max(0.2, 1.0 - (distance / RADIUS));
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE * (float) falloff);
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), (int) (FIRE_TICKS * falloff)));
            Vec3 push = target.position().subtract(caster.position()).normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(push.x * 0.5, 0.35, push.z * 0.5));
            target.hurtMarked = true;
        }

        double cx = caster.getX();
        double cy = caster.getY() + 1.0;
        double cz = caster.getZ();

        // ignição imediata no caster -- feedback instantâneo, antes da coreografia começar
        level.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 20, 0.3, 0.3, 0.3, 0.03);
        level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.4f, 0.8f);

        // anel de pilares de chama subindo ao redor do caster, um pouco espalhados no tempo
        double pillarRadius = RADIUS * 0.35;
        for (int i = 0; i < FLAME_PILLARS; i++) {
            double angle = (2 * Math.PI * i) / FLAME_PILLARS;
            double px = cx + pillarRadius * Math.cos(angle);
            double pz = cz + pillarRadius * Math.sin(angle);
            int delay = i % 2; // duas leves ondas, não tudo no mesmo tick
            AvatarFxScheduler.schedule(delay, () -> {
                for (double h = 0.0; h <= 1.8; h += 0.3) {
                    level.sendParticles(ParticleTypes.FLAME, px, cy - 1.0 + h, pz, 3, 0.1, 0.05, 0.1, 0.02);
                }
                level.sendParticles(ParticleTypes.LAVA, px, cy - 1.0, pz, 1, 0.1, 0.0, 0.1, 0.0);
            });
        }

        // a nova em si: 3 ondas de fogo se expandindo pra fora a partir do caster
        for (int wave = 1; wave <= NOVA_WAVES; wave++) {
            final int w = wave;
            final boolean lastWave = wave == NOVA_WAVES;
            AvatarFxScheduler.schedule(2 + wave, () -> {
                double waveRadius = RADIUS * w / NOVA_WAVES;
                int points = 12 + w * 4;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = cx + waveRadius * Math.cos(angle);
                    double z = cz + waveRadius * Math.sin(angle);
                    level.sendParticles(ParticleTypes.FLAME, x, cy - 0.6, z, 3, 0.2, 0.3, 0.2, 0.02);
                    if (i % 2 == 0) {
                        level.sendParticles(ParticleTypes.LAVA, x, cy - 0.6, z, 1, 0.1, 0.1, 0.1, 0.0);
                    }
                }
                level.playSound(null, cx, cy, cz, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS,
                        0.8f, 0.8f + w * 0.1f);

                if (lastWave) {
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
                    level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                            SoundSource.PLAYERS, 1.6f, 1.1f);
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}