package com.elementals.morebendings.bending.earthsubbendings.petrification;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.effects.ElementalsStatusEffects;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * "staticLegs" — segunda habilidade raiz da árvore de Petrification (ver
 * {@link PetrificationElement}). Defensiva: o bender planta as próprias
 * pernas no chão e as transforma em pedra. Enquanto ativa, o jogador não
 * consegue se mover (efeito {@link ElementalsStatusEffects#STATIONARY} do
 * mod base -- só trava movimento via {@code makeStuckInBlock}, então ainda
 * dá pra atacar/usar itens normalmente) mas ganha Resistência, uma espécie
 * de "postura de fortaleza" em troca da mobilidade.
 *
 * Canalizada enquanto o jogador ficar agachado, no MESMO esquema de {@code
 * MudTrapAbility}: {@link #activatesOnPress()} retorna {@code true} pra
 * ativar imediatamente ao apertar a tecla (sem isso, o framework só chama
 * {@link #onCall} ao SOLTAR a tecla -- ver {@code Bender#bend}), e como o
 * mod base não expõe um hook de "tecla da ability foi solta", usamos
 * {@code isShiftKeyDown()} em {@link #onTick} como o gatilho de
 * cancelamento -- por isso também exigimos Shift já pressionado no
 * instante do cast (senão a postura nasceria e morreria no mesmo tick, sem
 * o jogador nem perceber).
 */
public class StaticLegsAbility implements Ability {

    private static final float CAST_CHI_COST = 15.0f;
    private static final float TICK_CHI_COST = 0.3f;

    /** Duração de cada aplicação do efeito -- só precisa ser um pouco maior
     * que 1 tick, já que {@link #onTick} fica reaplicando enquanto ativa. */
    private static final int EFFECT_REFRESH_TICKS = 20; // 1s
    private static final int RESISTANCE_AMPLIFIER = 0; // Resistência I (-20% de dano)

    @Override
    public boolean activatesOnPress() {
        return true;
    }

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!player.isShiftKeyDown()) {
            // Sem isso, a postura começaria e cancelaria no mesmo tick (onTick
            // vê isShiftKeyDown()==false e libera na hora) -- pareceria que a
            // habilidade simplesmente não fez nada, mesmo consumindo chi.
            caster.displayClientMessage(
                    Component.literal("Segure Shift para manter as pernas estáticas."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        applyEffects(player);
        playCastFeedback(level, player);

        bender.setCurrAbility(this); // canalizada -- solta ao soltar Shift, ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!player.isShiftKeyDown()) {
            onRemove(bender);
            return;
        }
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }

        applyEffects(player);
    }

    @Override
    public void onRemove(Bender bender) {
        Player player = bender.player;
        // Remove os efeitos na hora em vez de deixar a curta duração de
        // "refresh" expirar sozinha -- fica mais responsivo ao soltar Shift.
        player.removeEffect(ElementalsStatusEffects.STATIONARY.get());
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        bender.setCurrAbility(null);
    }

    private void applyEffects(Player player) {
        player.addEffect(new MobEffectInstance(ElementalsStatusEffects.STATIONARY.get(),
                EFFECT_REFRESH_TICKS, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                EFFECT_REFRESH_TICKS, RESISTANCE_AMPLIFIER, false, false, true));
    }

    private void playCastFeedback(ServerLevel level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 0.7f, 0.6f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                player.getX(), player.getY() + 0.1, player.getZ(), 14, 0.3, 0.05, 0.3, 0.0);
    }
}