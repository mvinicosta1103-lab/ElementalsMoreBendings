package com.elementals.morebendings.bending.watersubbendings.ice;

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
import net.minecraft.world.phys.Vec3;

/**
 * "frostNova" — terceira habilidade raiz da árvore de Ice (ver {@link
 * IceElement}). Instantânea, igual {@code TotalZeroAbility} (Temperature):
 * OBRIGATÓRIO liberar {@code currAbility} no final.
 *
 * Diferente de {@code TotalZeroAbility} (que prioriza dano + apagar fogo),
 * frostNova prioriza CONTROLE: empurra tudo ao redor do caster pra fora
 * (nova de verdade) e aplica lentidão quase total por um instante (em vez
 * de dano alto), além de congelar água próxima -- mais "estourar uma bolha
 * de gelo pra abrir espaço" do que "queimar de frio". Com {@code
 * iceMastery} comprado, o raio e a duração da lentidão aumentam.
 */
public class FrostNovaAbility implements Ability {

    private static final double RADIUS = 4.5;
    private static final float DAMAGE = 2.5f;
    private static final int SLOW_DURATION_TICKS = 50; // 2.5s
    private static final double KNOCKBACK = 0.6;
    private static final float CHI_COST = 20.0f;
    /** Bônus de iceMastery (ver {@link IceElement#hasMastery}). */
    private static final double MASTERY_RADIUS_BONUS = 1.5;
    private static final int MASTERY_SLOW_BONUS_TICKS = 30; // +1.5s

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        boolean mastery = IceElement.hasMastery(bender);
        double radius = RADIUS + (mastery ? MASTERY_RADIUS_BONUS : 0);
        int slowDuration = SLOW_DURATION_TICKS + (mastery ? MASTERY_SLOW_BONUS_TICKS : 0);

        level.sendParticles(ParticleTypes.SNOWFLAKE,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                50, radius * 0.5, 0.7, radius * 0.5, 0.03);
        level.sendParticles(ParticleTypes.CLOUD,
                caster.getX(), caster.getY() + 0.2, caster.getZ(),
                24, radius * 0.35, 0.05, radius * 0.35, 0.02);
        level.playSound(null, caster.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 1.0f, 1.5f);
        level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE,
                SoundSource.PLAYERS, 0.8f, 1.0f);

        DamageSource freeze = level.damageSources().freeze();

        AABB area = caster.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive())) {

            target.hurt(freeze, DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowDuration, 5));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() / 3));

            // Empurrão radial pra fora, saindo do centro do caster -- é uma NOVA, não só um debuff.
            Vec3 away = target.position().subtract(caster.position());
            double dist = away.length();
            if (dist > 0.001) {
                Vec3 push = away.normalize().scale(KNOCKBACK).add(0, 0.15, 0);
                target.push(push.x, push.y, push.z);
                target.hurtMarked = true;
            }
        }

        freezeNearbyWater(level, caster.blockPosition(), radius);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    /** Congela água (fonte) dentro do raio, virando gelo comum -- mesmo efeito colateral de {@code TotalZeroAbility}. */
    private void freezeNearbyWater(ServerLevel level, BlockPos center, double radius) {
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
            if (pos.distSqr(center) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            }
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}