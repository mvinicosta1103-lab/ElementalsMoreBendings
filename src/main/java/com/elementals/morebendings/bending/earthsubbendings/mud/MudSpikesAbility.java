package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "mudSpikes" — quarta habilidade da árvore de Mud (ver {@link MudElement}),
 * uma AoE ofensiva de curto alcance. Mesmo esquema de {@code
 * MagmaSpikeAbility}: raycast na direção mirada, e no ponto de impacto um
 * punhado de blocos ao redor vira {@link Blocks#MUD}, cada um com um
 * cluster de farpas ({@link MudSpikeVisualEntity}) brotando por cima --
 * qualquer entidade viva pega no meio leva dano e um leve arremesso pra
 * cima. Diferente de magmaSpike, não incendeia nem deixa piso perigoso
 * depois -- é puramente físico/cinético, o "susto" vem do tanto de lama
 * que brota de uma vez, não de dano contínuo.
 *
 * Os blocos revertem ao original depois de {@link #RETRACT_AFTER_TICKS} --
 * ver {@link MudSpikeManager}, registrado no NeoForge.EVENT_BUS em
 * {@code ElementalsMoreBendingsMod}.
 */
public class MudSpikesAbility implements Ability {

    private static final double RANGE = 8.0;
    /** Raio da área de espinhos (blocos ao redor do centro do impacto). */
    private static final int RADIUS = 2;
    private static final float CHI_COST = 25.0f;
    private static final float DAMAGE = 3.5f;
    private static final double LAUNCH_UP = 0.5;
    /** Quanto tempo (em ticks de servidor) os espinhos ficam de pé antes de desmanchar. 20 ticks = 1s. */
    static final int RETRACT_AFTER_TICKS = 40; // 2s

    /** Mesmo conjunto de "chão natural" que {@code MudTrapAbility} usa pra virar lama. */
    private static final java.util.Set<Block> MUDDABLE = java.util.Set.of(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.MYCELIUM,
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.CLAY
    );

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 hitPos = hit.getLocation();
        BlockPos hitColumn = BlockPos.containing(hitPos.x, hitPos.y, hitPos.z);
        BlockPos center = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hitColumn).below();

        Map<BlockPos, BlockState> original = new HashMap<>();
        List<BlockPos> spikes = raiseSpikes(level, center, original);

        if (!spikes.isEmpty()) {
            damageAndLaunch(level, caster, center);
            MudSpikeManager.registerSpikes(level, spikes, original);
            level.playSound(null, center, SoundEvents.MUD_PLACE, SoundSource.PLAYERS, 0.8f, 0.9f);
        }

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    /**
     * Substitui o chão "muddable" ao redor do centro por {@code Blocks.MUD}
     * e brota um cluster de farpas visuais em cima de cada posição. Retorna
     * as posições alteradas; o {@link BlockState} de cada uma é salvo em
     * {@code original} ANTES da troca, pra {@link MudSpikeManager}
     * conseguir devolver o terreno exatamente como estava depois.
     */
    private List<BlockPos> raiseSpikes(ServerLevel level, BlockPos center, Map<BlockPos, BlockState> original) {
        List<BlockPos> placed = new ArrayList<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                // Só os blocos dentro do círculo (não o quadrado inteiro) -- dá
                // uma forma mais de "cluster orgânico" do que uma plataforma lisa.
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }

                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();

                BlockState existing = level.getBlockState(ground);
                if (!MUDDABLE.contains(existing.getBlock())) {
                    continue;
                }

                level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.MUD.defaultBlockState()),
                        ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 6, 0.2, 0.15, 0.2, 0.0);
                original.put(ground.immutable(), existing);
                level.setBlock(ground, Blocks.MUD.defaultBlockState(), 3);
                // Cluster de farpas de verdade (ver MudSpikeVisualEntity), sincronizado
                // pra sumir exatamente quando MudSpikeManager reverte o bloco.
                MudSpikeVisualEntity.spawn(level, ground, RETRACT_AFTER_TICKS);
                placed.add(ground.immutable());
            }
        }

        return placed;
    }

    /** Dano + leve arremesso pra cima em quem estiver na área no momento da erupção. */
    private void damageAndLaunch(ServerLevel level, Player caster, BlockPos center) {
        AABB area = new AABB(center).inflate(RADIUS + 0.5, 1.5, RADIUS + 0.5);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity entity : hit) {
            entity.hurt(level.damageSources().playerAttack(caster), DAMAGE);
            entity.push(0, LAUNCH_UP, 0);
            entity.hurtMarked = true; // garante que o cliente sincronize o impulso vertical
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}