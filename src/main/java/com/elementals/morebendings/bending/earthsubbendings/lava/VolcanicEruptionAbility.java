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
import java.util.Set;
import java.util.UUID;

/**
 * "volcanicEruption" — sexta habilidade da árvore de Lava (ver {@link
 * LavaElement}), a "ultimate": versão bem maior de {@link
 * MagmaSpikeAbility}. Raycast na direção mirada (mesmo {@link
 * SapsUtils#raycastFull} que {@link LavaPoolAbility}/{@link
 * MagmaSpikeAbility} usam) e, no ponto de impacto:
 *
 *  - Um núcleo pequeno ({@link #CORE_RADIUS}) vira {@link Blocks#LAVA} de
 *    verdade, igual {@link LavaPoolAbility} -- esfria pra {@link
 *    Blocks#BASALT} depois de um tempo bem maior (é uma cratera, não uma
 *    poça pequena).
 *  - Um anel ao redor ({@link #SPIKE_RADIUS}, maior que o de {@link
 *    MagmaSpikeAbility}) vira espinhos de {@link Blocks#MAGMA_BLOCK} que
 *    desmancham de volta pro bloco original depois, igual MagmaSpike.
 *  - Todo mundo na área (menos o próprio caster) toma dano, é arremessado
 *    pra cima/pra fora e pega fogo.
 *
 * Diferente das outras habilidades de Lava (todas instantâneas e sem
 * cooldown próprio, só limitadas pelo chi), esta tem um cooldown de
 * verdade -- mesmo esquema de {@code UpdraftAbility}/{@code
 * CombustionExplosionAbility} -- por ser bem mais destrutiva/cara.
 *
 * O revert de ambos os grupos (núcleo + espinhos) é dirigido por {@link
 * VolcanicEruptionManager}, registrado no NeoForge.EVENT_BUS igual {@link
 * LavaPoolManager}/{@link MagmaSpikeManager}.
 */
public class VolcanicEruptionAbility implements Ability {

    private static final double RANGE = 10.0;
    /** Raio (em blocos) do núcleo de lava de verdade no centro do impacto. */
    private static final int CORE_RADIUS = 1;
    /** Raio do anel de espinhos de magma ao redor do núcleo. Maior que o de {@link MagmaSpikeAbility}. */
    private static final int SPIKE_RADIUS = 3;

    private static final float CHI_COST = 55.0f;
    private static final float DAMAGE = 7.0f;
    private static final int FIRE_SECONDS = 5;
    private static final double LAUNCH_UP = 0.9;

    private static final int BASE_COOLDOWN_TICKS = 600; // 30s
    private static final float PROXIMITY_PAD = 1.5f;

    /** Quanto tempo o núcleo de lava fica líquido antes de esfriar pra basalto. */
    static final int CORE_COOL_AFTER_TICKS = 300; // 15s
    /** Quanto tempo os espinhos de magma ao redor ficam de pé antes de desmanchar. */
    static final int SPIKE_RETRACT_AFTER_TICKS = 60; // 3s

    /** Mesmo conjunto de blocos "moltenáveis" que {@link LavaPoolAbility} usa pro núcleo. */
    private static final Set<Block> MOLTENABLE = Set.of(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.MUD,
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.CLAY,
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE
    );

    /** Cooldown por jogador. Fica em memória só -- não precisa persistir entre logins. */
    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -1_000_000L);
        if (now - last < BASE_COOLDOWN_TICKS) {
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

        List<BlockPos> corePositions = raiseCore(level, center);
        Map<BlockPos, BlockState> spikeOriginals = new HashMap<>();
        List<BlockPos> spikePositions = raiseSpikes(level, center, corePositions, spikeOriginals);

        if (!corePositions.isEmpty() || !spikePositions.isEmpty()) {
            damageAndLaunch(level, caster, center);
            if (!corePositions.isEmpty()) {
                VolcanicEruptionManager.registerCore(level, corePositions);
            }
            if (!spikePositions.isEmpty()) {
                VolcanicEruptionManager.registerSpikes(level, spikePositions, spikeOriginals);
            }
            level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.7f);
        }

        lastUse.put(caster.getUUID(), now);
        bender.setCurrAbility(null); // libera a trava pra poder usar de novo (assim que o cooldown permitir)
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    /** Converte o núcleo (centro) em lava de verdade, igual {@link LavaPoolAbility#createPool}. */
    private List<BlockPos> raiseCore(ServerLevel level, BlockPos center) {
        List<BlockPos> placed = new ArrayList<>();

        for (int dx = -CORE_RADIUS; dx <= CORE_RADIUS; dx++) {
            for (int dz = -CORE_RADIUS; dz <= CORE_RADIUS; dz++) {
                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();

                BlockState state = level.getBlockState(ground);
                if (!MOLTENABLE.contains(state.getBlock())) {
                    continue;
                }

                level.setBlock(ground, Blocks.LAVA.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.LAVA,
                        ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 10, 0.25, 0.15, 0.25, 0.0);
                placed.add(ground.immutable());
            }
        }

        return placed;
    }

    /** Anel de espinhos de magma ao redor do núcleo, igual {@link MagmaSpikeAbility#raiseSpikes} mas com raio maior. */
    private List<BlockPos> raiseSpikes(ServerLevel level, BlockPos center, List<BlockPos> core, Map<BlockPos, BlockState> original) {
        List<BlockPos> placed = new ArrayList<>();

        for (int dx = -SPIKE_RADIUS; dx <= SPIKE_RADIUS; dx++) {
            for (int dz = -SPIKE_RADIUS; dz <= SPIKE_RADIUS; dz++) {
                if (dx * dx + dz * dz > SPIKE_RADIUS * SPIKE_RADIUS) {
                    continue;
                }

                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();

                if (core.contains(ground)) {
                    continue; // já virou núcleo de lava, não sobrescreve com espinho
                }

                BlockState existing = level.getBlockState(ground);
                if (!existing.isSolid() || existing.is(Blocks.MAGMA_BLOCK)) {
                    continue;
                }

                level.sendParticles(ParticleTypes.LAVA,
                        ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.0);
                original.put(ground.immutable(), existing);
                level.setBlock(ground, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                // Mesmo espinho 3D de verdade que MagmaSpikeAbility usa agora (ver
                // MagmaSpikeVisualEntity) -- reaproveitado aqui pro anel da erupção.
                MagmaSpikeVisualEntity.spawn(level, ground, SPIKE_RETRACT_AFTER_TICKS);
                placed.add(ground.immutable());
            }
        }

        return placed;
    }

    /** Dano + arremesso + ignição pra quem estiver na área no momento da erupção. */
    private void damageAndLaunch(ServerLevel level, Player caster, BlockPos center) {
        AABB area = new AABB(center).inflate(SPIKE_RADIUS + PROXIMITY_PAD, 2.0, SPIKE_RADIUS + PROXIMITY_PAD);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity entity : hit) {
            entity.hurt(level.damageSources().lava(), DAMAGE);
            entity.igniteForSeconds(FIRE_SECONDS);
            entity.push(0, LAUNCH_UP, 0);
            entity.hurtMarked = true; // garante que o cliente sincronize o impulso vertical
        }
    }
}