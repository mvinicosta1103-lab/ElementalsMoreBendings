package com.elementals.morebendings.bending.earthsubbendings.glass;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * "glassShards" — habilidade raiz (e única, por enquanto) da árvore de
 * Glass. Dispara um estilhaço de vidro na mira do jogador.
 *
 * Antes isso era um hitscan instantâneo (raycast na hora, sem entidade
 * própria) -- igual ao que Crystal usava antes de ganhar a
 * {@code CrystalShardEntity} de verdade (ver comentário histórico em
 * {@link com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardAbility}).
 * Agora usa uma {@link GlassShardEntity} de verdade: sai voando, tem
 * hitbox própria e pode simplesmente errar o alvo, no mesmo esquema de
 * CrystalShardEntity/BoneSpikeEntity.
 */
public class GlassShardsAbility implements Ability {

    private static final float BASE_SPEED = 2.2f;
    /** Espalhamento em graus -- bem pequeno, já que é um único estilhaço mirado. */
    private static final float DIVERGENCE = 1.0f;

    /** Bônus de dano por nível -- ver {@link GlassElement#GLASS_SHARDS_DAMAGE_I}/{@code _II}. */
    private static final float DAMAGE_PER_LEVEL = 1.0f;
    /** Bônus de velocidade do nível único -- ver {@link GlassElement#GLASS_SHARDS_SPEED_I}. */
    private static final float SPEED_BONUS = 0.8f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        float damage = GlassShardEntity.BASE_DAMAGE;
        float speed = BASE_SPEED;
        if (player instanceof ServerPlayer serverPlayer) {
            damage = getDamage(serverPlayer);
            speed = getSpeed(serverPlayer);
        }

        GlassShardEntity shard = new GlassShardEntity(level, player);
        shard.setDamage(damage);
        // setDeltaMovement(shooter, pitch, yaw, roll, speed, divergence) já vem pronto
        // no AbstractElementalsEntity do mod base -- calcula a direção a partir da
        // mira do jogador e aplica a velocidade/espalhamento.
        shard.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, speed, DIVERGENCE);
        level.addFreshEntity(shard);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 1.2f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static float getDamage(ServerPlayer player) {
        float damage = GlassShardEntity.BASE_DAMAGE;
        if (GlassElement.hasUpgrade(player, GlassElement.GLASS_SHARDS_DAMAGE_I)) damage += DAMAGE_PER_LEVEL;
        if (GlassElement.hasUpgrade(player, GlassElement.GLASS_SHARDS_DAMAGE_II)) damage += DAMAGE_PER_LEVEL;
        return damage;
    }

    public static float getSpeed(ServerPlayer player) {
        float speed = BASE_SPEED;
        if (GlassElement.hasUpgrade(player, GlassElement.GLASS_SHARDS_SPEED_I)) speed += SPEED_BONUS;
        return speed;
    }
}