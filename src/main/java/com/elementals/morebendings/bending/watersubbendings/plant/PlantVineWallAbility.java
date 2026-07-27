package com.elementals.morebendings.bending.watersubbendings.plant;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * "vineWall" — segunda habilidade raiz da árvore de Plant (ver
 * {@link PlantElement}). Levanta uma parede sólida de folhagem (3 de
 * largura x 3 de altura) na frente do caster por um tempo curto -- bloqueia
 * flechas e movimento, igual uma barreira de Earth, só que temporária e
 * feita de folhas em vez de pedra.
 *
 * Instantânea na hora de LEVANTAR (libera currAbility de cara, igual
 * MudSurgeAbility/CrystalShardAbility) -- quem cuida da parte "temporária"
 * (contagem regressiva + reverter os blocos) é {@link PlantVineWallManager},
 * dirigido tick a tick pelo ServerTickEvent registrado em
 * ElementalsMoreBendingsMod (mesmo esquema de MudTrapManager).
 */
public class PlantVineWallAbility implements Ability {

    private static final int DISTANCE_AHEAD = 3;
    private static final int WIDTH = 3;  // colunas (1 pra cada lado do centro + o centro)
    private static final int HEIGHT = 3; // blocos de altura
    private static final int DURATION_TICKS = 20 * 6; // 6s

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

        boolean placedAny = PlantVineWallManager.raise(level, player, columns, HEIGHT, DURATION_TICKS);

        if (placedAny) {
            level.playSound(null, center, SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 1.0f, 0.9f);
        }

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}