package com.elementals.morebendings.bending.watersubbendings.plant;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * "thornVolley" — habilidade raiz da árvore de Plant (grátis, junto com
 * vineWhip/vineWall). Dispara uma saraivada de espinhos na direção que o
 * jogador está olhando -- mesmo esquema de {@code CrystalShardAbility}:
 * cada espinho é uma {@link PlantThornVolleyEntity} de verdade, com
 * espalhamento aleatório (divergence), em vez de um hitscan instantâneo.
 */
public class PlantThornVolleyAbility implements Ability {

    private static final int THORN_COUNT = 5;
    private static final float SPEED = 2.0f;
    /** Espalhamento em graus — quanto maior, mais "aberta" fica a barragem. */
    private static final float DIVERGENCE = 5.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        for (int i = 0; i < THORN_COUNT; i++) {
            PlantThornVolleyEntity thorn = new PlantThornVolleyEntity(level, player);
            thorn.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, SPEED, DIVERGENCE);
            level.addFreshEntity(thorn);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.PLAYERS, 0.6f, 1.3f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}