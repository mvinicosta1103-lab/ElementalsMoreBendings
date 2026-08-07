package com.elementals.morebendings.bending.watersubbendings.blood;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * "bloodRush" -- nó enxertado no fim de {@code bloodStepRangeII}, leaf do
 * ramo {@code bloodStep} da árvore REAL de Blood (ver {@link
 * BloodMasteryGraft}).
 * <br><br>
 * {@code AbilityBlood3} (bloodStep) é um salto instantâneo único -- Blood
 * Rush cobre o oposto: uma corrida SUSTENTADA. Enquanto o jogador segura
 * Shift, o bender acelera a própria circulação continuamente, canalizando
 * chi por tick em troca de Velocidade e Salto. Solta Shift (ou fica sem
 * chi) e o efeito para -- mesmo esquema canalizado de
 * {@code MetalSenseAbility}/{@code StaticLegsAbility}, aplicado aqui como
 * buff de locomoção contínua em vez de sentido/utilidade.
 */
public class BloodRushAbility implements Ability {

    private static final float CAST_CHI_COST = 8.0f;
    private static final float TICK_CHI_COST = 0.4f;
    private static final int EFFECT_REFRESH_TICKS = 20; // 1s

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

        if (!bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_RUSH)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!player.isShiftKeyDown()) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(this, CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        applyRush(level, caster);
        bender.setCurrAbility(this); // canalizada -- solta ao soltar Shift, ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !player.isShiftKeyDown()) {
            onRemove(bender);
            return;
        }
        if (!bender.reduceChi(this, TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }
        if (caster.tickCount % EFFECT_REFRESH_TICKS == 0 && player.level() instanceof ServerLevel level) {
            applyRush(level, caster);
        }
    }

    private void applyRush(ServerLevel level, ServerPlayer caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                EFFECT_REFRESH_TICKS + 5, 1, false, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.JUMP,
                EFFECT_REFRESH_TICKS + 5, 0, false, false, false));

        level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                caster.getX(), caster.getY() + 0.2, caster.getZ(), 4, 0.3, 0.1, 0.3, 0.0);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}