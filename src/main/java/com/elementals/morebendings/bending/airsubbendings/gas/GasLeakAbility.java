package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Ramo de especialização "gasLeak" (ver {@link GasSkillTree}) — em vez de
 * afetar só o instante do cast como o {@link GasSuffocateAbility}, deixa uma
 * nuvem residual ({@link AreaEffectCloud} vanilla) no chão, que continua
 * afetando quem passar por perto por um tempo. Pensado mais pra controle de
 * área/armadilha do que dano direto.
 *
 * Exclusivo com gasSuffocate e gasIgnite.
 *
 * Duração escala com:
 *  - gasLeakDurationI → +100 ticks (5s)
 */
public class GasLeakAbility {

    private static final int BASE_DURATION_TICKS = 200; // 10s
    private static final float RADIUS = 3.5f;

    public static void register() {
        // Acionada a partir de GasCloudAbility.execute(), igual as outras
        // especializações — ver applyOnCloud() abaixo.
    }

    public static void applyOnCloud(ServerPlayer caster, ServerLevel level, double radius) {
        if (!GasElement.hasUpgrade(caster, GasSkillTree.GAS_LEAK)) {
            return;
        }
        AreaEffectCloud cloud = new AreaEffectCloud(level, caster.getX(), caster.getY(), caster.getZ());
        cloud.setOwner(caster);
        cloud.setRadius(RADIUS);
        cloud.setRadiusPerTick((0.0f - cloud.getRadius()) / (float) getDuration(caster));
        cloud.setDuration(getDuration(caster));
        cloud.setParticle(ParticleTypes.CLOUD);
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0));
        level.addFreshEntity(cloud);
    }

    public static int getDuration(ServerPlayer player) {
        int duration = BASE_DURATION_TICKS;
        if (GasElement.hasUpgrade(player, GasSkillTree.GAS_LEAK_DURATION_I)) duration += 100;
        return duration;
    }

    private GasLeakAbility() {
    }
}