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

/**
 * "Flying" — voo estilo criativo, limitado por um fôlego que esgota
 * enquanto o jogador está no ar e volta a encher quando ele pousa. Não usa
 * a árvore de upgrades do Gas; é uma ability única, ativada/desativada por
 * toggle (ver {@code ModKeyMappings} no lado cliente).
 *
 * Chame {@link #tick(ServerPlayer)} uma vez por tick por jogador voando —
 * o gancho certo pra isso é um listener de {@code PlayerTickEvent.Post} no
 * lado servidor (ainda não registrado — ver checklist no README/plano).
 */
public class FlyingAbility {

    private static final int MAX_STAMINA_TICKS = 20 * 20; // 20s de voo
    private static final int REGEN_PER_TICK_ON_GROUND = 2; // recupera 5x mais rápido no chão

    private static final Set<UUID> flying = new HashSet<>();
    private static final Map<UUID, Integer> stamina = new HashMap<>();

    public static void register() {
        // Sem registry vanilla pra mexer — ver nota na classe sobre o tick hook.
    }

    /** Chamada pelo toggle do keybind/comando. @return o novo estado (true = passou a voar). */
    public static boolean toggle(ServerPlayer player) {
        if (flying.contains(player.getUUID())) {
            stopFlying(player);
            return false;
        }
        if (!FlyingElement.isFlyingBender(player)) {
            return false;
        }
        if (getStamina(player) <= 0) {
            return false; // sem fôlego, não decola
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
        // não mexe em mayfly/flying se o jogador já tinha voo por outro
        // motivo (criativo/espectador) — só desliga se foi a gente que ligou.
        if (player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                || player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
            abilities.flying = false;
            abilities.mayfly = false;
            player.onUpdateAbilities();
        }
    }

    /** Chame uma vez por tick do servidor pra cada jogador com Flying desbloqueado. */
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
                stopFlying(player); // ficou sem fôlego, pousa forçado
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