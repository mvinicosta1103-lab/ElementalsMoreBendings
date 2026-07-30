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
 * "magmaSpike" — terceira habilidade da árvore de Lava (ver {@link
 * LavaElement}), uma AoE ofensiva de curto alcance: raycast na direção
 * mirada (mesmo {@link SapsUtils#raycastFull} que {@link LavaPoolAbility}
 * usa), e no ponto de impacto faz brotar um punhado de espinhos de {@link
 * Blocks#MAGMA_BLOCK} do chão ao redor -- qualquer entidade viva pega no
 * meio leva dano e é arremessada pra cima, igual uma erupção de verdade.
 *
 * Diferente de lavaPool (que fica lá até esfriar sozinho), os espinhos
 * aqui são só uma explosão pontual: erguem, causam o dano/knockback na
 * hora, e desmancham de volta pro bloco original pouco depois -- ver
 * {@link MagmaSpikeManager}, mesmo esquema (manager + registro no
 * NeoForge.EVENT_BUS) que {@link LavaPoolManager} já usa, só que
 * revertendo pro estado ANTERIOR em vez de esfriar pra basalto.
 */
public class MagmaSpikeAbility implements Ability {

    private static final double RANGE = 8.0;
    /** Raio da área de espinhos (blocos ao redor do centro do impacto). */
    private static final int RADIUS = 2;
    private static final float CHI_COST = 30.0f;
    private static final float DAMAGE = 4.0f;
    private static final double LAUNCH_UP = 0.7;
    /** Quanto tempo (em ticks de servidor) os espinhos ficam de pé antes de desmanchar. 20 ticks = 1s. */
    static final int RETRACT_AFTER_TICKS = 30; // 1.5s

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
            damageAndLaunch(level, player, center);
            MagmaSpikeManager.registerSpikes(level, spikes, original);
            level.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.3f);
        }

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    /**
     * Substitui o chão sólido ao redor do centro por magma_block. Retorna
     * as posições alteradas; o {@link BlockState} de cada uma é salvo em
     * {@code original} ANTES da troca, pra {@link MagmaSpikeManager}
     * conseguir devolver o terreno exatamente como estava depois.
     */
    private List<BlockPos> raiseSpikes(ServerLevel level, BlockPos center, Map<BlockPos, BlockState> original) {
        List<BlockPos> placed = new ArrayList<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                // Só os blocos dentro do círculo (não o quadrado inteiro) --
                // dá uma forma mais de "cluster de espinhos" do que uma
                // plataforma quadrada lisa.
                if (dx * dx + dz * dz > RADIUS * RADIUS) {
                    continue;
                }

                BlockPos column = center.offset(dx, 0, dz);
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();

                BlockState existing = level.getBlockState(ground);
                if (!existing.isSolid() || existing.is(Blocks.MAGMA_BLOCK)) {
                    continue;
                }

                level.sendParticles(ParticleTypes.LAVA,
                        ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.0);
                original.put(ground.immutable(), existing);
                level.setBlock(ground, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                placed.add(ground.immutable());
            }
        }

        return placed;
    }

    /** Dano + arremesso pra cima em quem estiver na área no momento da erupção. */
    private void damageAndLaunch(ServerLevel level, Player caster, BlockPos center) {
        AABB area = new AABB(center).inflate(RADIUS + 0.5, 1.5, RADIUS + 0.5);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity entity : hit) {
            entity.hurt(level.damageSources().lava(), DAMAGE);
            entity.push(0, LAUNCH_UP, 0);
            entity.hurtMarked = true; // garante que o cliente sincronize o impulso vertical
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
