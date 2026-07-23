package com.example.elementalmorebendings.crystal.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CrystalPrisonAbility ("crystalPrison")
 * Ramo 2 (Controle) — mira no inimigo mais próximo à frente e cristaliza
 * uma gaiola de ametista em volta dele, travando seu movimento horizontal
 * (mesmo mecanismo de trava do {@code QuicksandAbility}) e aplicando
 * Lentidão/Fadiga de Mineração por uma duração fixa. Diferente da
 * Quicksand (que prende qualquer um que entrar numa área), Crystal Prison
 * é um controle de alvo único, instantâneo, com duração fixa — quando o
 * tempo acaba (ou o alvo morre), a gaiola se desfaz e os blocos originais
 * são restaurados.
 */
public class CrystalPrisonAbility implements Ability {

    private static final float BASE_COST = 24.0f;
    private static final double RANGE = 10.0;
    private static final double ENTITY_SEARCH_WIDTH = 1.5;
    private static final int BASE_DURATION = 100; // 5s
    private static final int GRIP_BONUS_DURATION = 40; // +2s com crystalPrisonGripI

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        Object previous = bender.abilityData;
        if (previous instanceof PrisonState previousState) {
            release(player, previousState);
        }

        if (!AbilitySupport.spendUnlocked(bender, "crystalPrison", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        int cageRadius = bender.plrData.canUseUpgrade("crystalPrisonRadiusII") ? 2
                : (bender.plrData.canUseUpgrade("crystalPrisonRadiusI") ? 2 : 1);
        boolean strongGrip = bender.plrData.canUseUpgrade("crystalPrisonGripI");
        int duration = strongGrip ? BASE_DURATION + GRIP_BONUS_DURATION : BASE_DURATION;

        List<LivingEntity> candidates = AbilitySupport.entitiesInFront(player, RANGE, ENTITY_SEARCH_WIDTH);
        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (target == null) {
            bender.setCurrAbility(null);
            return;
        }

        ServerLevel world = player.serverLevel();
        Map<BlockPos, BlockState> originalStates = new LinkedHashMap<>();
        encaseTarget(world, player, bender, target, cageRadius, originalStates);

        AbilitySupport.spawnShatterBurst(world, target.position().add(0, target.getBbHeight() * 0.5, 0), 30, 0.6);

        bender.abilityData = new PrisonState(target, duration, strongGrip, originalStates);
        bender.setCurrAbility(this);
    }

    public void onTick(Bender bender) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player) || !(bender.abilityData instanceof PrisonState state)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!state.target.isAlive() || state.ticksLeft-- <= 0) {
            release(player, state);
            bender.abilityData = null;
            bender.setCurrAbility(null);
            return;
        }

        int slownessAmplifier = state.strongGrip ? 255 : 200;
        state.target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, slownessAmplifier, false, false, true));
        state.target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 1, false, false, true));

        // Trava de verdade a movimentação horizontal, não só via efeito.
        Vec3 motion = state.target.getDeltaMovement();
        state.target.setDeltaMovement(0.0, Math.max(motion.y, -0.05), 0.0);
        state.target.hasImpulse = true;
    }

    public void onRemove(Bender bender) {
        Player player2 = bender.player;
        if (bender.abilityData instanceof PrisonState state && player2 instanceof ServerPlayer player) {
            release(player, state);
        }
        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    /** Cristaliza uma gaiola de ametista (barras nas 4 direções cardeais, 2 blocos de altura) em volta do alvo. */
    private static void encaseTarget(ServerLevel world, ServerPlayer caster, Bender bender, LivingEntity target,
                                     int radius, Map<BlockPos, BlockState> originalStates) {
        if (!AbilitySupport.canDamageBlocks(world)) {
            return;
        }

        BlockPos feet = target.blockPosition();
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos bar = feet.relative(dir, radius);
            for (int y = 0; y <= 1; y++) {
                BlockPos pos = bar.above(y);
                BlockState current = world.getBlockState(pos);
                if (!current.isAir() && !current.getFluidState().isEmpty()) {
                    continue;
                }
                if (!AbilitySupport.isCrystalBendable(current, bender) && !current.isAir()) {
                    continue;
                }
                if (!caster.mayInteract((Level) world, pos)) {
                    continue;
                }
                originalStates.put(pos, current);
                world.setBlock(pos, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
            }
        }
    }

    /** Libera o alvo (remove os efeitos) e restaura os blocos da gaiola. */
    private static void release(ServerPlayer player, PrisonState state) {
        if (state.target.isAlive()) {
            state.target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            state.target.removeEffect(MobEffects.DIG_SLOWDOWN);
        }

        ServerLevel world = player.serverLevel();
        for (Map.Entry<BlockPos, BlockState> entry : state.originalStates.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!world.isLoaded(pos)) {
                continue;
            }
            world.setBlock(pos, entry.getValue(), 3);
        }
    }

    private static final class PrisonState {
        private final LivingEntity target;
        private int ticksLeft;
        private final boolean strongGrip;
        private final Map<BlockPos, BlockState> originalStates;

        private PrisonState(LivingEntity target, int ticksLeft, boolean strongGrip, Map<BlockPos, BlockState> originalStates) {
            this.target = target;
            this.ticksLeft = ticksLeft;
            this.strongGrip = strongGrip;
            this.originalStates = originalStates;
        }
    }
}