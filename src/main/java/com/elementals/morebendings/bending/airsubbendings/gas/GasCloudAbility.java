package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "gasCloud" — o nó raiz da {@link GasSkillTree}. É a única habilidade que
 * todo Gas bender tem de graça (preço 0); o resto da árvore só melhora ou
 * ramifica a partir dela.
 *
 * Efeito: solta uma nuvem de gás em volta do jogador. Entidades vivas
 * (exceto o próprio caster) que estejam dentro do raio recebem Náusea e
 * Lentidão por um instante — não causa dano, é só "abrir espaço" pro
 * verdadeiro dano vir da especialização (Sufocamento/Vazamento/Ignição).
 *
 * Raio e cooldown escalam com os upgrades de crescimento:
 *  - gasCloudSizeI / II  → +0.75 bloco de raio cada
 *  - gasVentI / II       → -20 ticks (1s) de cooldown cada
 */
public class GasCloudAbility {

    private static final double BASE_RADIUS = 3.0;
    private static final int BASE_COOLDOWN_TICKS = 100; // 5s
    private static final int MIN_COOLDOWN_TICKS = 60;   // 3s, com os 2 níveis de vent

    /** Cooldown por jogador. Fica em memória só — não precisa persistir entre logins. */
    private static final Map<UUID, Long> lastUse = new HashMap<>();

    public static void register() {
        // Nada pra registrar num registry vanilla — a habilidade é acionada
        // via comando/keybind do addon (ver com.elementals.morebendings.client.ModKeyMappings
        // no lado cliente, que manda um C2S pedindo pra executar essa ability).
    }

    /**
     * @return true se a habilidade foi de fato usada (ou seja, não estava em cooldown)
     */
    public static boolean execute(ServerPlayer player) {
        if (!GasElement.isGasBender(player)) {
            return false;
        }
        long now = player.level().getGameTime();
        long last = lastUse.getOrDefault(player.getUUID(), -1000L);
        int cooldown = getCooldownTicks(player);
        if (now - last < cooldown) {
            return false;
        }
        lastUse.put(player.getUUID(), now);

        double radius = getRadius(player);
        ServerLevel level = (ServerLevel) player.level();

        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 1.0, player.getZ(),
                (int) (24 * (radius / BASE_RADIUS)), radius * 0.4, 0.5, radius * 0.4, 0.02);

        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive());
        for (LivingEntity entity : nearby) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        // No máximo uma especialização fica desbloqueada por vez (nó
        // "gasSpecialization" é exclusive=true), então é seguro chamar as
        // três — só a que o jogador realmente comprou vai fazer alguma coisa.
        GasSuffocateAbility.applyOnCloud(player, level, radius);
        GasLeakAbility.applyOnCloud(player, level, radius);
        GasIgniteAbility.applyOnCloud(player, level, radius);
        return true;
    }

    public static double getRadius(ServerPlayer player) {
        double radius = BASE_RADIUS;
        if (GasElement.hasUpgrade(player, GasSkillTree.GAS_CLOUD_SIZE_I)) radius += 0.75;
        if (GasElement.hasUpgrade(player, GasSkillTree.GAS_CLOUD_SIZE_II)) radius += 0.75;
        return radius;
    }

    public static int getCooldownTicks(ServerPlayer player) {
        int cooldown = BASE_COOLDOWN_TICKS;
        if (GasElement.hasUpgrade(player, GasSkillTree.GAS_VENT_I)) cooldown -= 20;
        if (GasElement.hasUpgrade(player, GasSkillTree.GAS_VENT_II)) cooldown -= 20;
        return Math.max(cooldown, MIN_COOLDOWN_TICKS);
    }

    private GasCloudAbility() {
    }
}