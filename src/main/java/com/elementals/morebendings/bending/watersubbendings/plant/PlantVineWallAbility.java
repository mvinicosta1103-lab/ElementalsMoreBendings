package com.elementals.morebendings.bending.watersubbendings.plant;

import com.elementals.morebendings.situations.SituationChecks;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.entities.earth.EarthBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * {@link PlantElement}). Levanta uma parede de folhagem na frente do caster
 * por um tempo curto -- bloqueia flechas e movimento, igual uma barreira de
 * Earth, só que temporária e feita de folhas em vez de pedra.
 *
 * O tamanho e a duração da parede escalam com o quanto de vida vegetal tem
 * por perto (mesma lógica de detecção usada pelo {@code SituationsRegistry}
 * pra ensinar Plant Bending):
 * <ul>
 *     <li><b>Floresta</b> (muitas folhas por perto, ver {@link #isForest}) --
 *     a parede não fica presa a 1 única coluna: ela se estende por
 *     {@link #WIDTH_FOREST} colunas, na altura e duração cheias.</li>
 *     <li><b>Fora de floresta, com flor(es) por perto</b> -- vira 1 coluna só,
 *     e a altura/duração escalam entre um mínimo e o máximo conforme o
 *     número de flores num raio pequeno (mais flores = parede maior/mais
 *     duradoura, até {@link #FLOWER_SCALE_CAP} flores).</li>
 *     <li><b>Sem floresta e sem nenhuma flor por perto</b> -- não tem de onde
 *     puxar a energia; a habilidade falha (nenhuma parede sobe).</li>
 * </ul>
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

    // -- detecção de ambiente (mesmos raios/limiar usados pra ensinar Plant Bending) --
    private static final int FOREST_CHECK_RADIUS = 8;
    private static final int FOREST_LEAF_THRESHOLD = 40;
    private static final int FLOWER_CHECK_RADIUS = 6;
    private static final int FLOWER_SCALE_CAP = 6; // a partir daqui, flor sozinha já dá o "máximo" fora de floresta

    // -- floresta: parede larga, tamanho/duração cheios --
    private static final int WIDTH_FOREST = 7;   // colunas (3 pra cada lado do centro + o centro)

    // -- fora de floresta, escalando com flores: sempre 1 coluna só --
    private static final int WIDTH_FLOWER = 1;
    private static final int HEIGHT_MIN = 1;
    private static final int DURATION_MIN_TICKS = 20 * 2; // 2s no mínimo (pouca flor por perto)

    private static final int HEIGHT_MAX = 3;    // blocos de altura (também usado na floresta)
    private static final int DURATION_MAX_TICKS = 20 * 6; // 6s no máximo (floresta ou flores no cap)
    private static final float RISE_SPEED = 0.2f; // mesma velocidade da Earth Wall

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
            bender.setCurrAbility(null);
            return;
        }

        if (PlantVineWallManager.hasActiveWall(player)) {
            bender.setCurrAbility(null);
            return;
        }

        int width;
        int height;
        int durationTicks;

        boolean forest = isForest(serverPlayer);
        if (forest) {
            width = WIDTH_FOREST;
            height = HEIGHT_MAX;
            durationTicks = DURATION_MAX_TICKS;
        } else {
            int flowerCount = SituationChecks.countNearbyBlocks(
                    serverPlayer, FLOWER_CHECK_RADIUS, SituationChecks.FLOWER_BLOCKS);
            if (flowerCount <= 0) {
                // nem floresta, nem flor por perto -- não tem vida vegetal o bastante pra puxar a parede
                bender.setCurrAbility(null);
                return;
            }

            float scale = Math.min(1f, flowerCount / (float) FLOWER_SCALE_CAP);
            width = WIDTH_FLOWER;
            height = Math.round(HEIGHT_MIN + (HEIGHT_MAX - HEIGHT_MIN) * scale);
            durationTicks = Math.round(DURATION_MIN_TICKS + (DURATION_MAX_TICKS - DURATION_MIN_TICKS) * scale);
        }

        Direction facing = player.getDirection();
        Direction side = facing.getClockWise(); // eixo perpendicular -- é a "largura" da parede
        BlockPos center = player.blockPosition().relative(facing, DISTANCE_AHEAD);

        List<BlockPos> columns = new ArrayList<>();
        int half = width / 2;
        for (int w = -half; w <= half; w++) {
            columns.add(center.relative(side, w));
        }

        LinkedList<EarthBlockEntity> entities = new LinkedList<>();
        for (BlockPos column : columns) {
            raiseColumn(level, player, column, height, entities);
        }

        if (!entities.isEmpty()) {
            PlantVineWallManager.registerWall(player, entities, durationTicks);
            level.playSound(null, center, SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f);
        }

        bender.setCurrAbility(null);
    }

    /** Mesmo critério que o {@code SituationsRegistry} usa pra "está numa floresta". */
    private boolean isForest(ServerPlayer player) {
        return SituationChecks.countNearbyBlocks(player, FOREST_CHECK_RADIUS, SituationChecks.LEAF_BLOCKS)
                >= FOREST_LEAF_THRESHOLD;
    }

    /**
     * Sobe uma coluna de {@code height} folhas, uma entidade por altura, todas
     * partindo do nível do chão da coluna e subindo (flutuando, controladas)
     * até a posição final -- igual {@code AbilityEarthWall#placePillar}, só
     * que sem desenterrar bloco nenhum de verdade.
     */
    private void raiseColumn(ServerLevel level, Player player, BlockPos column, int height, LinkedList<EarthBlockEntity> entities) {
        for (int h = 0; h < height; h++) {
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