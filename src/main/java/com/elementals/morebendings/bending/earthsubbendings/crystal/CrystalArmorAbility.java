package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
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
 * "crystalArmor" — habilidade nova, filha de {@code crystalWall} (ver
 * {@link CrystalElement}, que já reservava o ícone {@code crystal_armor_icon.png}
 * pra ela). Defensiva: o bender se cobre de uma casca de placas de cristal
 * rente ao corpo.
 *
 * Mesmo esquema de canalização por Shift de {@code LavaArmorAbility} /
 * {@code StaticLegsAbility}: {@link #activatesOnPress()} ativa na hora, e
 * como o mod base não expõe um hook de "tecla solta", {@code
 * isShiftKeyDown()} em {@link #onTick} é o gatilho de cancelamento.
 *
 * Enquanto ativa: Resistência (a couraça de cristal absorve parte de
 * qualquer impacto sofrido).
 */
public class CrystalArmorAbility implements Ability {

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
            // Mesmo motivo do StaticLegsAbility/LavaArmorAbility: sem isso a
            // armadura nasceria e morreria no mesmo tick, gastando chi sem
            // o jogador perceber.
            caster.displayClientMessage(
                    Component.literal("Segure Shift para manter a armadura de cristal."), true);
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
        if (!(player instanceof ServerPlayer) || !player.isShiftKeyDown()) {
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
        // Remove o efeito na hora em vez de deixar a curta duração de
        // "refresh" expirar sozinha -- fica mais responsivo ao soltar Shift.
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        bender.setCurrAbility(null);
    }

    private void applyEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                EFFECT_REFRESH_TICKS, RESISTANCE_AMPLIFIER, false, false, true));
    }

    private void playCastFeedback(ServerLevel level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 0.8f, 0.7f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                player.getX(), player.getY() + 0.9, player.getZ(), 16, 0.3, 0.5, 0.3, 0.0);
    }
}