package com.example.obsidianwake.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * EruptionAbility ("eruption")
 * Strikes the ground in front of the player, dealing damage, knockback and
 * fire to everything caught in the blast radius. Instant, offensive AoE.
 * Também deixa uma piscina de lava 8x8 no chão, na área do impacto.
 */
public class EruptionAbility implements Ability {

    private static final float BASE_COST = 30.0f;
    private static final int LAVA_POOL_SIZE = 8; // piscina 8x8

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "eruption", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double radius = bender.plrData.canUseUpgrade("eruptionRadiusII") ? 5.0
                : (bender.plrData.canUseUpgrade("eruptionRadiusI") ? 4.0 : 3.0);
        float damage = bender.plrData.canUseUpgrade("eruptionPowerI") ? 8.0f : 5.0f;

        HitResult hit = player.pick(6.0, 0.0f, false);
        Vec3 center = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(6.0))
                : hit.getLocation();

        ServerLevel world = player.serverLevel();
        List<LivingEntity> targets = AbilitySupport.entitiesAround(player, center, radius);
        for (LivingEntity target : targets) {
            target.hurt(world.damageSources().magic(), damage);
            AbilitySupport.pushAway(center, target, 1.3, 0.6);
            target.igniteForSeconds(4.0f);
        }

        world.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.2, center.z,
                40, radius * 0.5, 0.3, radius * 0.5, 0.05);
        world.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.2, center.z,
                30, radius * 0.5, 0.2, radius * 0.5, 0.02);

        spawnLavaPool(world, BlockPos.containing(center), LAVA_POOL_SIZE);

        bender.setCurrAbility(null);
    }

    /**
     * Cria uma piscina de lava plana de size x size blocos, centrada no
     * ponto de impacto. Cada uma das colunas procura, numa janela vertical
     * pequena em volta do Y do impacto, o bloco sólido mais alto daquela
     * coluna (a "superfície" real do terreno ali) e o substitui por lava —
     * assim a piscina acompanha pequenos desníveis do terreno em vez de
     * ficar flutuando ou furar um buraco reto.
     * <p>
     * Respeita a mesma regra que as outras habilidades de bloco do addon
     * (bending-griefing / mobGriefing) e nunca substitui bedrock nem cria
     * uma coluna de lava caindo num vazio (pula colunas sem chão sólido
     * embaixo).
     */
    private static void spawnLavaPool(ServerLevel world, BlockPos impactCenter, int size) {
        if (!AbilitySupport.canDamageBlocks(world)) {
            return;
        }

        int start = -(size / 2);
        int end = size / 2; // size=8 -> colunas de -4 a +3 (8 no total, centrado no impacto)

        for (int dx = start; dx < end; dx++) {
            for (int dz = start; dz < end; dz++) {
                BlockPos surface = findSurface(world, impactCenter.offset(dx, 0, dz));
                if (surface == null) {
                    continue;
                }

                BlockState below = world.getBlockState(surface.below());
                if (below.isAir() || !below.getFluidState().isEmpty()) {
                    // sem chão sólido embaixo dessa coluna — pula, pra não
                    // criar uma cachoeira de lava escorrendo num buraco/vazio
                    continue;
                }

                BlockState current = world.getBlockState(surface);
                if (current.is(Blocks.BEDROCK) || current.is(Blocks.LAVA)) {
                    continue;
                }

                world.setBlock(surface, Blocks.LAVA.defaultBlockState(), 3);
            }
        }
    }

    /**
     * Procura, numa janela vertical pequena (2 acima até 3 abaixo) ao redor
     * do Y do impacto, o bloco sólido/não-líquido mais alto daquela coluna.
     * Retorna null se não achar nenhum bloco sólido nessa janela (ex.:
     * coluna caindo num precipício ou caverna).
     */
    private static BlockPos findSurface(ServerLevel world, BlockPos columnAtImpactY) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = columnAtImpactY.offset(0, dy, 0);
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}