package com.example.elementalmorebendings.lava.abilities;

import dev.saperate.elementals.Elementals;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * ObsidianFormAbility ("obsidianForm")
 *
 * Player looks at a patch of lava; it dries out and hardens into obsidian,
 * spreading outward from the aimed point over a few ticks.
 *
 * Self-contained version: doesn't depend on the ElementalsSubbending addon's
 * internal AbilitySupport class, only on the public Elementals API
 * (Bender, Element, Ability). Registered onto the existing "Lava" element
 * at runtime by ObsidianWakeMod.
 */
public class ObsidianFormAbility implements Ability {

    private static final float BASE_COST = 14.0f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!isUnlocked(bender, "obsidianForm")) {
            bender.setCurrAbility(null);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("You have not unlocked this Lava ability."), true);
            return;
        }

        if (!AbilitySupport.spendChi(bender, BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double range = bender.plrData.canUseUpgrade("obsidianFormRadiusII") ? 10.0
                : (bender.plrData.canUseUpgrade("obsidianFormRadiusI") ? 8.0 : 6.0);
        int radius = bender.plrData.canUseUpgrade("obsidianFormRadiusII") ? 3
                : (bender.plrData.canUseUpgrade("obsidianFormRadiusI") ? 2 : 1);

        HitResult hit = player.pick(range, 0.0f, false);
        Vec3 aimedAt = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getLookAngle().scale(range))
                : hit.getLocation();

        BlockPos center = BlockPos.containing((Position) aimedAt);
        ServerLevel world = player.serverLevel();

        List<BlockPos> targets = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius + 1) continue;
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.is(Blocks.LAVA)) {
                        targets.add(pos.immutable());
                    }
                }
            }
        }

        if (targets.isEmpty()) {
            bender.setCurrAbility(null);
            return;
        }

        int interval = bender.plrData.canUseUpgrade("obsidianFormSpeedI") ? 2 : 4;
        bender.abilityData = new ObsidianFormState(targets, interval);
        bender.setCurrAbility(this);
    }

    public void onTick(Bender bender) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player) || !(bender.abilityData instanceof ObsidianFormState state)) {
            bender.setCurrAbility(null);
            return;
        }

        if ((player.tickCount % state.interval) != 0) {
            return;
        }

        ServerLevel world = player.serverLevel();
        int hardened = 0;
        while (hardened < 2 && state.cursor < state.targets.size()) {
            BlockPos pos = state.targets.get(state.cursor++);
            BlockState current = world.getBlockState(pos);
            if (current.is(Blocks.LAVA) && canDamageBlocks(world) && player.mayInteract((Level) world, pos)) {
                world.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                hardened++;
            }
        }

        if (state.cursor >= state.targets.size()) {
            bender.abilityData = null;
            bender.setCurrAbility(null);
        }
    }

    public void onRemove(Bender bender) {
        if (bender.abilityData instanceof ObsidianFormState) {
            bender.abilityData = null;
        }
    }

    private static boolean isUnlocked(Bender bender, String upgradeName) {
        Element element = Element.getElement("Lava");
        return element != null && "Lava".equals(element.getName())
                && bender.hasElement(element)
                && bender.plrData.canUseUpgrade(upgradeName);
    }

    private static boolean canDamageBlocks(ServerLevel world) {
        try {
            return world.getGameRules().getBoolean(Elementals.BENDING_GRIEFING);
        } catch (LinkageError | RuntimeException ignored) {
            return world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        }
    }

    private static final class ObsidianFormState {
        private final List<BlockPos> targets;
        private final int interval;
        private int cursor;

        private ObsidianFormState(List<BlockPos> targets, int interval) {
            this.targets = targets;
            this.interval = interval;
        }
    }
}