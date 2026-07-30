package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * "lavaSurf" — quinta habilidade raiz da árvore de Lava (ver {@link
 * LavaElement}). Mobilidade: o bender "surfa" sobre uma onda de lava que
 * corre rente ao chão embaixo dele.
 *
 * Mesmo esquema de canalização por Shift de {@code StaticLegsAbility}
 * (Petrification): {@link #activatesOnPress()} retorna {@code true} pra
 * ativar imediatamente ao apertar a tecla, e como o mod base não expõe um
 * hook de "tecla solta", usamos {@code isShiftKeyDown()} em {@link
 * #onTick} como gatilho de cancelamento -- por isso também exigimos Shift
 * já pressionado no instante do cast.
 *
 * Enquanto ativa: Velocidade + Resistência a Fogo (a "onda" é lava de
 * verdade debaixo do jogador) + zera a distância de queda a cada tick pra
 * nunca tomar dano de queda saindo da surfada. Puramente cosmético/
 * utilitário -- não empurra nem acerta ninguém, ao contrário de {@link
 * LavaJetAbility}/{@link MagmaSpikeAbility}.
 */
public class LavaSurfAbility implements Ability {

    private static final float CAST_CHI_COST = 12.0f;
    private static final float TICK_CHI_COST = 0.4f;

    private static final int EFFECT_REFRESH_TICKS = 20; // 1s, reaplicado a cada tick ativo
    private static final int SPEED_AMPLIFIER = 1; // Velocidade II

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
            // Mesmo motivo do StaticLegsAbility: sem isso a surfada nasceria
            // e morreria no mesmo tick, gastando chi sem o jogador perceber.
            caster.displayClientMessage(
                    Component.literal("Segure Shift para surfar na lava."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        applyEffects(player);
        playTrailFeedback(level, player);

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
        player.fallDistance = 0; // surfando não devia doer ao "descer" da onda

        if (player.level() instanceof ServerLevel level && level.getGameTime() % 4 == 0) {
            playTrailFeedback(level, player);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    private void applyEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                EFFECT_REFRESH_TICKS, SPEED_AMPLIFIER, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                EFFECT_REFRESH_TICKS, 0, false, false, true));
    }

    /** Trilha de partículas/som de lava debaixo dos pés -- puramente visual. */
    private void playTrailFeedback(ServerLevel level, Player player) {
        level.sendParticles(ParticleTypes.LAVA,
                player.getX(), player.getY() + 0.1, player.getZ(), 4, 0.3, 0.02, 0.3, 0.0);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.25, 0.02, 0.25, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 0.3f, 1.6f);
    }
}