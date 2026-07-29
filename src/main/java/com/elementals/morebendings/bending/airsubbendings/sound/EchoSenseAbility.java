package com.elementals.morebendings.bending.airsubbendings.sound;

import com.elementals.morebendings.bending.airsubbendings.common.SoundAbility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Echo Sense — toggle passivo. A cada pulso periódico (ex.: a cada 4s),
 * revela a posição de entidades vivas próximas — inclusive atrás de paredes
 * — através de partículas visíveis só para o bender, simulando ecolocalização.
 * Não causa dano nem efeitos negativos: é puramente de percepção/utilidade.
 */
public class EchoSenseAbility extends SoundAbility {

    private static final double BASE_RADIUS = 12.0D;
    private static final int PULSE_INTERVAL_TICKS = 80; // 4s
    private boolean active = false;
    private int tickCounter = 0;

    public EchoSenseAbility(ServerPlayer bender) {
        super(bender, "echoSense");
    }

    @Override
    public long getCooldown() {
        return 0L;
    }

    @Override
    public boolean execute() {
        active = !active;
        tickCounter = 0;
        playSound(active ? "elementals:ability.echo_sense_on" : "elementals:ability.echo_sense_off", 0.8F, 1.4F);
        return true;
    }

    public void onTick() {
        if (!active) {
            return;
        }
        tickCounter++;
        if (tickCounter < PULSE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        ServerPlayer player = getBender();
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        double radius = hasUpgrade("echoSenseRadiusI") ? BASE_RADIUS + 6.0D : BASE_RADIUS;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());

        for (LivingEntity entity : nearby) {
            level.sendParticles(player, ParticleTypes.NOTE,
                    true,
                    entity.getX(), entity.getY() + entity.getBbHeight() + 0.3D, entity.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }
}