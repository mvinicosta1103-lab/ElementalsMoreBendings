package com.elementals.morebendings.bending.airsubbendings.atmosphere;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * "atmosphericDome" — segunda habilidade raiz da árvore de Atmosphere (ver
 * {@link AtmosphereElement}). Canalizada enquanto agachado, igual
 * {@code AirShieldAbility} do mod base: sem Entity nem Manager próprio —
 * toda a varredura de raio roda direto em {@link #onTick}, centrada em
 * {@code player.position()} a cada chamada, então a cúpula acompanha o
 * caster com o jogo já rodando, sem nenhum atraso de interpolação.
 * <p>
 * - Reflete projéteis: em vez de só desviar, calcula a direção de volta a
 *   partir do centro da cúpula e reaplica a velocidade original nessa
 *   direção -- o projétil "quica" de volta.
 * - Empurra toda entidade (exceto o caster) radialmente pra fora do raio.
 * - Quebra blocos leves (folhas, grama, teia, etc.) dentro do raio.
 * <p>
 * Raio e custo de chi/tick escalam com os upgrades de nível (recalculados
 * todo tick, é barato -- só leituras de HashMap, igual {@code
 * GasCloudAbility#getRadius}):
 *  - domeRadiusI / II      → +0.5 bloco de raio cada
 *  - domeEfficiencyI / II  → -0.05 de custo de chi/tick cada (mais tempo
 *    canalizando com a mesma reserva de chi)
 */
public class AtmosphericDomeAbility implements Ability {

    private static final double BASE_RADIUS = 2.5;
    private static final float BASE_TICK_CHI_COST = 0.25f;
    private static final float MIN_TICK_CHI_COST = 0.1f;
    private static final float CAST_CHI_COST = 6.0f;
    private static final double PUSH_STRENGTH = 0.5;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer) || !(player.level() instanceof ServerLevel)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        bender.setCurrAbility(this); // canalizada -- solta ao soltar Shift, ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!player.isShiftKeyDown()) {
            onRemove(bender);
            return;
        }
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!bender.reduceChi(getTickChiCost(caster))) {
            onRemove(bender);
            return;
        }

        double radius = getRadius(caster);
        reflectProjectiles(level, player, radius);
        pushEntitiesAway(level, player, radius);
        breakLightBlocks(level, player, radius);
        spawnShellParticles(level, player, radius);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static double getRadius(ServerPlayer player) {
        double radius = BASE_RADIUS;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.DOME_RADIUS_I)) radius += 0.5;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.DOME_RADIUS_II)) radius += 0.5;
        return radius;
    }

    public static float getTickChiCost(ServerPlayer player) {
        float cost = BASE_TICK_CHI_COST;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.DOME_EFFICIENCY_I)) cost -= 0.05f;
        if (AtmosphereElement.hasUpgrade(player, AtmosphereElement.DOME_EFFICIENCY_II)) cost -= 0.05f;
        return Math.max(cost, MIN_TICK_CHI_COST);
    }

    private void reflectProjectiles(ServerLevel level, Player player, double radius) {
        Vec3 center = player.position();
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(radius), Projectile::isAlive)) {

            double speed = projectile.getDeltaMovement().length();
            if (speed < 0.05) {
                speed = 0.6;
            }
            Vec3 awayFromCenter = projectile.position().subtract(center).normalize();
            projectile.setDeltaMovement(awayFromCenter.scale(speed));
            projectile.hasImpulse = true;
        }
    }

    private void pushEntitiesAway(ServerLevel level, Player player, double radius) {
        Vec3 center = player.position();
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(radius),
                e -> e != player && e instanceof LivingEntity)) {

            Vec3 away = entity.position().subtract(center);
            double dist = away.length();
            if (dist < 0.001) {
                away = new Vec3(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5);
                dist = away.length();
            }
            Vec3 push = away.scale(PUSH_STRENGTH / Math.max(dist, 0.5));
            entity.push(push.x, Math.max(push.y, 0.05), push.z);
        }
    }

    private void breakLightBlocks(ServerLevel level, Player player, double radius) {
        int r = (int) Math.ceil(radius);
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, r, r))) {
            if (pos.distSqr(center) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isLightBlock(state)) {
                level.destroyBlock(pos.immutable(), false, player);
            }
        }
    }

    private boolean isLightBlock(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.WOOL_CARPETS)
                || state.is(Blocks.COBWEB)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN);
    }

    private void spawnShellParticles(ServerLevel level, Player player, double radius) {
        if (level.getGameTime() % 2 != 0) {
            return;
        }
        Vec3 center = player.position().add(0, player.getBbHeight() / 2f, 0);
        for (int i = 0; i < 4; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * level.random.nextDouble() - 1);
            double x = center.x + radius * Math.sin(phi) * Math.cos(theta);
            double y = center.y + radius * Math.cos(phi);
            double z = center.z + radius * Math.sin(phi) * Math.sin(theta);
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0, 0, 0);
        }
    }
}