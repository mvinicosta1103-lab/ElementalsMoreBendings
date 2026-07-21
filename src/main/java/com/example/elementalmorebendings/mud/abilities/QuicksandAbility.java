package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.earth.EarthElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * QuicksandAbility ("quicksand")
 * Transforma o chão numa poça circular de lama e prende/trava quem estiver
 * em cima: os alvos ficam completamente travados (sem conseguir se mover),
 * exatamente enquanto o jogador que conjurou mantiver segurado o agachar
 * (shift). A poça de lama e o travamento dos alvos só desaparecem quando o
 * jogador solta o agachar (ou perde o canal por outro motivo) — os blocos
 * originais são restaurados nesse momento, em vez de ficarem lá pra sempre
 * ou de virarem ar.
 */
public class QuicksandAbility implements Ability {

    private static final float BASE_COST = 25.0f;
    private static final float TICK_COST = 0.3f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        Object previous = bender.abilityData;
        if (previous instanceof QuicksandState previousState) {
            release(bender, previousState);
        }

        if (!AbilitySupport.spendUnlocked(bender, "quicksand", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double radius = bender.plrData.canUseUpgrade("quicksandRadiusII") ? 5.0
                : (bender.plrData.canUseUpgrade("quicksandRadiusI") ? 4.0 : 3.0);
        boolean strongGrip = bender.plrData.canUseUpgrade("quicksandGripI");

        HitResult hit = player.pick(8.0, 0.0f, false);
        Vec3 center = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(8.0))
                : hit.getLocation();

        ServerLevel world = player.serverLevel();
        Map<BlockPos, BlockState> originalStates = new LinkedHashMap<>();
        spawnMudPatch(world, player, bender, BlockPos.containing(center), radius, originalStates);

        QuicksandState state = new QuicksandState(center, radius, strongGrip, originalStates);

        // Prende instantaneamente quem já estiver em cima da poça no momento do cast.
        gripTargets(world, player, state);

        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                center.x, center.y + 0.1, center.z,
                50, radius * 0.5, 0.1, radius * 0.5, 0.02);

        bender.abilityData = state;
        bender.setCurrAbility(this);
    }

    public void onTick(Bender bender) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player) || !(bender.abilityData instanceof QuicksandState state)) {
            bender.setCurrAbility(null);
            return;
        }

        // O canal só continua enquanto o jogador mantiver o agachar segurado.
        if (!player.isShiftKeyDown() || !AbilitySupport.spendChiPerTick(bender, TICK_COST)) {
            release(bender, state);
            bender.setCurrAbility(null);
            return;
        }

        ServerLevel world = player.serverLevel();
        gripTargets(world, player, state); // continua prendendo qualquer alvo novo que entrar na poça

        for (LivingEntity target : state.stuck) {
            if (!target.isAlive()) {
                continue;
            }
            int slownessAmplifier = state.strongGrip ? 255 : 200;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, slownessAmplifier, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 1, false, false, true));

            // Trava de verdade a movimentação horizontal, não só via efeito.
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(0.0, Math.max(motion.y, -0.05), 0.0);
            target.hasImpulse = true;
        }
    }

    public void onRemove(Bender bender) {
        if (bender.abilityData instanceof QuicksandState state) {
            release(bender, state);
        }
        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    private static void gripTargets(ServerLevel world, ServerPlayer caster, QuicksandState state) {
        for (LivingEntity target : AbilitySupport.entitiesAround(caster, state.center, state.radius)) {
            state.stuck.add(target);
        }
    }

    /** Solta os alvos travados e restaura os blocos originais da poça. */
    private static void release(Bender bender, QuicksandState state) {
        for (LivingEntity target : state.stuck) {
            if (!target.isAlive()) {
                continue;
            }
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            target.removeEffect(MobEffects.DIG_SLOWDOWN);
        }

        Player player2 = bender.player;
        if (player2 instanceof ServerPlayer player) {
            ServerLevel world = player.serverLevel();
            for (Map.Entry<BlockPos, BlockState> entry : state.originalStates.entrySet()) {
                BlockPos pos = entry.getKey();
                if (!world.isLoaded(pos)) {
                    continue;
                }
                world.setBlock(pos, entry.getValue(), 3);
            }
        }
    }

    private static void spawnMudPatch(ServerLevel world, ServerPlayer player, Bender bender,
                                      BlockPos center, double radius, Map<BlockPos, BlockState> originalStates) {
        if (!AbilitySupport.canDamageBlocks(world)) {
            return;
        }

        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue; // mantém o padrão circular
                }

                BlockPos surface = findSurface(world, center.offset(dx, 0, dz));
                if (surface == null) {
                    continue;
                }

                BlockState current = world.getBlockState(surface);
                if (current.is(Blocks.BEDROCK) || !current.getFluidState().isEmpty()) {
                    continue;
                }
                if (!EarthElement.isBlockBendable(current, bender)) {
                    continue;
                }
                if (!player.mayInteract((Level) world, surface)) {
                    continue;
                }

                originalStates.put(surface, current);
                world.setBlock(surface, Blocks.MUD.defaultBlockState(), 3);
            }
        }
    }

    private static BlockPos findSurface(ServerLevel world, BlockPos column) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = column.offset(0, dy, 0);
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    private static final class QuicksandState {
        private final Vec3 center;
        private final double radius;
        private final boolean strongGrip;
        private final Map<BlockPos, BlockState> originalStates;
        private final Set<LivingEntity> stuck = new HashSet<>();

        private QuicksandState(Vec3 center, double radius, boolean strongGrip, Map<BlockPos, BlockState> originalStates) {
            this.center = center;
            this.radius = radius;
            this.strongGrip = strongGrip;
            this.originalStates = originalStates;
        }
    }
}