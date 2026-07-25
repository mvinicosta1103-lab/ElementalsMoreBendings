package com.elementals.morebendings.bending.earthsubbendings.bone;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

/**
 * "boneControl" -- única habilidade de {@link BoneElement}. Conjura uma
 * {@link BoneSpikeEntity} flutuando à frente do jogador e deixa ele guiar
 * livremente pra cima/baixo e pros lados (seguindo a mira, igual
 * {@code AbilityBloodControl} do mod base faz com a vítima) e pra
 * perto/longe (roda do mouse, também copiado do Blood Control) antes de
 * arremessar.
 *
 * Fluxo:
 *  1. {@link #onCall} -- tecla solta: conjura a farpa e trava a ability
 *     como {@code currAbility} (fica "canalizada", não solta na hora).
 *  2. {@link #onTick} -- todo tick, empurra a farpa em direção ao ponto que
 *     o jogador está mirando, à distância atual (ver {@link #onMiddleClick}
 *     pra mudar a distância). É a mira do jogador (olhar pra cima/baixo,
 *     pros lados) que move a farpa nos 3 eixos -- não precisamos ler
 *     tecla de movimento nenhuma, o mesmo truque que Blood Control usa.
 *  3. {@link #onLeftClick} -- arremessa a farpa na direção mirada, com dano.
 *  4. {@link #onRightClick} -- cancela e desfaz a farpa sem arremessar.
 *
 * IMPORTANTE (mesma ressalva do Blood Control original): como esta ability
 * não sobrescreve {@code activatesOnPress()}, ela ativa ao SOLTAR a tecla
 * (comportamento padrão do framework -- ver {@code Bender#bend}), não ao
 * apertar. É assim que o Blood Control original também funciona.
 */
public class BoneControlAbility implements Ability {

    private static final float CAST_CHI_COST = 15.0f;
    private static final float TICK_CHI_COST = 0.2f;

    private static final int MIN_DISTANCE = 3;
    private static final int MAX_DISTANCE = 15;
    private static final int DEFAULT_DISTANCE = 6;
    private static final int DISTANCE_STEP = 2;

    private static final float MOVE_SPEED = 0.25f;
    private static final float LAUNCH_SPEED = 2.2f;
    private static final int LAUNCH_MAX_LIFETIME_TICKS = 60; // 3s pra acertar algo, senão some

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        BoneSpikeEntity spike = new BoneSpikeEntity(level, player);
        level.addFreshEntity(spike);
        setAbilityData(bender, spike, DEFAULT_DISTANCE);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BONE_BLOCK_BREAK, SoundSource.PLAYERS, 0.6f, 0.7f);

        bender.setCurrAbility(this); // canalizada -- ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }

        BoneSpikeEntity spike = getSpike(bender);
        if (spike == null || spike.isRemoved() || !spike.getIsControlled()) {
            // A farpa já foi arremessada, destruída, ou o dono desconectou --
            // nesses casos ela cuida de si mesma sozinha (ver BoneSpikeEntity),
            // só precisamos soltar a trava da ability.
            bender.setCurrAbility(null);
            return;
        }

        Player player = bender.player;
        int distance = getDistance(bender);

        // player.pick faz o raycast levando em conta blocos no caminho -- se o
        // jogador mirar numa parede antes da distância configurada, a farpa para
        // na parede em vez de tentar atravessar ela (mesma ideia do
        // AbilityBloodControl.onTick do mod base).
        HitResult hit = player.pick(distance, 1.0f, false);
        Vector3f goal = hit.getLocation().toVector3f();
        spike.moveEntityTowardsGoal(goal, MOVE_SPEED);
    }

    /** Roda do mouse -- sem Shift afasta a farpa, com Shift aproxima. Mesma
     * convenção do {@code AbilityBloodControl.onMiddleClick} original. */
    @Override
    public void onMiddleClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        BoneSpikeEntity spike = getSpike(bender);
        if (spike == null) {
            return;
        }
        int distance = getDistance(bender);
        int next = bender.player.isShiftKeyDown()
                ? Math.max(distance - DISTANCE_STEP, MIN_DISTANCE)
                : Math.min(distance + DISTANCE_STEP, MAX_DISTANCE);
        setAbilityData(bender, spike, next);
    }

    /** Arremessa a farpa na direção mirada, com dano -- a partir daqui ela vira
     * um projétil de verdade (ver comentário em BoneSpikeEntity). */
    @Override
    public void onLeftClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        BoneSpikeEntity spike = getSpike(bender);
        if (spike == null) {
            return;
        }

        Player player = bender.player;
        spike.setControlled(false);
        spike.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, LAUNCH_SPEED, 1.0f);
        spike.maxLifeTime = LAUNCH_MAX_LIFETIME_TICKS;
        spike.lifeTime = 0;

        bender.setCurrAbility(null);
        bender.abilityData = null;
    }

    /** Cancela sem arremessar -- desfaz a farpa. */
    @Override
    public void onRightClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        onRemove(bender);
    }

    @Override
    public void onRemove(Bender bender) {
        BoneSpikeEntity spike = getSpike(bender);
        // Só descarta se ainda estiver sendo controlada -- se já foi arremessada
        // (getIsControlled() == false), ela já é um projétil independente e deve
        // continuar voando mesmo depois da ability soltar a trava.
        if (spike != null && spike.getIsControlled()) {
            spike.discard();
        }
        bender.setCurrAbility(null);
        bender.abilityData = null;
    }

    private BoneSpikeEntity getSpike(Bender bender) {
        Object data = bender.abilityData;
        if (data instanceof Object[] arr && arr.length >= 1 && arr[0] instanceof BoneSpikeEntity spike) {
            return spike;
        }
        return null;
    }

    private int getDistance(Bender bender) {
        Object data = bender.abilityData;
        if (data instanceof Object[] arr && arr.length >= 2 && arr[1] instanceof Integer distance) {
            return distance;
        }
        return DEFAULT_DISTANCE;
    }

    private void setAbilityData(Bender bender, BoneSpikeEntity spike, int distance) {
        bender.abilityData = new Object[]{spike, distance};
    }
}