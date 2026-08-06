package com.elementals.morebendings.bending.earthsubbendings.metal;

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
 * "metalPlating" — nó enxertado no fim do leaf {@code metalArmorEfficiencyI},
 * folha do TRONCO do ramo {@code metalArmor} (raiz de onde {@code metalDecoy}
 * -- já usado por {@link ChestplateDevelopAbility} em {@code
 * metalDecoyDamageII} -- se ramifica). Não conflita: são cadeias diferentes
 * dentro do mesmo ramo-raiz (ver {@link MetalMasteryGraft}).
 *
 * Instantânea, mesmo esquema de {@code PetrifyingTouchAbility}: OBRIGATÓRIO
 * liberar {@code currAbility} de volta pra {@code null} em todo caminho de
 * saída de {@link #onCall} e em {@link #onRemove}.
 *
 * Reforça magneticamente a própria armadura de metal já equipada (ver
 * {@link MetalMasteryGraft#isWearingMetal}) por {@link #DURATION_TICKS},
 * concedendo Resistência e Absorção temporárias -- só funciona em quem já
 * estiver de metal (senão não há nada pra reforçar). Diferente de {@link
 * ChestplateDevelopAbility} (que CRIA uma peitoral do zero): esta buffa
 * quem já veste metal, incluindo peças de ferro/ouro/malha/netherite em
 * qualquer slot, não só o peito.
 */
public class MetalPlatingAbility implements Ability {

    private static final float CHI_COST = 20.0f;
    private static final int DURATION_TICKS = 200; // 10s
    private static final int RESISTANCE_AMPLIFIER = 1; // Resistência II
    private static final int ABSORPTION_AMPLIFIER = 1; // 4 corações extras

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(MetalMasteryGraft.METAL_PLATING)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!MetalMasteryGraft.isWearingMetal(caster)) {
            caster.displayClientMessage(
                    Component.literal("Você precisa estar usando armadura de metal de verdade."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, RESISTANCE_AMPLIFIER));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, ABSORPTION_AMPLIFIER));

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7f, 0.8f);
        level.sendParticles(ParticleTypes.CRIT,
                caster.getX(), caster.getY() + 1.0, caster.getZ(), 16, 0.4, 0.6, 0.4, 0.02);

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}