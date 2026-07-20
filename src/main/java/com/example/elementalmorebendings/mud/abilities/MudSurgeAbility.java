package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * MudSurgeAbility ("mudSurge")
 * Ramo 3 (Mobilidade) — habilidade bônus anexada dentro do ramo "mudSlide":
 * um puxão rápido de lama, pra frente e pra cima, tipo um pequeno "dash"
 * instantâneo. "mudSurgePowerI" aumenta a força do puxão.
 */
public class MudSurgeAbility implements Ability {

    private static final float BASE_COST = 15.0f;
    private static final double BASE_FORWARD_POWER = 1.4;
    private static final double BASE_UPWARD_POWER = 0.5;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "mudSurge", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        boolean empowered = bender.plrData.canUseUpgrade("mudSurgePowerI");
        double forwardPower = empowered ? BASE_FORWARD_POWER + 0.6 : BASE_FORWARD_POWER;
        double upwardPower = empowered ? BASE_UPWARD_POWER + 0.15 : BASE_UPWARD_POWER;

        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 0.01) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();

        player.setDeltaMovement(forward.x * forwardPower, upwardPower, forward.z * forwardPower);
        player.hasImpulse = true;
        player.fallDistance = 0;

        ServerLevel world = player.serverLevel();
        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                player.getX(), player.getY() + 0.2, player.getZ(),
                30, 0.3, 0.15, 0.3, 0.03);

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}