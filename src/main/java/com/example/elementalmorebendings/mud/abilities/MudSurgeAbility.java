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
 * Ramo 3 (Mobilidade) — habilidade bônus anexada dentro do ramo "mudSlide".
 * <p>
 * Antes era um toggle contínuo em background (igual LavaSurf) que ficava
 * "armado" indefinidamente drenando Chi e, por ser uma habilidade de fundo
 * de longa duração, acabava atrapalhando outras habilidades do jogador
 * (esvaziava o Chi disponível e disputava estado com o resto do bender).
 * Reescrita como um <b>impulso instantâneo</b>, sem nenhum estado em
 * background: um único surto de lama empurra o jogador pra frente e pra
 * cima, como um dash curto — gasta Chi uma vez só, no cast, e termina
 * imediatamente, exatamente como Mud Slide. "mudSurgePowerI" aumenta a
 * força do impulso.
 */
public class MudSurgeAbility implements Ability {

    private static final float BASE_COST = 14.0f;
    private static final double BASE_FORWARD_SPEED = 1.1;
    private static final double BASE_UPWARD_SPEED = 0.45;
    private static final double EMPOWERED_FORWARD_SPEED = 1.6;
    private static final double EMPOWERED_UPWARD_SPEED = 0.55;

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
        double forwardSpeed = empowered ? EMPOWERED_FORWARD_SPEED : BASE_FORWARD_SPEED;
        double upwardSpeed = empowered ? EMPOWERED_UPWARD_SPEED : BASE_UPWARD_SPEED;

        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 0.01) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();

        player.setDeltaMovement(forward.x * forwardSpeed, upwardSpeed, forward.z * forwardSpeed);
        player.hasImpulse = true;
        player.fallDistance = 0;

        ServerLevel world = player.serverLevel();
        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                player.getX(), player.getY() + 0.2, player.getZ(),
                35, 0.3, 0.15, 0.3, 0.04);

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}