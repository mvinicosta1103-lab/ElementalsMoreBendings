package com.example.elementalmorebendings.plant.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * RootGraspAbility ("rootGrasp")
 * <p>
 * O Root Snare original (habilidade base do addon Elementals Subbending,
 * fechado/closed-source — mesmo caso do Vine Whip) não tem NENHUM efeito
 * visual: ele só imobiliza o alvo, sem nenhuma partícula, som ou animação
 * indicando o que aconteceu. Como não dá pra editar a classe original (não
 * temos acesso ao código-fonte dela, só ao .jar), a solução é a mesma
 * usada pro Vine Whip → Vine Arc: uma habilidade NOVA, anexada como filho
 * do mesmo ramo ("rootSnare") na árvore, que recria o MESMO CONCEITO
 * (prender o inimigo mais próximo por um tempo) com uma animação de
 * verdade em cima.
 * <p>
 * A animação roda em background por {@link #GRASP_TICKS} ticks (~0.75s):
 * raízes/vinhas sobem em espiral ao redor do alvo, o raio da espiral vai
 * fechando conforme o tempo passa (como um punho se fechando), e só
 * quando a espiral termina de "fechar" é que o efeito de lentidão/prisão
 * é realmente aplicado — assim fica claro, visualmente, que o alvo está
 * sendo agarrado, e não é só um debuff instantâneo sem explicação.
 */
public class RootGraspAbility implements Ability {

    private static final float BASE_COST = 16.0f;
    private static final int GRASP_TICKS = 15;

    /** Estado da animação em andamento, guardado como "background data" no Bender. */
    private static final class GraspState {
        final LivingEntity target;
        final ServerLevel world;
        final int slowAmplifier;
        final int slowDuration;
        int tick = 0;

        GraspState(LivingEntity target, ServerLevel world, int slowAmplifier, int slowDuration) {
            this.target = target;
            this.world = world;
            this.slowAmplifier = slowAmplifier;
            this.slowDuration = slowDuration;
        }
    }

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        // evita reiniciar a animação em cima de um agarrão que já está
        // rolando (ex: o jogador aperta a tecla de novo por acidente).
        if (bender.isAbilityInBackground(this)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "rootGrasp", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double range = bender.plrData.canUseUpgrade("rootGraspRangeI") ? 10.0 : 7.0;
        int slowAmp = bender.plrData.canUseUpgrade("rootGraspPowerI") ? 3 : 1;
        int slowDuration = bender.plrData.canUseUpgrade("rootGraspPowerI") ? 60 : 40;

        List<LivingEntity> candidates = AbilitySupport.entitiesInFront(player, range, 1.6);
        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (target == null) {
            bender.setCurrAbility(null);
            return;
        }

        ServerLevel world = player.serverLevel();
        world.playSound(null, BlockPos.containing(target.position()),
                SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.6f);

        bender.addBackgroundAbility(this, new GraspState(target, world, slowAmp, slowDuration));
        bender.setCurrAbility(null);
    }

    public void onBackgroundTick(Bender bender, Object data) {
        if (!(data instanceof GraspState state)) {
            bender.removeAbilityFromBackground(this);
            return;
        }

        // se o alvo morreu ou saiu do mundo no meio da animação, cancela
        // sem aplicar a prisão final.
        if (!state.target.isAlive()) {
            bender.removeAbilityFromBackground(this);
            return;
        }

        if (state.tick >= GRASP_TICKS) {
            finishGrasp(state);
            bender.removeAbilityFromBackground(this);
            return;
        }

        drawGraspFrame(state);
        state.tick++;
    }

    /**
     * Um frame da animação: raízes subindo em espiral ao redor do alvo, com
     * o raio da espiral encolhendo conforme {@code tick} avança — como um
     * punho de vinhas se fechando em volta do corpo do alvo.
     */
    private static void drawGraspFrame(GraspState state) {
        LivingEntity target = state.target;
        double t = state.tick / (double) GRASP_TICKS;

        double radius = 0.9 - 0.55 * t;
        double height = target.getBbHeight() * Math.min(1.0, t * 1.3 + 0.15);
        int strands = 4;
        int pointsPerStrand = 6;

        Vec3 base = target.position();

        for (int s = 0; s < strands; s++) {
            double baseAngle = (2.0 * Math.PI * s / strands) + state.tick * 0.35;
            for (int p = 0; p < pointsPerStrand; p++) {
                double h = height * (p / (double) (pointsPerStrand - 1));
                double angle = baseAngle + h * 2.4; // espiral subindo
                double x = base.x + Math.cos(angle) * radius;
                double z = base.z + Math.sin(angle) * radius;
                double y = base.y + h;

                state.world.sendParticles(AbilitySupport.VINE_DUST, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
                if (p % 2 == 0) {
                    state.world.sendParticles(AbilitySupport.LEAF_DUST, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }

        if (state.tick % 4 == 0) {
            state.world.sendParticles(ParticleTypes.CRIT, base.x, base.y + 0.2, base.z, 3, 0.3, 0.1, 0.3, 0.0);
        }
    }

    /** Aplica a prisão de fato, com um "estouro" final marcando o momento em que o punho fecha. */
    private static void finishGrasp(GraspState state) {
        LivingEntity target = state.target;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, state.slowDuration, state.slowAmplifier));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.min(20, state.slowDuration), 0));

        state.world.sendParticles(AbilitySupport.VINE_DUST,
                target.getX(), target.getY(0.5), target.getZ(), 20, 0.35, 0.4, 0.35, 0.02);
        state.world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX(), target.getY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.01);
        state.world.playSound(null, BlockPos.containing(target.position()),
                SoundEvents.VINE_STEP, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    public void onRemove(Bender bender) {
        bender.removeAbilityFromBackground(this);
    }
}