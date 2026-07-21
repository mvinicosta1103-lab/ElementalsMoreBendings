package com.example.elementalmorebendings.crystal.abilities;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * CrystalWallAbility ("crystalWall")
 * Ramo 1 (Defesa) — cristaliza o chão na frente do jogador numa parede de
 * ametista maciça, na direção pra onde ele está olhando. Mesmo padrão de
 * {@code MudWallAbility}, usando {@link CrystalStructureAnimator} pra
 * erguer a parede camada por camada e depois recolhê-la, restaurando os
 * blocos originais.
 * <p>
 * Cada conjuração é independente: várias paredes podem coexistir em
 * lugares diferentes ao mesmo tempo, cada uma subindo/descendo no seu
 * próprio ritmo. Só as {@code MAX_CONCURRENT_WALLS} mais recentes ficam de
 * pé por vez.
 */
public class CrystalWallAbility implements Ability {

    private static final float BASE_COST = 20.0f;
    private static final double PLACE_DISTANCE = 3.0;
    private static final int TICKS_PER_LAYER = 2;
    private static final int HOLD_DURATION_TICKS = 160; // ~8s de pé
    private static final int MAX_CONCURRENT_WALLS = 3;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "crystalWall", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        int height = bender.plrData.canUseUpgrade("crystalWallHeightII") ? 4
                : (bender.plrData.canUseUpgrade("crystalWallHeightI") ? 3 : 2);
        int halfWidth = bender.plrData.canUseUpgrade("crystalWallWidthI") ? 2 : 1;

        ServerLevel world = player.serverLevel();
        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 0.01) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);

        Vec3 base = player.position().add(forward.scale(PLACE_DISTANCE));

        TreeMap<Integer, List<BlockPos>> layerMap = new TreeMap<>();
        Map<BlockPos, BlockState> originalStates = new LinkedHashMap<>();
        planWall(world, player, bender, base, side, halfWidth, height, layerMap, originalStates);

        if (originalStates.isEmpty()) {
            bender.setCurrAbility(null);
            return;
        }

        List<List<BlockPos>> layers = new ArrayList<>(layerMap.values());

        Object previousData = bender.getBackgroundAbilityData(this);
        List<CrystalStructureAnimator> activeWalls;
        if (previousData instanceof List<?> rawList) {
            @SuppressWarnings("unchecked")
            List<CrystalStructureAnimator> casted = (List<CrystalStructureAnimator>) rawList;
            activeWalls = casted;
        } else {
            activeWalls = new ArrayList<>();
        }

        while (activeWalls.size() >= MAX_CONCURRENT_WALLS) {
            activeWalls.remove(0).cancelAndRestoreAll();
        }

        activeWalls.add(new CrystalStructureAnimator(world, layers, originalStates,
                Blocks.AMETHYST_BLOCK.defaultBlockState(), TICKS_PER_LAYER, HOLD_DURATION_TICKS));
        bender.addBackgroundAbility(this, activeWalls);

        bender.setCurrAbility(null);
    }

    /** Planeja as posições da parede, agrupadas por altura (y) pra animação subir camada por camada. */
    private static void planWall(ServerLevel world, ServerPlayer player, Bender bender,
                                 Vec3 base, Vec3 side, int halfWidth, int height,
                                 TreeMap<Integer, List<BlockPos>> layerMap,
                                 Map<BlockPos, BlockState> originalStates) {
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

                originalStates.put(pos, current);
                layerMap.computeIfAbsent(y, k -> new ArrayList<>()).add(pos);
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

    public void onBackgroundTick(Bender bender, Object data) {
        if (!(data instanceof List<?> rawList)) {
            bender.removeAbilityFromBackground(this);
            return;
        }

        @SuppressWarnings("unchecked")
        List<CrystalStructureAnimator> activeWalls = (List<CrystalStructureAnimator>) rawList;

        activeWalls.removeIf(CrystalStructureAnimator::tick);

        if (activeWalls.isEmpty()) {
            bender.removeAbilityFromBackground(this);
        }
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}