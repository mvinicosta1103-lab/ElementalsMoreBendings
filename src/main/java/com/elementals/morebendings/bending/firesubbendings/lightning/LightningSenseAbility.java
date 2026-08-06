package com.elementals.morebendings.bending.firesubbendings.lightning;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * "lightningSense" — nó enxertado no fim do ramo {@code lightningRedirection}
 * da árvore REAL de Lightning Bending do mod base (ver {@link
 * LightningMasteryGraft}). Canalizada por Shift, mesmo esquema de {@code
 * MetalSenseAbility}: enquanto segura Shift, o bender sente todo alvo
 * "condutivo" (ver {@link LightningMasteryGraft#isConductive}) dentro de
 * {@link #RANGE} blocos -- aplica Brilho (Glowing), visível através de
 * paredes, como um campo eletrostático detectando quem conduziria um raio.
 * <br><br>
 * Puramente utilidade/escoteirismo -- não causa dano nem interfere em
 * ninguém, só revela. Combina bem com {@link LightningChainWhipAbility} e
 * {@link ElectroParalysisAbility}, que só afetam quem estiver "condutivo".
 */
public class LightningSenseAbility implements Ability {

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

        if (!bender.getData().canUseUpgrade(LightningMasteryGraft.LIGHTNING_SENSE)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!player.isShiftKeyDown()) {
            caster.displayClientMessage(
                    Component.literal("Segure Shift para manter o Sentido Elétrico ativo."), true);
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
                e -> e != caster && e.isAlive() && LightningMasteryGraft.isConductive(e));

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    GLOW_REFRESH_TICKS + 5, 0, false, false, false));
        }

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.8f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                caster.getX(), caster.getY() + 1.0, caster.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
    }
}