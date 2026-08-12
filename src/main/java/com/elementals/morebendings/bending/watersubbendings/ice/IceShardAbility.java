package com.elementals.morebendings.bending.watersubbendings.ice;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * "iceShard" — segunda habilidade raiz da árvore de Ice (ver {@link
 * IceElement}). Dispara uma barragem de estilhaços de gelo na direção que
 * o jogador está olhando -- mesmo esquema exato de {@code
 * CrystalShardAbility}: cada estilhaço é uma {@link IceShardEntity} de
 * verdade, com hitbox própria, que pode simplesmente errar o alvo.
 *
 * Com {@code iceMastery} comprado (ver {@link IceElement#hasMastery}), a
 * barragem sai maior -- {@link #MASTERY_BONUS_SHARDS} estilhaços extras.
 */
public class IceShardAbility implements Ability {

    private static final int SHARD_COUNT = 5;
    private static final int MASTERY_BONUS_SHARDS = 3;
    private static final float SPEED = 2.4f;
    /** Espalhamento em graus — quanto maior, mais "aberta" fica a barragem. */
    private static final float DIVERGENCE = 4.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        int shardCount = SHARD_COUNT + (IceElement.hasMastery(bender) ? MASTERY_BONUS_SHARDS : 0);
        for (int i = 0; i < shardCount; i++) {
            IceShardEntity shard = new IceShardEntity(level, player);
            // setDeltaMovement(shooter, pitch, yaw, roll, speed, divergence) já vem
            // pronto no AbstractElementalsEntity do mod base -- calcula a direção a
            // partir da mira do jogador, aplica um espalhamento aleatório
            // (divergence) e herda a velocidade do jogador.
            shard.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, SPEED, DIVERGENCE);
            level.addFreshEntity(shard);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5f, 1.6f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}