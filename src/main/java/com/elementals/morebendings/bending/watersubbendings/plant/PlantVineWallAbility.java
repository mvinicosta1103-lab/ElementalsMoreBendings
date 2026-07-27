package com.elementals.morebendings.bending.watersubbendings.plant;

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
 * "vineWall" — segunda habilidade raiz da árvore de Plant (ver
 * {@link PlantElement}). Levanta uma parede sólida de folhagem (3 de
 * largura x 3 de altura) na frente do caster por um tempo curto -- bloqueia
 * flechas e movimento, igual uma barreira de Earth, só que temporária e
 * feita de folhas em vez de pedra.
 *
 * A animação de subida é a mesma da {@link dev.saperate.elementals.elements.earth.AbilityEarthWall}
 * do mod base: cada bloco da parede é um {@link EarthBlockEntity} flutuante
 * (a mesma entidade que a Earth Wall usa) subindo até sua posição final --
 * só trocamos o {@code BlockState} carregado por ela pra
 * {@link Blocks#OAK_LEAVES}, já que o renderer dela desenha qualquer bloco
 * genericamente. Como as folhas nunca existiram no mundo de verdade (não tem
 * o que "desenterrar"), cada entidade nasce rente ao chão da própria coluna
 * e sobe até sua altura -- ao contrário da Earth Wall, que desenterra blocos
 * reais de um buraco abaixo do jogador.
 *
 * Quem cuida da parte "temporária" (contagem regressiva + desmanchar as
 * entidades) é {@link PlantVineWallManager}, dirigido tick a tick pelo
 * ServerTickEvent registrado em ElementalsMoreBendingsMod (mesmo esquema de
 * MudTrapManager).
 */
public class PlantVineWallAbility implements Ability {

    private static final int DISTANCE_AHEAD = 3;
    private static final int WIDTH = 3;  // colunas (1 pra cada lado do centro + o centro)
    private static final int HEIGHT = 3; // blocos de altura
    private static final int DURATION_TICKS = 20 * 6; // 6s
    private static final float RISE_SPEED = 0.2f; // mesma velocidade da Earth Wall

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (PlantVineWallManager.hasActiveWall(player)) {
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
            PlantVineWallManager.registerWall(player, entities, DURATION_TICKS);
            level.playSound(null, center, SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f);
        }

        bender.setCurrAbility(null);
    }

    /**
     * Sobe uma coluna de {@code height} folhas, uma entidade por altura, todas
     * partindo do nível do chão da coluna e subindo (flutuando, controladas)
     * até a posição final -- igual {@code AbilityEarthWall#placePillar}, só
     * que sem desenterrar bloco nenhum de verdade.
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
            entity.setBlockState(Blocks.OAK_LEAVES.defaultBlockState());
            entity.setTargetPosition(target.getCenter().toVector3f());
            entity.setMovementSpeed(RISE_SPEED);
            entity.setCollidable(true);
            // nunca vira bloco de verdade no mundo -- some com um "poof" quando a parede acabar
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