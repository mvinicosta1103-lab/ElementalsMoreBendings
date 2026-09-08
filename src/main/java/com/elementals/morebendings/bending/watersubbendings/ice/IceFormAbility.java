package com.elementals.morebendings.bending.watersubbendings.ice;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * "iceForm" — nó filho de {@link IceElement#ICE_SHARD} (mesmo esquema de
 * {@code CrystalStepAbility} pendurada embaixo de {@code crystalShard}):
 * aponta pra um bloco e alterna o estado dele entre água e gelo.
 *
 * Raycast MANUAL via {@code level.clip(ClipContext)} até {@link #RANGE},
 * com {@link ClipContext.Fluid#ANY} -- diferente da maioria das outras
 * abilities do addon, que usam {@code SapsUtils.raycastFull(player, RANGE,
 * false)}: esse utilitário passa {@code Fluid.NONE} pro clip, então o raio
 * atravessa a água direto e acerta o bloco sólido debaixo dela -- por isso
 * o "congelar" nunca disparava (a mira nunca era reconhecida como água),
 * só o "derreter" (gelo é sólido, sempre acertado normalmente). Fazendo o
 * clip aqui mesmo com {@code Fluid.ANY} a água passa a parar o raio na sua
 * própria superfície, igual {@code MetalGrappleAbility} faz pra blocos.
 *
 * Dois casos, decididos pelo bloco/fluido mirado:
 *
 *  - Água (fonte, {@link Fluids#WATER}) -- vira {@link Blocks#ICE}.
 *  - Gelo já formado ({@link #MELTABLE}, inclui {@code PACKED_ICE}/
 *    {@code BLUE_ICE}/{@code FROSTED_ICE}) -- derrete de volta pra
 *    {@link Blocks#WATER} (fonte).
 *
 * Afeta um pequeno cluster (o bloco mirado + vizinhos imediatos no mesmo
 * nível, até {@link #RADIUS}) em vez de um bloco só, pra parecer mais um
 * efeito de dobra do que um clique de "trocar bloco". Instantânea -- não
 * trava {@code currAbility} (mesmo esquema de {@code IceShardAbility}).
 */
public class IceFormAbility implements Ability {

    private static final double RANGE = 6.0;
    private static final int RADIUS = 1; // cluster 3x3 no plano XZ do bloco mirado
    private static final float CHI_COST = 8.0f;

    /** Qualquer forma de gelo que a ability sabe derreter de volta pra água. */
    private static final Set<Block> MELTABLE = Set.of(
            Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.FROSTED_ICE
    );

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 eye = caster.getEyePosition();
        Vec3 reach = eye.add(caster.getLookAngle().scale(RANGE));
        // Fluid.ANY -- sem isso o raio ignora água e atravessa até o bloco
        // sólido debaixo dela, nunca detectando a mira em água (ver JavaDoc).
        HitResult hit = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, caster));
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            caster.displayClientMessage(Component.literal("Aponte para água ou gelo."), true);
            bender.setCurrAbility(null);
            return;
        }

        BlockPos center = blockHit.getBlockPos();
        boolean isWaterSource = level.getFluidState(center).is(Fluids.WATER) && level.getFluidState(center).isSource();
        boolean isMeltable = MELTABLE.contains(level.getBlockState(center).getBlock());

        if (!isWaterSource && !isMeltable) {
            caster.displayClientMessage(Component.literal("Aponte para água ou gelo."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        if (isWaterSource) {
            freezeCluster(level, center);
        } else {
            meltCluster(level, center);
        }

        bender.setCurrAbility(null); // instantânea -- libera a trava pra poder usar de novo
    }

    /** Vira água em gelo no bloco mirado e nos vizinhos imediatos que também forem fonte de água. */
    private void freezeCluster(ServerLevel level, BlockPos center) {
        int changed = 0;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (level.getFluidState(pos).is(Fluids.WATER) && level.getFluidState(pos).isSource()) {
                    level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.SNOWFLAKE,
                            pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.25, 0.05, 0.25, 0.01);
                    changed++;
                }
            }
        }
        if (changed > 0) {
            level.playSound(null, center, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    /** Derrete gelo (qualquer variante de {@link #MELTABLE}) de volta pra água no bloco mirado e vizinhos. */
    private void meltCluster(ServerLevel level, BlockPos center) {
        int changed = 0;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos pos = center.offset(dx, 0, dz);
                if (MELTABLE.contains(level.getBlockState(pos).getBlock())) {
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.SPLASH,
                            pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.25, 0.05, 0.25, 0.01);
                    changed++;
                }
            }
        }
        if (changed > 0) {
            level.playSound(null, center, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 1.1f);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}