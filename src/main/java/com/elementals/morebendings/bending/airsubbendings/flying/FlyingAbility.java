package com.elementals.morebendings.bending.airsubbendings.flying;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FlyingAbility {

    private static final int MAX_STAMINA_TICKS = 20 * 20;
    private static final int REGEN_PER_TICK_ON_GROUND = 2;

    private static final Set<UUID> flying = new HashSet<>();
    private static final Map<UUID, Integer> stamina = new HashMap<>();

    public static void register() {
        // TODO: Registre o tick no servidor (ex: NeoForge.EVENT_BUS.addListener(...))
    }

    public static boolean toggle(ServerPlayer player) {
        if (flying.contains(player.getUUID())) {
            stopFlying(player);
            return false;
        }
        // Se der erro aqui, a classe FlyingElement não existe ou o método tá errado!
        if (!FlyingElement.isFlyingBender(player)) {
            return false;
        }
        if (getStamina(player) <= 0) {
            return false;
        }
        startFlying(player);
        return true;
    }

    private static void startFlying(ServerPlayer player) {
        flying.add(player.getUUID());
        Abilities abilities = player.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        player.onUpdateAbilities();
    }

    private static void stopFlying(ServerPlayer player) {
        flying.remove(player.getUUID());
        Abilities abilities = player.getAbilities();

        if (player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                || player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
            abilities.flying = false;
            abilities.mayfly = false;
            player.onUpdateAbilities();
        }
    }

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        if (flying.contains(id)) {
            int left = getStamina(player) - 1;
            stamina.put(id, left);
            if (player.tickCount % 5 == 0) {
                ServerLevel level = (ServerLevel) player.level();
                level.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY(), player.getZ(),
                        3, 0.3, 0.1, 0.3, 0.01);
            }
            if (left <= 0) {
                stopFlying(player);
            }
        } else if (player.onGround()) {
            stamina.merge(id, REGEN_PER_TICK_ON_GROUND, (curr, add) ->
                    Math.min(MAX_STAMINA_TICKS, curr + add));
        }
    }

    public static int getStamina(ServerPlayer player) {
        return stamina.getOrDefault(player.getUUID(), MAX_STAMINA_TICKS);
    }

    public static boolean isFlying(ServerPlayer player) {
        return flying.contains(player.getUUID());
    }

    private FlyingAbility() {
    }
}