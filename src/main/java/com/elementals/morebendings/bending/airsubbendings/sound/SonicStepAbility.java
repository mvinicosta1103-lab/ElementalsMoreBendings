package com.elementals.morebendings.bending.airsubbendings.sound;

import com.elementals.morebendings.bending.airsubbendings.common.SoundAbility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Sonic Step — impulsiona o bender rapidamente na direção em que está
 * olhando, usando uma onda de choque como propulsão. Curto alcance, cooldown
 * baixo — pensada como ferramenta de mobilidade/esquiva, não de dano.
 */
public class SonicStepAbility extends SoundAbility {

    private static final double BASE_DISTANCE = 6.0D;
    private static final long BASE_COOLDOWN_MS = 3000L;

    public SonicStepAbility(ServerPlayer bender) {
        super(bender, "sonicStep");
    }

    @Override
    public long getCooldown() {
        return hasUpgrade("sonicStepCooldownI") ? BASE_COOLDOWN_MS - 800L : BASE_COOLDOWN_MS;
    }

    @Override
    public boolean execute() {
        ServerPlayer player = getBender();
        if (player == null) {
            return false;
        }

        double distance = hasUpgrade("sonicStepDistanceI") ? BASE_DISTANCE + 2.0D : BASE_DISTANCE;
        Vec3 look = player.getLookAngle();
        Vec3 destination = player.position().add(look.scale(distance));

        // Idealmente aqui entra uma varredura de colisão (raycast) contra o
        // mundo pra não teleportar o bender pra dentro de blocos sólidos —
        // reaproveite a mesma lógica de trajetória usada por MoltenGripAbility.
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(look.scale(0.4D));

        playSound("elementals:ability.sonic_step", 1.0F, 1.3F);
        return true;
    }
}