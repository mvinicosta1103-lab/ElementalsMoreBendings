package com.elementals.morebendings.bending.airsubbendings.sound;

import com.elementals.morebendings.bending.airsubbendings.common.SoundAbility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Silence Field — habilidade toggle. Enquanto ativa, cria uma zona ao redor
 * do bender que abafa o som (evita que mobs hostis "escutem" passos) e
 * aplica leve Slowness a inimigos que entram na área — pensada como
 * ferramenta furtiva/defensiva, e não ofensiva pura como as outras.
 *
 * Consome um "tick de foco" a cada segundo enquanto ativa; desliga sozinha
 * se o bender ficar sem stamina/chi (ajuste esse hook pro seu sistema de
 * energia, se o mod tiver um).
 */
public class SilenceFieldAbility extends SoundAbility {

    private static final double BASE_RADIUS = 5.0D;
    private boolean active = false;

    public SilenceFieldAbility(ServerPlayer bender) {
        super(bender, "silenceField");
    }

    @Override
    public long getCooldown() {
        return 0L; // toggle não usa cooldown fixo, usa custo por tick
    }

    @Override
    public boolean execute() {
        active = !active;
        playSound(active ? "elementals:ability.silence_field_on" : "elementals:ability.silence_field_off", 1.0F, 1.0F);
        return true;
    }

    /**
     * Deve ser chamado a cada tick do bender enquanto o toggle estiver ativo
     * (mesmo hook usado por Lava Surf no addon Obsidian Wake).
     */
    public void onTick() {
        if (!active) {
            return;
        }
        ServerPlayer player = getBender();
        if (player == null) {
            return;
        }

        double radius = hasUpgrade("silenceFieldRadiusI") ? BASE_RADIUS + 2.0D : BASE_RADIUS;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        for (LivingEntity entity : nearby) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
        }
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }
}