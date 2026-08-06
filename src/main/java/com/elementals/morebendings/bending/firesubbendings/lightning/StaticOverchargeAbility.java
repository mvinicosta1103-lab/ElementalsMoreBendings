package com.elementals.morebendings.bending.firesubbendings.lightning;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * "staticOvercharge" — nó enxertado no fim do leaf {@code
 * lightningVoltArcStrengthII}, folha IRMÃ de {@code lightningEMP} e {@code
 * lightningStaticAura} dentro do ramo {@code lightningVoltArc} da árvore
 * REAL de Lightning Bending do mod base (ver {@link LightningMasteryGraft}).
 * Não conflita: cadeia diferente das outras duas folhas do mesmo ramo-raiz.
 * <br><br>
 * Instantânea, mesmo esquema de {@code PetrifyingTouchAbility}: OBRIGATÓRIO
 * liberar {@code currAbility} de volta pra {@code null} em todo caminho de
 * saída de {@link #onCall} e em {@link #onRemove}.
 * <br><br>
 * O bender se carrega de eletricidade estática por {@link #DURATION_TICKS},
 * ganhando Velocidade e Agilidade (Haste) temporárias -- puro buff de auto
 * mobilidade/combate corpo a corpo, cobrindo o tema "sobrecarga pessoal"
 * que nenhuma habilidade base de Lightning cobre (todas elas afetam o
 * ambiente/outros, nenhuma buffa o próprio caster).
 */
public class StaticOverchargeAbility implements Ability {

    private static final float CHI_COST = 20.0f;
    private static final int DURATION_TICKS = 200; // 10s
    private static final int SPEED_AMPLIFIER = 1; // Velocidade II
    private static final int HASTE_AMPLIFIER = 1; // Agilidade II

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(LightningMasteryGraft.STATIC_OVERCHARGE)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, SPEED_AMPLIFIER));
        caster.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, DURATION_TICKS, HASTE_AMPLIFIER));

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8f, 1.4f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                caster.getX(), caster.getY() + 1.0, caster.getZ(), 18, 0.4, 0.6, 0.4, 0.05);

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}