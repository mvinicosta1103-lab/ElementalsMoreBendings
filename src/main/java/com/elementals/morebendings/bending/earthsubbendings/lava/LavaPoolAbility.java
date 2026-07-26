package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * "lavaPool" — habilidade raiz da árvore de Lava (ver {@link LavaElement}).
 *
 * Raycast na direção mirada (mesmo {@link SapsUtils#raycastFull} que
 * {@code MudTrapAbility}/{@code AbilityEarthTrap} usam) até {@link #RANGE}
 * blocos. No ponto de impacto, converte o chão numa área {@link #RADIUS}
 * (uma "poça" quadrada) em {@link Blocks#LAVA} de verdade — dano/ignição de
 * quem pisar nela vêm de graça do comportamento vanilla do bloco, igual o
 * {@code MudTrapAbility} não precisa aplicar slowness manualmente porque
 * {@link Blocks#MUD} já faz isso sozinho.
 *
 * A poça não é permanente: fica registrada no {@link LavaPoolManager}, que
 * a esfria em {@link Blocks#BASALT} depois de {@link #COOL_AFTER_TICKS}
 * (ver lá o motivo de basalto e não pedra/obsidiana).
 *
 * AVISO DE DESIGN: como são blocos de LAVA de verdade (não uma partícula
 * decorativa), eles PODEM escorrer pras bordas se o terreno ao redor não
 * for plano — igual um balde de lava comum. Isso é proposital (é uma
 * habilidade ofensiva, lava sem risco nenhum seria sem graça), mas se quiser
 * conter o escorrimento, dá pra prender um "muro" de {@link Blocks#BASALT} na
 * borda da área antes de colocar a lava — não fiz isso aqui pra manter a
 * primeira versão simples; ver {@link #createPool} se quiser adicionar.
 */
public class LavaPoolAbility implements Ability {

    private static final double RANGE = 8.0;
    /** Poça quadrada de (2*RADIUS+1)² blocos -- RADIUS=1 -> 3x3. */
    private static final int RADIUS = 1;
    /** Quanto tempo (em ticks de servidor) a poça fica como lava de verdade antes de esfriar. 20 ticks = 1s. */
    static final int COOL_AFTER_TICKS = 200; // 10s

    /** Blocos de terreno "naturais" que a lava pode substituir. Nada de baú, madeira trabalhada, etc. */
    private static final Set<Block> MOLTENABLE = Set.of(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.MUD,
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.CLAY,
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE
    );

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 hitPos = hit.getLocation();
        BlockPos hitColumn = BlockPos.containing(hitPos.x, hitPos.y, hitPos.z);
        BlockPos center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hitColumn).below();

        List<BlockPos> pool = createPool(level, center);

        if (!pool.isEmpty()) {
            LavaPoolManager.registerPool(level, pool);
            level.playSound(null, center, SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.9f);
        }

        // Instantânea -- não trava a habilidade (mesmo esquema da CrystalShardAbility).
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    /** Converte o chão numa área quadrada em lava. Retorna as posições que viraram lava de verdade. */
    private List<BlockPos> createPool(ServerLevel level, BlockPos center) {
        List<BlockPos> placed = new ArrayList<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();

                BlockState state = level.getBlockState(ground);
                if (!MOLTENABLE.contains(state.getBlock())) {
                    continue;
                }

                level.setBlock(ground, Blocks.LAVA.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.LAVA,
                        ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.0);
                placed.add(ground.immutable());
            }
        }

        return placed;
    }
}