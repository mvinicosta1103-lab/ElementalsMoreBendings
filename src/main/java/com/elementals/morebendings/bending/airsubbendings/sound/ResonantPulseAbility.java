package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "resonantPulse" — segunda habilidade raiz da árvore de Sound (ver
 * {@link SoundElement}). Instantânea, igual {@code EchoingVoiceAbility}.
 *
 * Diferente de echoingVoice (ofensiva), essa é utilitária: o jogador
 * solta um pulso de eco esférico ao redor de si que "atravessa" blocos
 * (o alcance é medido em linha reta, não por raycast/linha de visão) --
 * toda criatura viva dentro do raio fica marcada com {@link
 * MobEffects#GLOWING} por alguns segundos, revelando sua posição mesmo
 * atrás de paredes. Eco-localização, no espírito de como Toph "vê" com
 * vibrações -- só que com som em vez de terra.
 */
public class ResonantPulseAbility implements Ability {

    private static final double RADIUS = 20.0;
    private static final int GLOW_DURATION_TICKS = 100; // 5s
    private static final int BASE_COOLDOWN_TICKS = 140; // 7s
    private static final float CAST_CHI_COST = 3.0f;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -100000L);
        if (now - last < BASE_COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        level.sendParticles(ParticleTypes.SONIC_BOOM,
                caster.getX(), caster.getY() + 1.0, caster.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.NOTE,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                30, RADIUS * 0.15, 0.5, RADIUS * 0.15, 1.0);
        level.playSound(null, caster.blockPosition(), SoundEvents.SCULK_BLOCK_SPREAD,
                SoundSource.PLAYERS, 1.0f, 1.0f);

        AABB area = caster.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION_TICKS, 0));
        }

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}