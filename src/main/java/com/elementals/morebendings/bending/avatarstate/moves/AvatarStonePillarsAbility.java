package com.elementals.morebendings.bending.avatarstate.moves;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "avatarStonePillars" — segunda ability de Terra do {@code
 * AvatarElement}. Raycast até o ponto mirado e arremessa vários "pilares"
 * de pedra pra cima naquela área (dano/knockup em impacto), muito mais
 * amplo e destrutivo que qualquer EarthSpike normal.
 * <p>
 * Dano/knockup continuam instantâneos no momento do impacto (como antes).
 * A parte visual agora é uma sequência de verdade: os pilares erupcionam
 * um a um ao redor do círculo (não todos juntos) e cada um CRESCE de
 * baixo pra cima ao longo de alguns ticks, em vez de aparecer pronto —
 * ver {@link AvatarFxScheduler}.
 */
public class AvatarStonePillarsAbility implements Ability {

    private static final double RANGE = 18.0;
    private static final double IMPACT_RADIUS = 6.0;
    private static final float DAMAGE = 7.0f;
    private static final float CHI_COST = 30.0f;

    private static final int PILLAR_COUNT = 8;
    private static final double PILLAR_HEIGHT = 2.6;
    private static final int GROWTH_STEPS = 6; // ticks pra um pilar crescer do chão até o topo
    private static final int STAGGER_TICKS = 2; // atraso entre um pilar começar e o próximo

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        bender.setCurrAbility(null);

        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 center = hit.getLocation();

        AABB area = new AABB(center, center).inflate(IMPACT_RADIUS, 4.0, IMPACT_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);
        for (LivingEntity target : nearby) {
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.9, 0.0));
            target.hurtMarked = true;
        }

        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState deepslate = Blocks.COBBLED_DEEPSLATE.defaultBlockState();

        // marca o alvo instantaneamente (poeira baixa) pra dar aviso do impacto vindo
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stone),
                center.x, center.y + 0.1, center.z, 16, IMPACT_RADIUS * 0.5, 0.1, IMPACT_RADIUS * 0.5, 0.05);

        for (int i = 0; i < PILLAR_COUNT; i++) {
            double angle = (2 * Math.PI * i) / PILLAR_COUNT;
            double dist = IMPACT_RADIUS * 0.6;
            double px = center.x + dist * Math.cos(angle);
            double pz = center.z + dist * Math.sin(angle);
            double baseY = center.y;
            BlockState blockState = (i % 2 == 0) ? stone : deepslate;
            int startDelay = i * STAGGER_TICKS;

            AvatarFxScheduler.schedule(startDelay, () -> {
                level.playSound(null, px, baseY, pz, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.3f, 0.6f);
                for (int step = 1; step <= GROWTH_STEPS; step++) {
                    final int s = step;
                    AvatarFxScheduler.schedule(step - 1, () -> {
                        double topY = baseY + (PILLAR_HEIGHT * s) / GROWTH_STEPS;
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                                px, topY, pz, 10, 0.35, 0.15, 0.35, 0.05);
                        if (s == GROWTH_STEPS) {
                            // topo do pilar terminando de subir -- estilhaço final + som de impacto
                            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                                    px, topY, pz, 18, 0.4, 0.2, 0.4, 0.15);
                            level.playSound(null, px, topY, pz, SoundEvents.GENERIC_BIG_FALL,
                                    SoundSource.PLAYERS, 1.0f, 0.9f);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}