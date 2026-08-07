package com.elementals.morebendings.bending.airsubbendings.gas;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "gasCloud" — nó raiz da árvore do {@link GasElement}. Antes era acionada
 * por um packet C2S customizado (removido); agora é uma {@code Ability}
 * de verdade, disparada pelo próprio sistema de cast nativo do mod base
 * (tecla já ligada ao slot 0 do elemento atual), igual
 * {@code PressurePointAbility}.
 *
 * Efeito: solta uma nuvem de gás em volta do jogador. Entidades vivas
 * (exceto o próprio caster) dentro do raio recebem Náusea e Lentidão.
 * <p>
 * REWORK: antes disparava também a especialização "ativa" do jogador
 * (Sufocamento/Vazamento/Ignição) automaticamente no final do cast. Isso
 * mudou -- as três especializações agora são abilities independentes,
 * com tecla própria cada uma (ver {@link GasSuffocateAbility}, {@link
 * GasLeakAbility}, {@link GasIgniteAbility}). Esta ability ficou só com
 * o papel de controle de área/utilidade.
 *
 * Raio e cooldown escalam com os upgrades de crescimento:
 *  - gasCloudSizeI / II  → +0.75 bloco de raio cada
 *  - gasVentI / II       → -20 ticks (1s) de cooldown cada
 */
public class GasCloudAbility implements Ability {

    private static final double BASE_RADIUS = 3.0;
    private static final int BASE_COOLDOWN_TICKS = 100; // 5s
    private static final int MIN_COOLDOWN_TICKS = 60;   // 3s, com os 2 níveis de vent
    private static final float CAST_CHI_COST = 4.0f;    // ajustar conforme balanceamento

    /** Cooldown por jogador. Fica em memória só — não precisa persistir entre logins. */
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
        int cooldown = getCooldownTicks(caster);
        if (now - last < cooldown) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        double radius = getRadius(caster);

        level.sendParticles(ParticleTypes.CLOUD,
                caster.getX(), caster.getY() + 1.0, caster.getZ(),
                (int) (24 * (radius / BASE_RADIUS)), radius * 0.4, 0.5, radius * 0.4, 0.02);

        AABB area = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());
        for (LivingEntity entity : nearby) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        // REWORK: Suffocate/Leak/Ignite não são mais disparadas daqui.
        // Cada uma virou uma Ability própria com tecla dedicada (ver
        // GasSuffocateAbility/GasLeakAbility/GasIgniteAbility) -- o
        // jogador escolhe explicitamente qual usar e quando, em vez de
        // depender da especialização "ativa" no momento em que apertasse
        // Gas Cloud. Esta ability agora é só o utilitário defensivo/de
        // controle de área (Confusão + Lentidão).

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    public static double getRadius(ServerPlayer player) {
        double radius = BASE_RADIUS;
        if (GasElement.hasUpgrade(player, GasElement.GAS_CLOUD_SIZE_I)) radius += 0.75;
        if (GasElement.hasUpgrade(player, GasElement.GAS_CLOUD_SIZE_II)) radius += 0.75;
        return radius;
    }

    public static int getCooldownTicks(ServerPlayer player) {
        int cooldown = BASE_COOLDOWN_TICKS;
        if (GasElement.hasUpgrade(player, GasElement.GAS_VENT_I)) cooldown -= 20;
        if (GasElement.hasUpgrade(player, GasElement.GAS_VENT_II)) cooldown -= 20;
        return Math.max(cooldown, MIN_COOLDOWN_TICKS);
    }
}