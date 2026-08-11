package com.elementals.morebendings.bending.avatarstate.moves;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * "avatarDeepFreeze" — 3º nível de Água do {@code AvatarElement}, filho
 * de {@code avatarMaelstrom}. Diferente de {@code avatarTidalWave}
 * (empurrão numa linha) e {@code avatarMaelstrom} (puxão pro centro),
 * este CONGELA os alvos no lugar -- não literalmente encaixotados em
 * blocos de gelo de verdade (evita ter que gerenciar/reverter blocos no
 * mundo), mas com {@code MOVEMENT_SLOWDOWN}/{@code DIG_SLOWDOWN} em
 * amplitude alta por alguns segundos, o suficiente pra imobilizar na
 * prática -- com cristais de gelo subindo ao redor de cada alvo pra
 * vender bem a ideia.
 * <p>
 * Dano/efeito são aplicados instantaneamente no cast -- a animação de
 * cristais de gelo subindo corre ao longo de alguns ticks via
 * {@link AvatarFxScheduler}.
 */
public class AvatarDeepFreezeAbility implements Ability {

    private static final double RANGE = 16.0;
    private static final double IMPACT_RADIUS = 5.0;
    private static final float DAMAGE = 6.0f;
    private static final float CHI_COST = 34.0f;
    private static final int FREEZE_TICKS = 70; // ~3.5s
    private static final int FREEZE_AMPLIFIER = 3; // Slowness IV
    private static final int GROWTH_STEPS = 5;

    private static final DustParticleOptions FROST_DUST =
            new DustParticleOptions(new Vector3f(0.75f, 0.95f, 1.0f), 1.8f);

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

        AABB area = new AABB(center, center).inflate(IMPACT_RADIUS, 3.5, IMPACT_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);

        for (LivingEntity target : nearby) {
            double distance = Math.max(0.5, target.position().distanceTo(center));
            double falloff = Math.max(0.3, 1.0 - distance / IMPACT_RADIUS);
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE * (float) falloff);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, FREEZE_AMPLIFIER, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, FREEZE_TICKS, 2, false, true));
            target.hurtMarked = true;
        }

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);
        level.sendParticles(FROST_DUST, center.x, center.y + 0.2, center.z,
                20, IMPACT_RADIUS * 0.4, 0.2, IMPACT_RADIUS * 0.4, 0.02);

        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();

        // anel de cristais de gelo subindo ao redor do centro, um pouco espalhados no tempo
        int crystalPoints = 10;
        for (int i = 0; i < crystalPoints; i++) {
            double angle = (2 * Math.PI * i) / crystalPoints;
            double dist = IMPACT_RADIUS * (0.4 + 0.5 * (i % 2));
            double px = center.x + dist * Math.cos(angle);
            double pz = center.z + dist * Math.sin(angle);
            int startDelay = i % 3;

            AvatarFxScheduler.schedule(startDelay, () -> {
                for (int step = 1; step <= GROWTH_STEPS; step++) {
                    final int s = step;
                    AvatarFxScheduler.schedule(step - 1, () -> {
                        double topY = center.y + (1.6 * s) / GROWTH_STEPS;
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ice),
                                px, topY, pz, 4, 0.15, 0.1, 0.15, 0.03);
                        level.sendParticles(ParticleTypes.SNOWFLAKE, px, topY, pz, 2, 0.1, 0.1, 0.1, 0.01);
                        if (s == GROWTH_STEPS) {
                            level.playSound(null, px, topY, pz, SoundEvents.GLASS_PLACE,
                                    SoundSource.PLAYERS, 0.8f, 1.3f);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}