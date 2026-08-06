package com.elementals.morebendings.bending.earthsubbendings.metal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * "metalSense" — nó enxertado no fim do ramo {@code metalCable} da árvore
 * REAL de Metal Bending do mod base (ver {@link MetalMasteryGraft}).
 * Canalizada por Shift (mesmo esquema de {@code LavaArmorAbility} /
 * {@code StaticLegsAbility}): enquanto segura Shift, o bender "sente"
 * qualquer entidade viva com armadura de metal de verdade (ver
 * {@link MetalMasteryGraft#isWearingMetal}) dentro de {@link #RANGE} blocos
 * -- aplica um leve efeito de Brilho (Glowing) nelas, contorno visível
 * através de paredes, igual um sentido de metal de verdade detectando
 * através de obstáculos.
 *
 * Puramente utilidade/escoteirismo -- não causa dano nem interfere em
 * ninguém, só revela. Combina bem com {@link MetalSlamAbility} (que só
 * afeta quem está usando metal).
 */
public class MetalSenseAbility implements Ability {

    private static final double RANGE = 20.0;
    private static final float CAST_CHI_COST = 10.0f;
    private static final float TICK_CHI_COST = 0.25f;
    private static final int GLOW_REFRESH_TICKS = 20; // 1s, reaplicado a cada tick ativo

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

        if (!bender.getData().canUseUpgrade(MetalMasteryGraft.METAL_SENSE)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!player.isShiftKeyDown()) {
            caster.displayClientMessage(
                    Component.literal("Segure Shift para manter o Sentido de Metal ativo."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        pulse(level, caster);
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
        if (caster.tickCount % GLOW_REFRESH_TICKS == 0 && player.level() instanceof ServerLevel level) {
            pulse(level, caster);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    private void pulse(ServerLevel level, ServerPlayer caster) {
        AABB area = caster.getBoundingBox().inflate(RANGE);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != caster && e.isAlive() && MetalMasteryGraft.isWearingMetal(e));

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    GLOW_REFRESH_TICKS + 5, 0, false, false, false));
        }

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.4f, 1.6f);
        level.sendParticles(ParticleTypes.END_ROD,
                caster.getX(), caster.getY() + 1.0, caster.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
    }
}