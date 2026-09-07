package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.entities.earth.EarthBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedList;

/**
 * "mudShell" — nova habilidade da árvore de Mud (ver {@link MudElement}),
 * aninhada como filha de {@code mudSpikes}. Levanta um abrigo/cúpula de
 * lama ao redor do próprio caster -- como se ele improvisasse uma caverna
 * na hora -- que fica de pé por {@link #DURATION_TICKS} e depois desmancha
 * sozinha (ver {@link MudShellManager}).
 *
 * Geometricamente é uma semiesfera oca (casca de espessura ~1 bloco,
 * {@link #RADIUS} de raio) centrada nos pés do caster: paredes e teto
 * fechados, chão aberto (o terreno natural embaixo continua livre) -- o
 * jogador fica de pé DENTRO da cúpula, protegido de flechas/mobs por fora,
 * exatamente como estar dentro de uma cavidade de terra.
 *
 * Cálculo da casca: iteramos cada posição de bloco dentro da caixa
 * delimitadora (-RADIUS..RADIUS em X/Z, 0..RADIUS em Y) e testamos se a
 * distância ao centro cai numa faixa fina perto de RADIUS -- é a técnica
 * clássica de "voxelizar" uma esfera testando cada posição discreta (ver
 * {@link #isShellVoxel}), o que garante uma casca CONTÍNUA sem buracos
 * (ao contrário de amostrar pontos na superfície, que pode deixar lacunas).
 *
 * Cada bloco da casca é um {@link EarthBlockEntity} flutuante que nasce no
 * centro (peito do caster) e voa até sua posição final na casca -- visual
 * de "a caverna se fechando ao seu redor" -- nunca vira bloco de verdade
 * no mundo, igual {@code MudWallAbility}/{@code CrystalWallAbility}.
 *
 * Sem controle de dispensar antes do tempo (diferente de
 * {@code AbilityEarthWall}) -- fica de pé pela duração fixa, igual
 * {@code CrystalWallAbility}/{@code PlantVineWallAbility}. Sem limite de
 * abrigos simultâneos por caster -- cada cast só acumula uma entrada nova
 * no manager.
 */
public class MudShellAbility implements Ability {

    private static final int RADIUS = 3;
    private static final double SHELL_THICKNESS = 1.2; // espessura aproximada da casca, em blocos
    private static final int DURATION_TICKS = 20 * 12; // 12s -- abrigo dura mais que uma parede comum
    private static final float FLY_SPEED = 0.3f;
    private static final float CHI_COST = 35.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        BlockPos feet = player.blockPosition();
        Vec3 spawnCenter = player.position().add(0, player.getBbHeight() * 0.5, 0);

        LinkedList<EarthBlockEntity> entities = new LinkedList<>();
        double maxDistSq = (double) RADIUS * RADIUS;
        double minDistSq = Math.pow(RADIUS - SHELL_THICKNESS, 2);

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                for (int y = 0; y <= RADIUS; y++) {
                    if (!isShellVoxel(x, y, z, minDistSq, maxDistSq)) {
                        continue;
                    }

                    BlockPos target = feet.offset(x, y, z);
                    BlockState current = level.getBlockState(target);
                    if (!current.canBeReplaced()) {
                        continue; // já tem alguma coisa sólida ali -- pula só esse bloco da casca
                    }

                    spawnShellBlock(level, player, spawnCenter, target, entities);
                }
            }
        }

        if (!entities.isEmpty()) {
            MudShellManager.registerShell(entities, DURATION_TICKS);
            level.playSound(null, feet, SoundEvents.MUD_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }

        bender.setCurrAbility(null); // instantânea -- ver MudWallAbility/PurifyingWaterAbility pro mesmo padrão
    }

    /** @return true se a posição (x,y,z) relativa ao centro cai na faixa fina que forma a casca da cúpula. */
    private boolean isShellVoxel(int x, int y, int z, double minDistSq, double maxDistSq) {
        double distSq = (double) x * x + (double) y * y + (double) z * z;
        return distSq <= maxDistSq && distSq >= minDistSq;
    }

    private void spawnShellBlock(ServerLevel level, Player player, Vec3 spawnCenter, BlockPos target,
                                 LinkedList<EarthBlockEntity> entities) {
        EarthBlockEntity entity = new EarthBlockEntity(level, player, spawnCenter.x, spawnCenter.y, spawnCenter.z);
        entity.setBlockState(Blocks.MUD.defaultBlockState());
        entity.setTargetPosition(target.getCenter().toVector3f());
        entity.setMovementSpeed(FLY_SPEED);
        entity.setCollidable(true);
        // nunca vira bloco de verdade no mundo -- desmancha quando o abrigo acabar
        entity.setDrops(false);

        level.addFreshEntity(entity);
        entities.add(entity);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}