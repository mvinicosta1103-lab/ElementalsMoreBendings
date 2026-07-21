package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * MudSurgeAbility ("mudSurge")
 * Ramo 3 (Mobilidade) — habilidade bônus anexada dentro do ramo "mudSlide".
 * Toggle (liga/desliga), igual à LavaSurfAbility: enquanto ativo e o
 * jogador estiver em cima de/dentro de um bloco de Mud, ele "patina" na
 * lama — acelera continuamente na direção pra onde está olhando até um
 * teto de velocidade bem alto, sem sofrer o atolamento normal do bloco de
 * Mud vanilla, como se estivesse surfando/deslizando por cima dela.
 * "mudSurgePowerI" aumenta tanto a aceleração quanto o teto de velocidade.
 */
public class MudSurgeAbility implements Ability {

    private static final float TICK_COST = 0.4f;

    private static final double BASE_ACCEL = 0.10;
    private static final double BASE_MAX_SPEED = 0.85;
    private static final double EMPOWERED_ACCEL = 0.14;
    private static final double EMPOWERED_MAX_SPEED = 1.25;

    public void onCall(Bender bender, long deltaT) {
        if (!AbilitySupport.isUnlocked(bender, "mudSurge")) {
            bender.setCurrAbility(null);
            return;
        }

        if (bender.isAbilityInBackground(this)) {
            bender.removeAbilityFromBackground(this);
        } else {
            bender.addBackgroundAbility(this, null);
        }
        bender.setCurrAbility(null);
    }

    public void onBackgroundTick(Bender bender, Object data) {
        Player player = bender.player;

        if (!AbilitySupport.isUnlocked(bender, "mudSurge")) {
            bender.removeAbilityFromBackground(this);
            return;
        }

        BlockPos below = player.blockPosition().below();
        boolean onMud = player.level().getBlockState(below).is(Blocks.MUD)
                || player.level().getBlockState(player.blockPosition()).is(Blocks.MUD);

        if (!onMud) {
            return; // continua "armado" sem gastar chi; só cobra/empurra quando estiver sobre lama
        }

        if (!bender.reduceChi(TICK_COST, false)) {
            bender.removeAbilityFromBackground(this);
            return;
        }

        boolean empowered = bender.plrData.canUseUpgrade("mudSurgePowerI");
        double accel = empowered ? EMPOWERED_ACCEL : BASE_ACCEL;
        double maxSpeed = empowered ? EMPOWERED_MAX_SPEED : BASE_MAX_SPEED;

        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        Vec3 motion = player.getDeltaMovement();
        Vec3 currentHoriz = new Vec3(motion.x, 0.0, motion.z);

        Vec3 direction;
        if (look.lengthSqr() > 1.0E-4) {
            direction = look.normalize();
        } else if (currentHoriz.lengthSqr() > 1.0E-4) {
            direction = currentHoriz.normalize();
        } else {
            direction = new Vec3(0.0, 0.0, 1.0);
        }

        double currentSpeed = currentHoriz.length();
        double newSpeed = Math.min(maxSpeed, currentSpeed + accel);
        Vec3 newHoriz = direction.scale(newSpeed);

        player.setDeltaMovement(newHoriz.x, motion.y, newHoriz.z);
        player.hasImpulse = true;
        player.fallDistance = 0;

        int speedAmp = empowered ? 2 : 1;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, speedAmp, false, false, true));

        if (player.tickCount % 4 == 0) {
            ServerLevel world = (ServerLevel) player.level();
            world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                    player.getX(), player.getY() + 0.1, player.getZ(),
                    6, 0.25, 0.05, 0.25, 0.02);
        }
    }

    public void onRemove(Bender bender) {
        bender.removeAbilityFromBackground(this);
    }
}