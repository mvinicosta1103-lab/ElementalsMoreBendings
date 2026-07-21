package com.example.elementalmorebendings.crystal.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * CrystalStepAbility ("crystalStep")
 * Ramo 3 (Mobilidade) — habilidade bônus anexada dentro do ramo
 * "crystalDash": o jogador se estilhaça num ponto e se reforma instantes
 * depois um pouco à frente — um blink curto, na direção pra onde está
 * olhando, em vez de um impulso de velocidade como o Crystal Dash. Bom
 * pra atravessar um vão pequeno ou escapar de um agarrão.
 * <p>
 * Diferente de {@code MudSurgeAbility} (que dá um impulso de velocidade,
 * então ainda respeita colisão normalmente ao longo do trajeto), este é um
 * teleporte de verdade: a posição de chegada é limitada pela primeira
 * parede sólida no caminho (via raycast), pra não atravessar blocos.
 * "crystalStepRangeI" aumenta a distância máxima do blink.
 */
public class CrystalStepAbility implements Ability {

    private static final float BASE_COST = 16.0f;
    private static final double BASE_RANGE = 6.0;
    private static final double EMPOWERED_RANGE = 9.0;
    private static final double WALL_BUFFER = 0.4;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "crystalStep", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double range = bender.plrData.canUseUpgrade("crystalStepRangeI") ? EMPOWERED_RANGE : BASE_RANGE;

        Vec3 start = player.position();
        Vec3 look = player.getLookAngle().normalize();

        HitResult hit = player.pick(range, 0.0f, false);
        double distance = hit.getType() == HitResult.Type.MISS
                ? range
                : Math.max(0.5, hit.getLocation().distanceTo(player.getEyePosition()) - WALL_BUFFER);

        Vec3 destination = start.add(look.x * distance, 0.0, look.z * distance);

        ServerLevel world = player.serverLevel();
        AbilitySupport.spawnShatterBurst(world, start.add(0, player.getBbHeight() * 0.5, 0), 26, 0.45);

        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);

        AbilitySupport.spawnShatterBurst(world, destination.add(0, player.getBbHeight() * 0.5, 0), 26, 0.45);
        world.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 1.3f);

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}