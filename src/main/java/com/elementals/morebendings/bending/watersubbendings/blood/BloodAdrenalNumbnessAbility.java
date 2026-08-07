package com.elementals.morebendings.bending.watersubbendings.blood;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.effects.ElementalsStatusEffects;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * "bloodAdrenalNumbness" -- nó enxertado no fim de
 * {@code bloodOverchargeStrengthI}, sub-ramo {@code bloodOvercharge}
 * DENTRO de {@code bloodStep} na árvore REAL de Blood (ver {@link
 * BloodMasteryGraft}).
 * <br><br>
 * Sinergia direta com o efeito {@code OVERCHARGED} que
 * {@code AbilityBlood3} (bloodOvercharge) já concede ao próprio caster
 * (mesmo padrão de sinergia entre habilidades usado por
 * {@code PlasmaClawsAbility}/Blue Fire): SÓ pode ser usada enquanto o
 * bender está Overcharged. Consome parte do próprio Overcharge pra
 * mascarar a dor -- Resistência e Absorção temporárias, representando o
 * corpo "anestesiado" pelo excesso de fluxo sanguíneo. Puramente
 * defensivo, sem afetar terceiros.
 */
public class BloodAdrenalNumbnessAbility implements Ability {

    private static final float CHI_COST = 12.0f;
    private static final int BUFF_DURATION_TICKS = 100; // 5s

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_ADRENAL_NUMBNESS)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!SapsUtils.safeHasStatusEffect(ElementalsStatusEffects.OVERCHARGED.get(), (LivingEntity) caster)) {
            caster.displayClientMessage(
                    Component.literal("Só funciona enquanto você estiver Overcharged."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(this, CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_DURATION_TICKS, 1, false, true, true));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BUFF_DURATION_TICKS, 0, false, true, true));

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6f, 0.7f);
        level.sendParticles(ParticleTypes.HEART,
                caster.getX(), caster.getY() + 1.2, caster.getZ(), 5, 0.3, 0.3, 0.3, 0.0);

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}