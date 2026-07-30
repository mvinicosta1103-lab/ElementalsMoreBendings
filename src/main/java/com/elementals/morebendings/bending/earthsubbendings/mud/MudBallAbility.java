package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * "mudBall" — terceira habilidade raiz da árvore de Mud (ver {@link
 * MudElement}). Arremessa uma única {@link MudBallEntity} reta na direção
 * mirada -- diferente de {@code CrystalShardAbility} (barragem de 6
 * estilhaços), aqui é um tiro só, mais pesado e lento, que atordoa (Lentidão)
 * em vez de cortar.
 * <p>
 * Instantânea (sem {@link #onTick} / sem estado próprio): por isso,
 * igual a {@code CrystalShardAbility}/{@code MudSurgeAbility}, é
 * OBRIGATÓRIO chamar {@code bender.setCurrAbility(null)} no final do
 * {@link #onCall} (e também em {@link #onRemove}), senão o bender fica
 * travado nesta ability pra sempre.
 */
public class MudBallAbility implements Ability {

    private static final float SPEED = 1.6f;
    /** Pequeno desvio aleatório -- não é hitscan perfeito, mas também não é uma barragem espalhada. */
    private static final float DIVERGENCE = 1.5f;
    private static final float CHI_COST = 15.0f;

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

        MudBallEntity ball = new MudBallEntity(level, player);
        // setDeltaMovement(shooter, pitch, yaw, roll, speed, divergence) já vem
        // pronto no AbstractElementalsEntity do mod base -- calcula a direção a
        // partir da mira do jogador e aplica um pequeno espalhamento aleatório.
        ball.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, SPEED, DIVERGENCE);
        level.addFreshEntity(ball);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.MUD_PLACE, SoundSource.PLAYERS, 0.7f, 0.8f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}