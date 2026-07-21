package com.example.elementalmorebendings.crystal.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * CrystalDashAbility ("crystalDash")
 * Ramo 3 (Mobilidade) — o jogador se impulsiona pra frente numa investida
 * de cristal: um rastro de ametista se forma sob seus pés no instante do
 * impulso e se estilhaça logo em seguida, empurrando quem estiver no
 * caminho. Mesmo padrão de {@code MudSlideAbility}.
 */
public class CrystalDashAbility implements Ability {

    private static final float BASE_COST = 18.0f;
    private static final double BASE_DISTANCE = 6.0;
    private static final double WIDTH = 1.5;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "crystalDash", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double distance = bender.plrData.canUseUpgrade("crystalDashDistanceII") ? BASE_DISTANCE + 4.0
                : (bender.plrData.canUseUpgrade("crystalDashDistanceI") ? BASE_DISTANCE + 2.0 : BASE_DISTANCE);
        double speed = bender.plrData.canUseUpgrade("crystalDashSpeedI") ? 1.6 : 1.1;

        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 0.01) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();

        ServerLevel world = player.serverLevel();

        player.setDeltaMovement(forward.x * speed, Math.max(player.getDeltaMovement().y, 0.15), forward.z * speed);
        player.hasImpulse = true;
        player.fallDistance = 0;

        Vec3 center = player.position().add(forward.scale(distance / 2.0));
        List<LivingEntity> targets = AbilitySupport.entitiesAround(player, center, distance / 2.0 + WIDTH);
        for (LivingEntity target : targets) {
            if (target == player) {
                continue;
            }
            target.push(forward.x * 1.2, 0.35, forward.z * 1.2);
            target.hurtMarked = true;
        }

        AbilitySupport.spawnShatterBurst(world, player.position().add(0, 0.2, 0), 30, 0.4);
        world.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.7f, 1.2f);

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}