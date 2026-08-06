package com.elementals.morebendings.bending.firesubbendings.lightning;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.ElementalConfig;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * "lightningRod" — nó enxertado no fim do leaf {@code
 * lightningOverchargeStrengthII}, ramo {@code lightningOvercharge} da
 * árvore REAL de Lightning Bending do mod base (ver {@link
 * LightningMasteryGraft}) -- até então o único dos 4 ramos-raiz sem nenhuma
 * habilidade deste addon pendurada nele.
 * <br><br>
 * Instantânea, mesmo esquema de {@code PetrifyingTouchAbility}: OBRIGATÓRIO
 * liberar {@code currAbility} de volta pra {@code null} em todo caminho de
 * saída de {@link #onCall} e em {@link #onRemove}.
 * <br><br>
 * O caster se torna um para-raios vivo: um {@code LightningBolt} de
 * verdade cai exatamente na posição dele. O próprio caster é imune (ganha
 * Resistência total por 1 tick antes do impacto, mesmo truque usado por
 * benders de água que absorvem o próprio ataque), mas todo mundo mais
 * dentro de {@link #RADIUS} leva dano -- e o impacto devolve {@link
 * #CHI_REFUND} de chi pro caster, como se ele estivesse "recarregando".
 * Defensivo/ofensivo em área ao mesmo tempo, cobrindo o tema "atrair um
 * raio pra si" que nenhuma habilidade base de Lightning cobre (Storm
 * derruba raios aleatórios numa área ao longo do tempo; isto é instantâneo
 * e sempre na posição do próprio caster).
 */
public class LightningRodAbility implements Ability {

    private static final double RADIUS = 5.0;
    private static final float CHI_COST = 30.0f;
    private static final float CHI_REFUND = 15.0f;
    private static final float SPLASH_DAMAGE = 5.0f;
    private static final int IMMUNITY_TICKS = 40; // resistência total, cobre a queda do raio e o dano residual de fogo

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(LightningMasteryGraft.LIGHTNING_ROD)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        // Resistência total (amplifier 4 = imune a praticamente todo dano)
        // pra garantir que o próprio raio e o fogo residual não firam o caster.
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, IMMUNITY_TICKS, 4, false, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, IMMUNITY_TICKS, 0, false, false, false));

        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(caster.getX(), caster.getY(), caster.getZ());
        bolt.setVisualOnly(false);
        level.addFreshEntity(bolt);

        AABB splash = caster.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, splash,
                e -> e != caster && e.isAlive());
        for (LivingEntity victim : victims) {
            victim.hurt(level.damageSources().lightningBolt(), SPLASH_DAMAGE);
        }

        // Bender não expõe um "giveChi" público -- mexe direto no campo
        // público plrData.chi (mesmo campo que reduceChi usa por baixo) e
        // sincroniza manualmente pro cliente, igual Bender#addXp faz ao
        // recarregar o chi no level-up.
        bender.plrData.chi = Math.min(ElementalConfig.get().MAX_CHI, bender.plrData.chi + CHI_REFUND);
        bender.syncChi();

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                caster.getX(), caster.getY() + 1.0, caster.getZ(), 30, RADIUS * 0.3, 1.0, RADIUS * 0.3, 0.1);

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}