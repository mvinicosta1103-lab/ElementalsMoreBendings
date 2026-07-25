package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * "crystalShard" — habilidade raiz da árvore de Crystal. Dispara uma
 * barragem de estilhaços de cristal na direção que o jogador está olhando.
 *
 * Antes isso era um hitscan instantâneo (raycast na hora + partículas, sem
 * entidade nenhuma — dano garantido pra quem estivesse na linha). Agora
 * cada estilhaço é uma {@link CrystalShardEntity} de verdade: sai voando,
 * tem hitbox própria, e pode simplesmente errar o alvo — mais parecido
 * com uma saraivada de flechas de cristal do que um laser.
 */
public class CrystalShardAbility implements Ability {

    private static final int SHARD_COUNT = 6;
    private static final float SPEED = 2.2f;
    /** Espalhamento em graus — quanto maior, mais "aberta" fica a barragem. */
    private static final float DIVERGENCE = 4.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        for (int i = 0; i < SHARD_COUNT; i++) {
            CrystalShardEntity shard = new CrystalShardEntity(level, player);
            // setDeltaMovement(shooter, pitch, yaw, roll, speed, divergence) já vem
            // pronto no AbstractElementalsEntity do mod base — calcula a direção a
            // partir da mira do jogador, aplica um espalhamento aleatório
            // (divergence) e herda a velocidade do jogador. Perfeito pra uma
            // barragem: cada estilhaço sai com um desvio levemente diferente.
            shard.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, SPEED, DIVERGENCE);
            level.addFreshEntity(shard);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.4f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}