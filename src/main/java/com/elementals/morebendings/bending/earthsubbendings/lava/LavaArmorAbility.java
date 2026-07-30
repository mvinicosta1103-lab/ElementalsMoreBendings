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
 * "lavaArmor" — sétima habilidade da árvore de Lava (ver {@link
 * LavaElement}). Defensiva: o bender se cobre de uma casca de lava
 * semi-endurecida. Mesmo esquema de canalização por Shift de {@code
 * StaticLegsAbility} (Petrification) e {@link LavaSurfAbility}.
 *
 * Enquanto ativa:
 *  - Resistência a Fogo (imune a fogo/lava de verdade) + Resistência
 *    (postura defensiva, igual a Petrification).
 *  - Qualquer golpe corpo a corpo DIRETO sofrido incendeia quem bateu --
 *    ver {@link LavaArmorState} (rastreia quem está ativo) e {@link
 *    LavaArmorCombatHandler} (o hook de dano de verdade, registrado no
 *    NeoForge.EVENT_BUS em {@code ElementalsMoreBendingsMod}, mesmo
 *    esquema de {@code PlasmaBoostCombatHandler}).
 */
public class LavaArmorAbility implements Ability {

    private static final float CAST_CHI_COST = 15.0f;
    private static final float TICK_CHI_COST = 0.3f;

    private static final int EFFECT_REFRESH_TICKS = 20; // 1s, reaplicado a cada tick ativo
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
            // Mesmo motivo do StaticLegsAbility: sem isso a armadura nasceria
            // e morreria no mesmo tick, gastando chi sem o jogador perceber.
            caster.displayClientMessage(
                    Component.literal("Segure Shift para manter a armadura de lava."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        LavaArmorState.activate(caster);
        applyEffects(player);
        playCastFeedback(level, player);

        bender.setCurrAbility(this); // canalizada -- solta ao soltar Shift, ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !player.isShiftKeyDown()) {
            onRemove(bender);
            return;
        }
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }

        LavaArmorState.activate(caster); // no-op se já ativo -- reforça caso algo tenha limpo o set
        applyEffects(player);
    }

    @Override
    public void onRemove(Bender bender) {
        Player player = bender.player;
        if (player instanceof ServerPlayer caster) {
            LavaArmorState.deactivate(caster);
        }
        // Remove os efeitos na hora em vez de deixar a curta duração de
        // "refresh" expirar sozinha -- fica mais responsivo ao soltar Shift.
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        bender.setCurrAbility(null);
    }

    private void applyEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                EFFECT_REFRESH_TICKS, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                EFFECT_REFRESH_TICKS, RESISTANCE_AMPLIFIER, false, false, true));
    }

    private void playCastFeedback(ServerLevel level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 0.6f);
        level.sendParticles(ParticleTypes.LAVA,
                player.getX(), player.getY() + 0.9, player.getZ(), 12, 0.3, 0.5, 0.3, 0.0);
    }
}