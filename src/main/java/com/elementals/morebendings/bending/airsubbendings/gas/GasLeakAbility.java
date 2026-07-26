package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;

/**
 * Ramo de especialização "gasLeak" (ver {@link GasElement}) — deixa uma
 * nuvem residual no chão, que continua afetando quem passar por perto
 * (exceto o próprio caster) por um tempo.
 *
 * A {@link AreaEffectCloud} aqui é usada só pra visual/posição — a
 * aplicação de efeito de verdade é manual, via {@link GasLeakManager},
 * porque a nuvem vanilla afeta o próprio owner/thrower por padrão e a
 * regra é o dobrador nunca ser afetado pelo próprio gás.
 *
 * Efeitos: Náusea + Envenenamento (não Lentidão — isso é só na explosão
 * inicial da nuvem em {@link GasCloudAbility}).
 *
 * Duração escala com:
 *  - gasLeakDurationI → +100 ticks (5s)
 */
public class GasLeakAbility {

    private static final int BASE_DURATION_TICKS = 200; // 10s
    private static final float RADIUS = 3.5f;

    public static void applyOnCloud(ServerPlayer caster, ServerLevel level, double radius) {
        if (!GasElement.hasUpgrade(caster, GasElement.GAS_LEAK)) {
            return;
        }
        int duration = getDuration(caster);

        AreaEffectCloud cloud = new AreaEffectCloud(level, caster.getX(), caster.getY(), caster.getZ());
        cloud.setOwner(caster);
        cloud.setRadius(RADIUS);
        cloud.setRadiusPerTick((0.0f - cloud.getRadius()) / (float) duration);
        cloud.setDuration(duration);
        cloud.setParticle(ParticleTypes.CLOUD);
        // Sem addEffect aqui de propósito -- ver GasLeakManager.
        level.addFreshEntity(cloud);

        GasLeakManager.register(level, cloud, caster);
    }

    public static int getDuration(ServerPlayer player) {
        int duration = BASE_DURATION_TICKS;
        if (GasElement.hasUpgrade(player, GasElement.GAS_LEAK_DURATION_I)) duration += 100;
        return duration;
    }

    private GasLeakAbility() {
    }
}