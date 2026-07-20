package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.earth.EarthElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * MudWallAbility ("mudWall")
 * Levanta uma parede de lama na frente do jogador, na direção pra onde ele
 * está olhando. Defensiva/instantânea, no mesmo espírito de uma earth wall,
 * só que com bloco de Mud (que empapa/atola quem tentar atravessar por
 * cima, já que Mud vanilla desacelera entidades).
 */
public class MudWallAbility implements Ability {

    private static final float BASE_COST = 20.0f;
    private static final double PLACE_DISTANCE = 3.0;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "mudWall", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        int height = bender.plrData.canUseUpgrade("mudWallHeightII") ? 4
                : (bender.plrData.canUseUpgrade("mudWallHeightI") ? 3 : 2);
        int halfWidth = bender.plrData.canUseUpgrade("mudWallWidthI") ? 2 : 1;

        ServerLevel world = player.serverLevel();
        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 0.01) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);

        Vec3 base = player.position().add(forward.scale(PLACE_DISTANCE));
        buildWall(world, player, bender, base, side, halfWidth, height);

        bender.setCurrAbility(null);
    }

    private static void buildWall(ServerLevel world, ServerPlayer player, Bender bender,
                                  Vec3 base, Vec3 side, int halfWidth, int height) {
        if (!AbilitySupport.canDamageBlocks(world)) {
            return;
        }

        for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
            Vec3 column = base.add(side.scale(lateral));
            BlockPos surface = findSurface(world, BlockPos.containing(column), 4);
            if (surface == null) {
                continue;
            }

            for (int y = 0; y < height; y++) {
                BlockPos pos = surface.above(y);
                BlockState current = world.getBlockState(pos);

                if (!current.isAir() && !current.getFluidState().isEmpty()) {
                    continue; // não substitui água/lava no meio da parede
                }
                if (!EarthElement.isBlockBendable(current, bender) && !current.isAir()) {
                    continue; // respeita blocos protegidos/não-bendable
                }
                if (!player.mayInteract((Level) world, pos)) {
                    continue; // respeita claims/regiões protegidas
                }

                world.setBlock(pos, Blocks.MUD.defaultBlockState(), 3);
            }
        }
    }

    /** Procura o bloco sólido mais alto numa janela vertical em torno do ponto dado. */
    private static BlockPos findSurface(ServerLevel world, BlockPos column, int searchRange) {
        for (int dy = searchRange; dy >= -searchRange; dy--) {
            BlockPos pos = column.offset(0, dy, 0);
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return pos.above();
            }
        }
        return null;
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}