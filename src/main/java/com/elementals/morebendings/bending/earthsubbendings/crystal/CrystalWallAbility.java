package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.entities.earth.EarthBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * "crystalWall" — terceira habilidade raiz da árvore de Crystal (ver
 * {@link CrystalElement}). Levanta uma parede sólida de cristal (3 de
 * largura x 3 de altura) na frente do caster por um tempo curto -- bloqueia
 * flechas e movimento, igual a {@code AbilityEarthWall} do mod base, só que
 * temporária e feita de ametista.
 *
 * Mesmo truque visual de {@link com.elementals.morebendings.bending.watersubbendings.plant.PlantVineWallAbility}:
 * cada bloco da parede é um {@link EarthBlockEntity} flutuante subindo até
 * sua posição final, sem nunca virar bloco de verdade no mundo -- por isso
 * cada entidade nasce rente ao chão da própria coluna e sobe até sua altura,
 * ao contrário da Earth Wall (que desenterra blocos reais).
 *
 * Quem cuida da parte "temporária" (contagem regressiva + estilhaçar as
 * entidades) é {@link CrystalWallManager}, dirigido tick a tick pelo
 * ServerTickEvent registrado em ElementalsMoreBendingsMod. Sem limite de
 * paredes simultâneas -- cada cast sobe a sua própria, independente das
 * que já estão de pé (ver {@link CrystalWallManager}).
 */
public class CrystalWallAbility implements Ability {

    private static final int DISTANCE_AHEAD = 3;
    private static final int WIDTH = 3;  // colunas (1 pra cada lado do centro + o centro)
    private static final int HEIGHT = 3; // blocos de altura
    private static final int DURATION_TICKS = 20 * 8; // 8s -- cristal aguenta mais que folhagem
    private static final float RISE_SPEED = 0.25f;
    private static final float CHI_COST = 30.0f;

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

        Direction facing = player.getDirection();
        Direction side = facing.getClockWise(); // eixo perpendicular -- é a "largura" da parede
        BlockPos center = player.blockPosition().relative(facing, DISTANCE_AHEAD);

        List<BlockPos> columns = new ArrayList<>();
        int half = WIDTH / 2;
        for (int w = -half; w <= half; w++) {
            columns.add(center.relative(side, w));
        }

        LinkedList<EarthBlockEntity> entities = new LinkedList<>();
        for (BlockPos column : columns) {
            raiseColumn(level, player, column, entities);
        }

        if (!entities.isEmpty()) {
            CrystalWallManager.registerWall(player, entities, DURATION_TICKS);
            level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f);
            level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.1f);
        }

        bender.setCurrAbility(null);
    }

    /**
     * Sobe uma coluna de {@code HEIGHT} blocos de cristal, uma entidade por
     * altura, todas partindo do nível do chão da coluna e subindo (flutuando,
     * controladas) até a posição final -- igual {@code
     * PlantVineWallAbility#raiseColumn} / {@code AbilityEarthWall#placePillar}.
     */
    private void raiseColumn(ServerLevel level, Player player, BlockPos column, LinkedList<EarthBlockEntity> entities) {
        for (int h = 0; h < HEIGHT; h++) {
            BlockPos target = column.above(h);
            BlockState current = level.getBlockState(target);
            if (!current.canBeReplaced()) {
                continue; // essa altura da coluna está bloqueada -- pula só ela, não a coluna inteira
            }

            EarthBlockEntity entity = new EarthBlockEntity(level, player,
                    column.getX() + 0.5, column.getY(), column.getZ() + 0.5);
            entity.setBlockState(Blocks.AMETHYST_BLOCK.defaultBlockState());
            entity.setTargetPosition(target.getCenter().toVector3f());
            entity.setMovementSpeed(RISE_SPEED);
            entity.setCollidable(true);
            // nunca vira bloco de verdade no mundo -- estilhaça quando a parede acabar
            entity.setDrops(false);

            level.addFreshEntity(entity);
            entities.add(entity);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}