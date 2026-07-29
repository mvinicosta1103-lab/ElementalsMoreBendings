package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "sonicStep" — impulsiona o bender rapidamente na direção em que está
 * olhando, usando uma onda de choque como propulsão. Curto alcance,
 * cooldown baixo — ferramenta de mobilidade/esquiva, não de dano.
 * Faz um raycast de bloco (clip) antes de teleportar pra não jogar o
 * bender pra dentro de parede.
 *
 *  - sonicStepDistanceI -> +2.0 blocos de distância
 *  - sonicStepCooldownI -> -0.8s de cooldown
 */
public class SonicStepAbility implements Ability {

    private static final double BASE_DISTANCE = 6.0;
    private static final int BASE_COOLDOWN_TICKS = 60; // 3s
    private static final int MIN_COOLDOWN_TICKS = 44;  // 2.2s
    private static final float CAST_CHI_COST = 2.0f;

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
        int cooldown = SoundElement.hasUpgrade(caster, SoundElement.SONIC_STEP_COOLDOWN_I)
                ? MIN_COOLDOWN_TICKS : BASE_COOLDOWN_TICKS;
        if (now - last < cooldown) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        double distance = SoundElement.hasUpgrade(caster, SoundElement.SONIC_STEP_DISTANCE_I)
                ? BASE_DISTANCE + 2.0 : BASE_DISTANCE;
        Vec3 look = caster.getLookAngle();
        Vec3 origin = caster.position().add(0, caster.getEyeHeight() * 0.5, 0);
        Vec3 desired = origin.add(look.scale(distance));

        // Raycast de bloco pra não teleportar o bender pra dentro de parede --
        // se bater em algo antes da distância desejada, para um pouco antes do bloco.
        HitResult hit = level.clip(new ClipContext(origin, desired,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 destination = desired;
        if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            destination = blockHit.getLocation().subtract(look.scale(0.3));
        }
        destination = destination.subtract(0, caster.getEyeHeight() * 0.5, 0);

        caster.teleportTo(destination.x, destination.y, destination.z);
        caster.setDeltaMovement(look.scale(0.4));
        caster.hurtMarked = true;

        level.sendParticles(ParticleTypes.SONIC_BOOM, origin.x, origin.y, origin.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, caster.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0f, 1.3f);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
