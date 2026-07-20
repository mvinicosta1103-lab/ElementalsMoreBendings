package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * MudSlideAbility ("mudSlide")
 * Ramo 3 (Mobilidade) — antes desse arquivo existir, "mudSlide" era só um
 * nó de upgrade na árvore sem nenhuma Ability por trás (ficava sem efeito
 * nenhum ao usar). Agora dispara uma onda de lama: empurra o jogador pra
 * frente com um impulso de velocidade e arrasta/derruba quem estiver no
 * caminho, na direção pra onde o jogador está olhando.
 */
public class MudSlideAbility implements Ability {

    private static final float BASE_COST = 18.0f;
    private static final double BASE_DISTANCE = 6.0;
    private static final double WIDTH = 1.5;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "mudSlide", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double distance = bender.plrData.canUseUpgrade("mudSlideDistanceII") ? BASE_DISTANCE + 4.0
                : (bender.plrData.canUseUpgrade("mudSlideDistanceI") ? BASE_DISTANCE + 2.0 : BASE_DISTANCE);
        double speed = bender.plrData.canUseUpgrade("mudSlideSpeedI") ? 1.6 : 1.1;

        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 0.01) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        forward = forward.normalize();

        ServerLevel world = player.serverLevel();

        // Impulso pro jogador — o "surf" na onda de lama
        player.setDeltaMovement(forward.x * speed, Math.max(player.getDeltaMovement().y, 0.15), forward.z * speed);
        player.hasImpulse = true;
        player.fallDistance = 0;

        // Empurra/derruba quem estiver no caminho da onda
        Vec3 center = player.position().add(forward.scale(distance / 2.0));
        List<LivingEntity> targets = AbilitySupport.entitiesAround(player, center, distance / 2.0 + WIDTH);
        for (LivingEntity target : targets) {
            if (target == player) {
                continue;
            }
            target.push(forward.x * 1.2, 0.35, forward.z * 1.2);
            target.hurtMarked = true;
        }

        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                player.getX(), player.getY() + 0.2, player.getZ(),
                40, 0.4, 0.15, 0.4, 0.03);

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}