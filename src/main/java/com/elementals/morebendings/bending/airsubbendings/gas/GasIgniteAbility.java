package com.elementals.morebendings.bending.airsubbendings.gas;

import com.elementals.morebendings.bending.airsubbendings.common.SpecializationCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Ramo de especialização "gasIgnite" (ver {@link GasElement}) — ignita a
 * nuvem de gás: fogo nos alvos (o dobrador que castou NUNCA é afetado,
 * igual às outras especializações) E no CHÃO ao redor (pedido do
 * usuário: "vai incendiar a área, alvo e chão").
 * <p>
 * Fogo no chão não cobre 100% da área de propósito (ver {@link
 * #GROUND_FIRE_CHANCE}) -- pareceria um piso xadrez artificial em vez de
 * um incêndio orgânico se cobrisse tudo. É fogo de verdade (bloco {@link
 * Blocks#FIRE}), então segue as regras normais do vanilla depois de
 * colocado (pode se apagar sozinho, se espalhar pra blocos inflamáveis
 * vizinhos, etc.).
 * <p>
 * Comprar não é mais exclusivo com Suffocate/Leak (ver {@link
 * SpecializationCycle}); só faz efeito se for a especialização
 * ATUALMENTE ATIVA pro jogador (alternada pela tecla de "Cycle
 * Specialization").
 * <p>
 * É um nó terminal (sem upgrades de melhoria ainda) e o mais caro dos três
 * (3 pontos), justamente por não precisar de investimento extra pra ser forte.
 */
public class GasIgniteAbility {

    private static final int FIRE_SECONDS = 6;
    private static final int FIRE_TICKS = FIRE_SECONDS * 20;
    private static final float GROUND_FIRE_CHANCE = 0.45f;
    private static final int MAX_GROUND_HEIGHT_DIFF = 3; // não desce/sobe além disso em relação ao caster (evita acender um penhasco/caverna distante embaixo)

    public static void applyOnCloud(ServerPlayer caster, ServerLevel level, double radius) {
        if (!GasElement.hasUpgrade(caster, GasElement.GAS_IGNITE)
                || !SpecializationCycle.isGasActive(caster, GasElement.GAS_IGNITE)) {
            return;
        }

        level.sendParticles(ParticleTypes.FLAME,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                (int) (20 * (radius / 3.0)), radius * 0.4, 0.5, radius * 0.4, 0.02);
        level.sendParticles(ParticleTypes.LAVA,
                caster.getX(), caster.getY() + 0.5, caster.getZ(),
                6, radius * 0.3, 0.2, radius * 0.3, 0.0);

        AABB area = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());
        for (LivingEntity entity : nearby) {
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), FIRE_TICKS));
        }

        igniteGround(caster, level, radius);
    }

    /** Acende blocos de fogo em cima do chão ao redor do caster. */
    private static void igniteGround(ServerPlayer caster, ServerLevel level, double radius) {
        BlockPos casterPos = caster.blockPosition();
        int r = (int) Math.ceil(radius);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos column = casterPos.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();

                if (Math.abs(ground.getY() - casterPos.getY()) > MAX_GROUND_HEIGHT_DIFF) {
                    continue;
                }
                if (!level.getFluidState(ground).isEmpty()) {
                    continue; // água/lava embaixo -- não acende
                }
                BlockPos firePos = ground.above();
                if (!level.getBlockState(firePos).isAir()) {
                    continue; // já tem bloco ali -- não substitui nada
                }
                if (level.random.nextFloat() > GROUND_FIRE_CHANCE) {
                    continue;
                }
                level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }

    private GasIgniteAbility() {
    }
}