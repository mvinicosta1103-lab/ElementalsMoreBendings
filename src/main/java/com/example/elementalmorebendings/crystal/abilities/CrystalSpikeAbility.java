package com.example.elementalmorebendings.crystal.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.entities.earth.EarthBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * CrystalSpikeAbility ("crystalSpike")
 * Ramo 4 (Ataque) — mira num ponto (chão ou entidade) e faz um cacho de
 * espinhos de ametista irromper do impacto: dano/empurrão instantâneo em
 * quem já estiver na área, mais um punhado de blocos de terra/pedra
 * próximos ao impacto (respeitando bending-griefing) transmutados em
 * ametista e arremessados pra cima como estilhaços, reutilizando
 * {@link EarthBlockEntity} — o mesmo utilitário de "bloco voador" que
 * {@code ObsidianPillarAbility} (Lava) usa, da API pública do Elementals.
 * <p>
 * "crystalSpikePowerI"/"crystalSpikePowerII" aumentam o dano,
 * "crystalSpikeRadiusI" aumenta o raio de impacto. "crystalMastery" é o
 * capstone da árvore inteira, aninhado aqui (mesmo motivo documentado em
 * {@code MudElement}: a UpgradeTreeScreen só desenha até 4 ramos saindo da
 * raiz, então o capstone fica dentro de um ramo em vez de ocupar um 5º
 * slot).
 */
public class CrystalSpikeAbility implements Ability {

    private static final float BASE_COST = 26.0f;
    private static final double CAST_RANGE = 10.0;
    private static final double IMPACT_RADIUS = 2.5;
    private static final int SPIKE_COUNT_BASE = 4;
    private static final int SPIKE_COUNT_POWERED = 6;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "crystalSpike", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        ServerLevel world = player.serverLevel();
        boolean powered = bender.plrData.canUseUpgrade("crystalSpikePowerII");
        float damage = bender.plrData.canUseUpgrade("crystalSpikePowerI")
                ? (powered ? 9.0f : 7.0f) : 5.0f;
        double radius = bender.plrData.canUseUpgrade("crystalSpikeRadiusI") ? IMPACT_RADIUS + 1.0 : IMPACT_RADIUS;
        int spikeCount = powered ? SPIKE_COUNT_POWERED : SPIKE_COUNT_BASE;

        HitResult hit = player.pick(CAST_RANGE, 0.0f, false);
        Vec3 impact = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(CAST_RANGE))
                : hit.getLocation();
        BlockPos impactBlock = BlockPos.containing(impact);

        // Dano/empurrão instantâneo em quem já estiver na área de impacto.
        List<LivingEntity> targets = AbilitySupport.entitiesAround(player, impact, radius);
        for (LivingEntity target : targets) {
            target.hurt(world.damageSources().magic(), damage);
            AbilitySupport.pushAway(impact, target, 0.6, 0.5);
        }

        // Cristaliza blocos de terra/pedra próximos e os arremessa como
        // estilhaços de ametista, formando os espinhos ao redor do impacto.
        if (AbilitySupport.canDamageBlocks(world)) {
            List<BlockPos> sourceBlocks = findNearbyBendableBlocks(world, bender, impactBlock, spikeCount);
            for (int i = 0; i < sourceBlocks.size(); i++) {
                BlockPos source = sourceBlocks.get(i);
                world.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());

                double angle = (2.0 * Math.PI * i) / Math.max(1, sourceBlocks.size());
                double ox = Math.cos(angle) * radius * 0.5;
                double oz = Math.sin(angle) * radius * 0.5;
                Vec3 spikeTarget = new Vec3(impact.x + ox, impact.y + 1.4 + (i % 2), impact.z + oz);

                EarthBlockEntity entity = new EarthBlockEntity(world, player,
                        source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5);
                entity.setBlockState(Blocks.AMETHYST_BLOCK.defaultBlockState());
                entity.setTargetPosition(spikeTarget.toVector3f());
                entity.setShiftToFreeze(false);
                entity.setDamageOnTouch(true);
                entity.setDamage(damage * 0.5f);
                entity.setMaxLifeTime(20);
                entity.setDropOnEndOfLife(false);
                entity.setMovementSpeed(0.75f);

                world.addFreshEntity(entity);
            }
        }

        AbilitySupport.spawnShatterBurst(world, impact, 40, radius * 0.4);

        bender.setCurrAbility(null);
    }

    /**
     * Procura, numa varredura em anéis crescentes ao redor do ponto de
     * impacto, até {@code max} blocos de terra/pedra dobráveis (não-ar) —
     * mesmo padrão de busca usado por {@code ObsidianPillarAbility}, só
     * que em blocos comuns em vez de obsidiana já existente.
     */
    private static List<BlockPos> findNearbyBendableBlocks(ServerLevel world, Bender bender, BlockPos center, int max) {
        List<BlockPos> found = new ArrayList<>();
        search:
        for (int r = 0; r <= 4; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
                    for (int y = -3; y <= 1; y++) {
                        BlockPos pos = center.offset(x, y, z);
                        BlockState state = world.getBlockState(pos);
                        if (!state.isAir() && EarthElement.isBlockBendable(state, bender)) {
                            found.add(pos.immutable());
                            if (found.size() >= max) break search;
                        }
                    }
                }
            }
        }
        return found;
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}