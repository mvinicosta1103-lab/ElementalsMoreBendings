package com.elementals.morebendings.bending.earthsubbendings.glass;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * "glassSpray" — segunda habilidade raiz da árvore de Glass (ver
 * {@link GlassElement}), irmã de {@code glassShards}. Em vez de um único
 * estilhaço certeiro, estilhaça um punhado de vidro na direção mirada de
 * uma vez só -- um leque de {@link GlassShardEntity} espalhadas, cada uma
 * mais fraca que o estilhaço solo, pra cobrir uma área em vez de mirar
 * fundo num único alvo. Reaproveita a mesma entidade de projétil de
 * {@code glassShards}, só que disparada várias vezes com divergência bem
 * maior -- sem precisar de nenhuma entidade/renderer novo.
 *
 * Instantânea, igual {@code glassShards}/{@code sandBlast}: libera
 * {@code currAbility} de volta pra {@code null} no final de {@link #onCall}
 * e em {@link #onRemove}.
 */
public class GlassSprayAbility implements Ability {

    /** Quantidade base de estilhaços por disparo -- sobe com {@link GlassElement#GLASS_SPRAY_WIDE_I}. */
    private static final int BASE_SHARD_COUNT = 3;
    private static final int WIDE_BONUS_SHARDS = 2;

    /** Espalhamento em graus -- bem mais aberto que o estilhaço solo de glassShards. */
    private static final float BASE_DIVERGENCE = 14.0f;
    private static final float WIDE_BONUS_DIVERGENCE = 6.0f;

    private static final float SHARD_DAMAGE = 1.5f;
    private static final float SHARD_SPEED = 2.0f;
    private static final float CHI_COST = 22.0f;

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

        int shardCount = BASE_SHARD_COUNT;
        float divergence = BASE_DIVERGENCE;
        if (player instanceof ServerPlayer serverPlayer && GlassElement.hasUpgrade(serverPlayer, GlassElement.GLASS_SPRAY_WIDE_I)) {
            shardCount += WIDE_BONUS_SHARDS;
            divergence += WIDE_BONUS_DIVERGENCE;
        }

        for (int i = 0; i < shardCount; i++) {
            GlassShardEntity shard = new GlassShardEntity(level, player);
            shard.setDamage(SHARD_DAMAGE);
            // Cada estilhaço rola sua própria divergência aleatória dentro do
            // cone -- setDeltaMovement já cuida do espalhamento aleatório em
            // torno da mira, então bastam N chamadas independentes.
            shard.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, SHARD_SPEED, divergence);
            level.addFreshEntity(shard);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7f, 0.9f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}