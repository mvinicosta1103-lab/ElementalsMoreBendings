package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Ramo de especialização "gasLeak" (ver {@link GasElement}) — deixa uma
 * nuvem residual no chão, que continua envenenando quem passar por perto
 * (exceto o próprio caster) por um tempo -- ver {@link GasLeakManager}
 * pra aplicação de efeito de verdade tick a tick.
 * <p>
 * NOVO (pedido do usuário): além do veneno em quem passa pela nuvem, o
 * gás também deixa o CHÃO por baixo dela infértil -- terra lavrada vira
 * terra crua (destruindo a plantação em cima) e grama/micélio/podzol
 * viram terra crua também. Efeito de solo é PERMANENTE (não volta
 * sozinho, ao contrário da nuvem em si), aplicado uma única vez no
 * momento do cast em {@link #spoilGround}.
 * <p>
 * REWORK: Suffocate/Leak/Ignite deixaram de ser "efeitos alternados por
 * uma tecla de cycle" e viraram abilities independentes, cada uma com
 * tecla própria -- não existe mais uma especialização "ativa" pra
 * checar aqui. Só faz efeito se o jogador tiver comprado o nó.
 * <p>
 * Duração da nuvem escala com:
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
        spoilGround(level, caster);
    }

    /** Deixa o chão por baixo da nuvem infértil -- terra lavrada e grama/micélio/podzol viram terra crua. */
    private static void spoilGround(ServerLevel level, ServerPlayer caster) {
        BlockPos center = caster.blockPosition();
        int r = (int) Math.ceil(RADIUS);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }
                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
                BlockState state = level.getBlockState(ground);

                if (state.is(Blocks.FARMLAND)) {
                    BlockPos cropPos = ground.above();
                    if (!level.getBlockState(cropPos).isAir()) {
                        level.destroyBlock(cropPos, false); // derruba a plantação sem soltar item -- ela apodrece, não é colhida
                    }
                    level.setBlock(ground, Blocks.DIRT.defaultBlockState(), 3);
                } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
                    level.setBlock(ground, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }
    }

    public static int getDuration(ServerPlayer player) {
        int duration = BASE_DURATION_TICKS;
        if (GasElement.hasUpgrade(player, GasElement.GAS_LEAK_DURATION_I)) duration += 100;
        return duration;
    }

    private GasLeakAbility() {
    }
}