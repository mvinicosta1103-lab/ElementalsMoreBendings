package com.example.elementalmorebendings.lava.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.entities.earth.EarthBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * ObsidianPillarAbility ("obsidianPillar")
 *
 * This is what lets a Lava bender "control" obsidian, the way Earth benders
 * control earth/stone: obsidian is intentionally NOT in Elementals' earth
 * bendable-blocks tag (it's treated as special), so normal earthbending
 * can't move it. This ability reuses the same EarthBlockEntity animation
 * class Earth abilities use, but only works on obsidian blocks, and only
 * for people who have Lava unlocked.
 *
 * Gathers up to `height` nearby obsidian blocks and hurls them upward,
 * stacking into a spike at the aimed spot. Anything caught underneath gets
 * hit and knocked up.
 */
public class ObsidianPillarAbility implements Ability {

    private static final float BASE_COST = 18.0f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "obsidianPillar", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        ServerLevel world = player.serverLevel();
        if (!AbilitySupport.canDamageBlocks(world)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = player.pick(8.0, 0.0f, false);
        if (hit.getType() == HitResult.Type.MISS) {
            bender.setCurrAbility(null);
            return;
        }

        BlockPos targetColumn = BlockPos.containing((Position) hit.getLocation());
        int height = bender.plrData.canUseUpgrade("obsidianPillarTallI") ? 5 : 3;
        int searchRadius = 4;

        List<BlockPos> sourceBlocks = new ArrayList<>();
        search:
        for (int r = 0; r <= searchRadius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
                    for (int y = -2; y <= 2; y++) {
                        BlockPos pos = targetColumn.offset(x, y, z);
                        if (world.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                            sourceBlocks.add(pos.immutable());
                            if (sourceBlocks.size() >= height) break search;
                        }
                    }
                }
            }
        }

        if (sourceBlocks.isEmpty()) {
            bender.setCurrAbility(null);
            return;
        }

        for (int i = 0; i < sourceBlocks.size(); i++) {
            BlockPos source = sourceBlocks.get(i);
            BlockState state = world.getBlockState(source);
            world.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());

            Vec3 target = targetColumn.getCenter().relative(Direction.UP, i + 0.6);

            EarthBlockEntity entity = new EarthBlockEntity(world, player,
                    source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5);
            entity.setBlockState(state);
            entity.setTargetPosition(target.toVector3f());
            entity.setShiftToFreeze(false);
            entity.setDamageOnTouch(true);
            entity.setDamage(3);
            entity.setMaxLifeTime(25);
            entity.setDropOnEndOfLife(true);
            entity.setMovementSpeed(0.6f);

            world.addFreshEntity(entity);
        }

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
