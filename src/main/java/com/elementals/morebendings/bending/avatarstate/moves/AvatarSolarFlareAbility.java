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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.List;

/**
 * "avatarSolarFlare" — 3º nível de Fogo do {@code AvatarElement}, filho
 * de {@code avatarMeteorStorm}. Diferente de {@code avatarInfernoNova}
 * (dano+ignição puro) e {@code avatarMeteorStorm} (chuva de área), este é
 * mais controle que dano bruto: um clarão ofuscante que cega todo mundo
 * por perto (raio cheio, sem falloff -- cegueira é tudo ou nada) além de
 * um dano moderado que aí sim cai com a distância.
 * <p>
 * Dano/cegueira são aplicados instantaneamente no cast -- a animação é só
 * o clarão se expandindo em ondas de luz ao longo de alguns ticks, via
 * {@link AvatarFxScheduler}.
 */
public class AvatarSolarFlareAbility implements Ability {

    private static final double RADIUS = 9.0;
    private static final float DAMAGE = 8.0f;
    private static final float CHI_COST = 36.0f;
    private static final int BLIND_TICKS = 100; // 5s
    private static final int WAVES = 3;

    private static final DustParticleOptions FLARE_DUST =
            new DustParticleOptions(new Vector3f(1.0f, 0.95f, 0.6f), 2.4f);

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
            double falloff = Math.max(0.25, 1.0 - distance / RADIUS);
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE * (float) falloff);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0, false, true));
            target.hurtMarked = true;
        }

        double cx = caster.getX();
        double cy = caster.getY() + 1.2;
        double cz = caster.getZ();

        // núcleo do clarão -- instantâneo, o "flash" em si
        level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 30, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.6f, 2.0f);
        level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.0f, 1.8f);

        // ondas de luz se expandindo pra fora
        for (int wave = 1; wave <= WAVES; wave++) {
            final int w = wave;
            AvatarFxScheduler.schedule(wave, () -> {
                double waveRadius = RADIUS * w / WAVES;
                int points = 14 + w * 4;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double x = cx + waveRadius * Math.cos(angle);
                    double z = cz + waveRadius * Math.sin(angle);
                    level.sendParticles(FLARE_DUST, x, cy - 0.6, z, 3, 0.15, 0.25, 0.15, 0.0);
                    if (i % 3 == 0) {
                        level.sendParticles(ParticleTypes.END_ROD, x, cy - 0.6, z, 1, 0.05, 0.1, 0.05, 0.01);
                    }
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}