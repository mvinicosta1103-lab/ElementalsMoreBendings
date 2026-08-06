package com.elementals.morebendings.situations;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.function.BiPredicate;

/**
 * Funções auxiliares usadas pelas condições em {@link SituationsRegistry}.
 * Tudo aqui é baseado em blocos/fluidos/luz do céu -- de propósito, pra não
 * depender de tags de bioma (que variam mais entre versões do jogo e são
 * mais fáceis de errar o nome sem poder compilar contra o jar real).
 */
public final class SituationChecks {

    private SituationChecks() {
    }

    /**
     * Conta quantas posições num raio esférico (em blocos) ao redor do
     * jogador batem com o predicado dado. Barato o bastante pros raios
     * pequenos/médios usados aqui (6-8 blocos), rodando só 1x a cada
     * {@link SituationsSystem#CHECK_INTERVAL_TICKS} ticks.
     */
    public static int countNearby(ServerPlayer player, int radius, BiPredicate<Level, BlockPos> predicate) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        double radiusSq = (double) radius * radius;
        int count = 0;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) > radiusSq) {
                continue;
            }
            if (predicate.test(level, pos)) {
                count++;
            }
        }
        return count;
    }

    /** Atalho pra {@link #countNearby} checando se o bloco é um dos listados. */
    public static int countNearbyBlocks(ServerPlayer player, int radius, Block... blocks) {
        return countNearby(player, radius, (level, pos) -> {
            BlockState state = level.getBlockState(pos);
            for (Block block : blocks) {
                if (state.is(block)) {
                    return true;
                }
            }
            return false;
        });
    }

    /** Atalho pra {@link #countNearby} checando o fluido (lava, água, etc). */
    public static int countNearbyFluid(ServerPlayer player, int radius, TagKey<Fluid> fluidTag) {
        return countNearby(player, radius, (level, pos) -> level.getFluidState(pos).is(fluidTag));
    }

    public static int countNearbyLava(ServerPlayer player, int radius) {
        return countNearbyFluid(player, radius, FluidTags.LAVA);
    }

    public static int countNearbyWater(ServerPlayer player, int radius) {
        return countNearbyFluid(player, radius, FluidTags.WATER);
    }

    /** Verdadeiro se o céu não é visível da posição do jogador (caverna/subterrâneo/coberto). */
    public static boolean isUnderground(ServerPlayer player) {
        return !player.level().canSeeSky(player.blockPosition());
    }

    /** Verdadeiro se o jogador está a céu aberto. */
    public static boolean canSeeSky(ServerPlayer player) {
        return player.level().canSeeSky(player.blockPosition());
    }

    /** Verdadeiro se está chovendo (ou nevando, que também conta como "isRainingAt") na posição do jogador. */
    public static boolean isPrecipitatingOn(ServerPlayer player) {
        return player.level().isRainingAt(player.blockPosition());
    }

    /**
     * Verdadeiro se, olhando pra baixo a partir do jogador até {@code depth}
     * blocos, só existir ar -- ou seja, um vão vazio embaixo dele (beira de
     * ilha flutuante, precipício profundo, etc), usado como aproximação de
     * "estar perto do vazio".
     */
    public static boolean hasEmptyDropBelow(ServerPlayer player, int depth) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        for (int i = 1; i <= depth; i++) {
            BlockPos below = pos.below(i);
            if (below.getY() < level.getMinBuildHeight()) {
                return true;
            }
            if (!level.getBlockState(below).isAir()) {
                return false;
            }
        }
        return true;
    }

    public static final Block[] HOT_BLOCKS = {Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.MAGMA_BLOCK};
    public static final Block[] COLD_BLOCKS = {
            Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW
    };
    public static final Block[] AMETHYST_BLOCKS = {
            Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.AMETHYST_CLUSTER,
            Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD
    };
    public static final Block[] DRIPSTONE_BLOCKS = {Blocks.DRIPSTONE_BLOCK, Blocks.POINTED_DRIPSTONE};
    public static final Block[] MUD_BLOCKS = {Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS};
    public static final Block[] SAND_BLOCKS = {Blocks.SAND, Blocks.RED_SAND};
    public static final Block[] MUSHROOM_BLOCKS = {
            Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK,
            Blocks.MUSHROOM_STEM, Blocks.CRIMSON_FUNGUS, Blocks.WARPED_FUNGUS
    };
    public static final Block[] SOUL_BLOCKS = {
            Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.SOUL_FIRE, Blocks.SOUL_TORCH, Blocks.SOUL_LANTERN
    };
    public static final Block[] LEAF_BLOCKS = {
            Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES,
            Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES
    };
}
