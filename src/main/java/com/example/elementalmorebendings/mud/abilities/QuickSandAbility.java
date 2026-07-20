package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.earth.EarthElement;
import net.minecraft.core.BlockPos;
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

import java.util.List;

/**
 * QuicksandAbility ("quicksand")
 * Transforma o chão numa área de lama numa poça circular e prende/afunda
 * quem estiver em cima: aplica Lentidão (grip) e Fadiga de Mineração
 * (representando o "afundar" na lama), além de um pequeno puxão pra baixo.
 */
public class QuicksandAbility implements Ability {

    private static final float BASE_COST = 25.0f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
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
        spawnMudPatch(world, player, bender, BlockPos.containing(center), radius);

        List<LivingEntity> targets = AbilitySupport.entitiesAround(player, center, radius);
        int slownessAmplifier = strongGrip ? 2 : 1;
        int durationTicks = strongGrip ? 100 : 60; // 5s ou 3s

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, slownessAmplifier));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, 1));
            AbilitySupport.pushAway(center, target, 0.0, -0.15); // pequeno "afundão"
        }

        world.sendParticles(ParticleTypes.MUD, center.x, center.y + 0.1, center.z,
                50, radius * 0.5, 0.1, radius * 0.5, 0.02);

        bender.setCurrAbility(null);
    }

    private static void spawnMudPatch(ServerLevel world, ServerPlayer player, Bender bender,
                                      BlockPos center, double radius) {
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

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}