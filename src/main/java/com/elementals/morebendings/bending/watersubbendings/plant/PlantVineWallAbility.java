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
 * por perto -- não precisa ser floresta de verdade, só precisa ter algum
 * {@link SituationChecks#PLANT_DERIVED_BLOCKS bloco derivado de planta}
 * (madeira, folha, flor, lavoura, trepadeira etc.) a até
 * {@link #PLANT_PRESENCE_RADIUS} blocos de distância:
 * <ul>
 *     <li><b>Vida vegetal densa por perto</b> ({@link #PLANT_POWER_CAP} ou
 *     mais blocos num raio de {@link #PLANT_POWER_RADIUS}, ex: dentro de uma
 *     floresta de verdade) -- a parede não fica presa a 1 única coluna: ela
 *     se estende por {@link #WIDTH_FOREST} colunas, na altura e duração
 *     cheias.</li>
 *     <li><b>Pouca vida vegetal densa, mas tem algo dentro de
 *     {@link #PLANT_PRESENCE_RADIUS} blocos</b> -- vira 1 coluna só, e a
 *     altura/duração escalam entre um mínimo e o máximo conforme o número de
 *     blocos vegetais no raio mais próximo ({@link #PLANT_POWER_RADIUS}).</li>
 *     <li><b>Nenhum bloco derivado de planta nem a {@link #PLANT_PRESENCE_RADIUS}
 *     blocos de distância</b> -- não tem de onde puxar a energia; a
 *     habilidade falha (nenhuma parede sobe).</li>
 * </ul>
 * A habilidade não é mais limitada a uma única parede ativa por vez -- dá
 * pra levantar várias em sequência, cada uma com sua própria contagem
 * regressiva independente (ver {@link PlantVineWallManager}).
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

    // -- detecção de vida vegetal --
    private static final int PLANT_PRESENCE_RADIUS = 50; // só precisa ter ALGO derivado de planta até aqui pra puxar poder
    private static final int PLANT_POWER_RADIUS = 8;     // raio usado pra medir "quanta" vida vegetal tem bem perto (escala o tamanho)
    private static final int PLANT_POWER_CAP = 40;        // a partir daqui já conta como "denso" (tipo floresta) -- parede larga e no máximo

    // -- vida vegetal densa por perto: parede larga, tamanho/duração cheios --
    private static final int WIDTH_FOREST = 7;   // colunas (3 pra cada lado do centro + o centro)

    // -- escalando com a quantidade de blocos vegetais por perto: sempre 1 coluna só --
    private static final int WIDTH_SINGLE = 1;
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

        int width;
        int height;
        int durationTicks;

        int powerCount = SituationChecks.countNearbyBlocks(
                serverPlayer, PLANT_POWER_RADIUS, SituationChecks.PLANT_DERIVED_BLOCKS);

        if (powerCount >= PLANT_POWER_CAP) {
            // vida vegetal densa bem perto (tipo floresta de verdade) -- parede larga, tamanho/duração cheios
            width = WIDTH_FOREST;
            height = HEIGHT_MAX;
            durationTicks = DURATION_MAX_TICKS;
        } else {
            // não tem densidade por perto -- mas ainda dá pra puxar poder se houver ALGO
            // derivado de planta até PLANT_PRESENCE_RADIUS blocos de distância (não precisa ser floresta)
            boolean plantNearby = powerCount > 0 || SituationChecks.hasNearbyBlock(
                    serverPlayer, PLANT_PRESENCE_RADIUS, SituationChecks.PLANT_DERIVED_BLOCKS);
            if (!plantNearby) {
                // nenhum bloco derivado de planta nem a 50 blocos -- não tem de onde puxar a energia
                bender.setCurrAbility(null);
                return;
            }

            float scale = Math.min(1f, powerCount / (float) PLANT_POWER_CAP);
            width = WIDTH_SINGLE;
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