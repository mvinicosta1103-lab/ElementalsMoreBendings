package com.elementals.morebendings.bending.airsubbendings.temperature;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "scorchingWave" — segunda habilidade raiz da árvore de Temperature (ver
 * {@link TemperatureElement}). Instantânea, igual {@code TotalZeroAbility},
 * só que o lado oposto: calor extremo em vez de frio absoluto.
 *
 * Toda criatura viva (exceto o caster) dentro do raio:
 *  - recebe dano de fogo ({@code damageSources().onFire()});
 *  - pega fogo por alguns segundos (igniteForSeconds);
 *  - tem qualquer congelamento acumulado zerado (ticksFrozen = 0) --
 *    contrário direto do totalZero.
 *
 * O próprio caster ganha Resistência a Fogo por um tempo curto (não faz
 * sentido incendiar coisas ao redor e pegar fogo também), e blocos de
 * gelo/neve dentro do raio derretem de volta em água/ar.
 */
public class ScorchingWaveAbility implements Ability {

    private static final double RADIUS = 5.0;
    private static final float DAMAGE = 4.0f;
    private static final float IGNITE_SECONDS = 5.0f;
    private static final int FIRE_RESISTANCE_DURATION_TICKS = 100; // 5s
    private static final int BASE_COOLDOWN_TICKS = 120; // 6s
    private static final float CAST_CHI_COST = 4.0f;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -100000L);
        if (now - last < BASE_COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        level.sendParticles(ParticleTypes.FLAME,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                40, RADIUS * 0.5, 0.6, RADIUS * 0.5, 0.02);
        level.sendParticles(ParticleTypes.LAVA,
                caster.getX(), caster.getY() + 0.2, caster.getZ(),
                10, RADIUS * 0.4, 0.1, RADIUS * 0.4, 0.0);
        level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_BURN,
                SoundSource.PLAYERS, 1.0f, 0.9f);

        DamageSource fireDamage = level.damageSources().onFire();

        AABB area = caster.getBoundingBox().inflate(RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive())) {

            target.hurt(fireDamage, DAMAGE);
            target.igniteForSeconds(IGNITE_SECONDS);
            target.setTicksFrozen(0);
        }

        caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_RESISTANCE_DURATION_TICKS, 0));

        meltNearbyIceAndSnow(level, caster.blockPosition());

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    /** Derrete gelo/neve dentro do raio de volta em água/ar, oposto do totalZero. */
    private void meltNearbyIceAndSnow(ServerLevel level, BlockPos center) {
        int r = (int) Math.ceil(RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
            if (pos.distSqr(center) > RADIUS * RADIUS) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
                level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            } else if (state.is(Blocks.SNOW)) {
                level.removeBlock(pos, false);
            } else if (state.is(Blocks.SNOW_BLOCK)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}