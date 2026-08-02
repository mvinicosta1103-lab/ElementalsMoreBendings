package com.elementals.morebendings.bending.earthsubbendings.crystal;

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
 * "crystalSpike" — segunda habilidade raiz da árvore de Crystal (ver
 * {@link CrystalElement}), uma AoE ofensiva de curto alcance. Mesmo esquema
 * de {@code MudSpikesAbility}: raycast na direção mirada, e no ponto de
 * impacto um punhado de blocos "de pedra" ao redor vira
 * {@link Blocks#AMETHYST_BLOCK} de repente, arremessando quem estiver em
 * cima. Diferente da lama (que é puramente físico), o cristal também deixa
 * as farpas mais "afiadas" -- dano um pouco maior, já que aqui não tem
 * susto de volume, e sim de estilhaço.
 *
 * Os blocos revertem ao original depois de {@link #RETRACT_AFTER_TICKS} --
 * ver {@link CrystalSpikeManager}, registrado no NeoForge.EVENT_BUS em
 * {@code ElementalsMoreBendingsMod}.
 */
public class CrystalSpikeAbility implements Ability {

    private static final double RANGE = 8.0;
    /** Raio da área de espinhos (blocos ao redor do centro do impacto). */
    private static final int RADIUS = 2;
    private static final float CHI_COST = 25.0f;
    private static final float DAMAGE = 4.0f;
    private static final double LAUNCH_UP = 0.55;
    /** Quanto tempo (em ticks de servidor) os espinhos ficam de pé antes de desmanchar. 20 ticks = 1s. */
    static final int RETRACT_AFTER_TICKS = 60; // 3s

    /** Chão "rochoso" que pode ser cristalizado -- pedra e variantes, não terra/areia. */
    private static final java.util.Set<Block> CRYSTALLIZABLE = java.util.Set.of(
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.DEEPSLATE,
            Blocks.COBBLED_DEEPSLATE, Blocks.TUFF, Blocks.CALCITE,
            Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.BLACKSTONE
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
            CrystalSpikeManager.registerSpikes(level, spikes, original);
            level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 0.9f, 0.8f);
            level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.3f);
        }

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    /**
     * Substitui o chão "cristalizável" ao redor do centro por {@code
     * Blocks.AMETHYST_BLOCK}. Retorna as posições alteradas; o
     * {@link BlockState} de cada uma é salvo em {@code original} ANTES da
     * troca, pra {@link CrystalSpikeManager} conseguir devolver o terreno
     * exatamente como estava depois.
     */
    private List<BlockPos> raiseSpikes(ServerLevel level, BlockPos center, Map<BlockPos, BlockState> original) {
        List<BlockPos> placed = new ArrayList<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                // Só os blocos dentro do círculo (não o quadrado inteiro) -- dá
                // uma forma mais de "cluster de geodo" do que uma plataforma lisa.
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }

                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();

                BlockState existing = level.getBlockState(ground);
                if (!CRYSTALLIZABLE.contains(existing.getBlock())) {
                    continue;
                }

                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                        ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.0);
                original.put(ground.immutable(), existing);
                level.setBlock(ground, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
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