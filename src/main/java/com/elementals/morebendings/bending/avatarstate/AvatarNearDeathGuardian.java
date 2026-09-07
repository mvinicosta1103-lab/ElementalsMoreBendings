package com.elementals.morebendings.bending.avatarstate;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
import com.elementals.morebendings.effects.MoreBendingsEffects;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Instinto de sobrevivência do Avatar: quando quem está prestes a nascer/já
 * é o Avatar (ver {@link #isBornOrCurrentAvatar}) toma um golpe que deixaria
 * ele perto da morte, o Avatar State liga na hora (se ainda não estiver
 * ligado) e um dos 4 "arcos" elementares (mesmos elementos dos anéis de
 * {@link AvatarStateManager.RingElement}) reage automaticamente e de forma
 * ALEATÓRIA, protegendo com dano em área e/ou empurrão -- nunca dois arcos
 * de uma vez, só um por ativação:
 * <ul>
 *     <li><b>Ar</b> -- escudo de vento: repulsão radial + corte + sangramento,
 *     depois some.</li>
 *     <li><b>Fogo</b> -- explosão de fogo azul.</li>
 *     <li><b>Água</b> -- estilhaços de gelo/lâminas de água: congela, dano
 *     perfurante e sangramento.</li>
 *     <li><b>Terra</b> -- vários espinhos de pedra teleguiados, um pra cada
 *     entidade mais próxima (ver {@link #triggerEarthGuard}, usa o
 *     {@link AvatarFxScheduler} pra "perseguir" o alvo tick a tick).</li>
 * </ul>
 * <p>
 * O golpe que disparou a reação também é amortecido (não anulado -- não é
 * invencibilidade, ver {@link #DAMAGE_ABSORPTION_FRACTION}), e a reação
 * inteira tem um cooldown por jogador (ver {@link #COOLDOWN_TICKS}) pra não
 * disparar de novo a cada tique enquanto a vida continuar baixa.
 */
public final class AvatarNearDeathGuardian {

    private AvatarNearDeathGuardian() {
    }

    // ==================== Gatilho ====================

    /** Só reage se, depois do golpe, a vida restante for <= isso (2 corações). */
    private static final float NEAR_DEATH_HEALTH_THRESHOLD = 4.0f;

    /** Nunca reage de novo antes disso (30s) -- evita disparo repetido a cada golpe enquanto a vida seguir baixa. */
    private static final int COOLDOWN_TICKS = 600;

    /** Fração do golpe que disparou a reação que é absorvida pelo burst -- amortece, não anula. */
    private static final float DAMAGE_ABSORPTION_FRACTION = 0.65f;

    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new HashMap<>();

    /**
     * Registrado via NeoForge.EVENT_BUS em ElementalsMoreBendingsMod, em
     * {@code LivingIncomingDamageEvent} -- ou seja, roda ANTES do golpe ser
     * de fato aplicado, o que é o que permite tanto amortecer o dano quanto
     * ativar o Avatar State "no mesmo momento" (antes da vida realmente cair
     * pra perto de zero), em vez de reagir só depois do fato consumado.
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getAmount() <= 0.0f) {
            return;
        }
        if (!isBornOrCurrentAvatar(victim)) {
            return;
        }

        float remainingHealth = victim.getHealth() - event.getAmount();
        if (remainingHealth > NEAR_DEATH_HEALTH_THRESHOLD) {
            return; // ainda não está perto de morrer -- não reage à toa em qualquer arranhão
        }
        if (isOnCooldown(victim, level)) {
            return;
        }

        LAST_TRIGGER_TICK.put(victim.getUUID(), level.getGameTime());

        // Amortece o golpe que quase matou -- sobra o suficiente pra ele
        // sobreviver na maioria dos casos, sem virar invencibilidade.
        event.setAmount(event.getAmount() * (1.0f - DAMAGE_ABSORPTION_FRACTION));

        if (!AvatarStateManager.isActive(victim)) {
            AvatarStateManager.activate(victim);
        }

        triggerRandomGuardArc(level, victim);
    }

    /**
     * "Nascer como Avatar" = já segurar o título de Avatar do servidor (ver
     * {@link ServerAvatarManager}, inclusive quem acabou de receber o título
     * sozinho ao logar com o cargo vago). "Ser o Avatar" cobre também quem
     * já dominou os 4 elementos-base por conta própria ({@link
     * AvatarStateManager#isEligible}) ou já está com o Avatar State ligado
     * agora -- qualquer um desses três já conta como "é o Avatar" pra fins
     * desse instinto de sobrevivência.
     */
    private static boolean isBornOrCurrentAvatar(ServerPlayer player) {
        return ServerAvatarManager.isCurrentAvatar(player)
                || AvatarStateManager.isActive(player)
                || AvatarStateManager.isEligible(player);
    }

    private static boolean isOnCooldown(ServerPlayer player, ServerLevel level) {
        Long last = LAST_TRIGGER_TICK.get(player.getUUID());
        return last != null && (level.getGameTime() - last) < COOLDOWN_TICKS;
    }

    /** Limpa o cooldown de quem desconecta -- sem isso o UUID nunca mais sairia do mapa. */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_TRIGGER_TICK.remove(event.getEntity().getUUID());
    }

    // ==================== Escolha aleatória do arco ====================

    private static void triggerRandomGuardArc(ServerLevel level, ServerPlayer victim) {
        AvatarStateManager.RingElement[] values = AvatarStateManager.RingElement.values();
        AvatarStateManager.RingElement chosen = values[ThreadLocalRandom.current().nextInt(values.length)];
        switch (chosen) {
            case AIR -> triggerAirGuard(level, victim);
            case FIRE -> triggerFireGuard(level, victim);
            case WATER -> triggerWaterGuard(level, victim);
            case EARTH -> triggerEarthGuard(level, victim);
        }
    }

    // ==================== Ar: escudo de vento (repulsão + corte + sangramento) ====================

    private static final double AIR_RADIUS = 5.0;
    private static final float AIR_SLASH_DAMAGE = 6.0f;
    private static final double AIR_KNOCKBACK = 1.5;
    private static final int BLEEDING_DURATION_TICKS = 100; // 5s

    private static void triggerAirGuard(ServerLevel level, ServerPlayer victim) {
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.6f, 0.7f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                victim.getX(), victim.getY() + 1.0, victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                90, AIR_RADIUS * 0.4, 1.0, AIR_RADIUS * 0.4, 0.2);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                8, AIR_RADIUS * 0.35, 0.6, AIR_RADIUS * 0.35, 0.0);

        // Escudo é instantâneo (dá a rajada e some no mesmo tick) -- não
        // fica uma barreira física parada, é um "estouro" defensivo único.
        DamageSource slash = level.damageSources().playerAttack(victim);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                victim.getBoundingBox().inflate(AIR_RADIUS), e -> e != victim && e.isAlive())) {
            target.hurt(slash, AIR_SLASH_DAMAGE);
            target.addEffect(new MobEffectInstance(MoreBendingsEffects.BLEEDING, BLEEDING_DURATION_TICKS, 0));

            Vec3 away = target.position().subtract(victim.position());
            Vec3 direction = away.length() > 0.001 ? away.normalize() : new Vec3(0, 1, 0);
            Vec3 push = direction.scale(AIR_KNOCKBACK).add(0, 0.35, 0);
            target.push(push.x, push.y, push.z);
            target.hurtMarked = true;
        }
    }

    // ==================== Fogo: explosão de fogo azul ====================

    private static final double FIRE_RADIUS = 5.5;
    private static final float FIRE_EXPLOSION_DAMAGE = 9.0f;

    /**
     * O vanilla não tem "fogo azul" de verdade pra acender numa entidade
     * qualquer (a textura azul só existe no soul fire, que só pega em
     * blocos específicos) -- então o azul aqui vem só da partícula
     * ({@code SOUL_FIRE_FLAME} + esse dust customizado), enquanto o dano de
     * queimada em si usa {@code setRemainingFireTicks} normal (visual
     * laranja padrão na entidade acesa).
     */
    private static final DustParticleOptions BLUE_FLAME_DUST =
            new DustParticleOptions(new Vector3f(0.15f, 0.45f, 1.0f), 1.7f);

    private static void triggerFireGuard(ServerLevel level, ServerPlayer victim) {
        Vec3 center = victim.position().add(0, 1.0, 0);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.6f, 0.65f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z,
                110, FIRE_RADIUS * 0.4, 0.8, FIRE_RADIUS * 0.4, 0.06);
        level.sendParticles(BLUE_FLAME_DUST, center.x, center.y, center.z,
                70, FIRE_RADIUS * 0.35, 0.6, FIRE_RADIUS * 0.35, 0.02);

        AABB area = new AABB(center.x - FIRE_RADIUS, center.y - FIRE_RADIUS, center.z - FIRE_RADIUS,
                center.x + FIRE_RADIUS, center.y + FIRE_RADIUS, center.z + FIRE_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != victim && e.isAlive())) {
            double dist = center.distanceTo(target.position());
            double falloff = Math.max(0.0, 1.0 - dist / FIRE_RADIUS);
            float damage = (float) (FIRE_EXPLOSION_DAMAGE * falloff);
            if (damage < 0.5f) {
                continue;
            }
            target.hurt(level.damageSources().playerAttack(victim), damage);
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), (int) (100 * falloff)));

            Vec3 push = target.position().subtract(center);
            push = push.length() < 0.01 ? new Vec3(0, 1, 0) : push.normalize();
            double kb = 0.6 * falloff + 0.25;
            target.push(push.x * kb, Math.max(push.y * kb, 0.25), push.z * kb);
            target.hurtMarked = true;
        }
    }

    // ==================== Água: estilhaços de gelo + lâminas (congela + perfura + sangramento) ====================

    private static final double WATER_RADIUS = 5.0;
    private static final float WATER_PIERCE_DAMAGE = 6.0f;
    private static final int WATER_FREEZE_DURATION_TICKS = 70; // 3.5s

    private static void triggerWaterGuard(ServerLevel level, ServerPlayer victim) {
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.4f, 1.3f);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 0.9f);
        level.sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                100, WATER_RADIUS * 0.4, 0.8, WATER_RADIUS * 0.4, 0.08);
        level.sendParticles(ParticleTypes.SPLASH, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                50, WATER_RADIUS * 0.35, 0.5, WATER_RADIUS * 0.35, 0.12);

        DamageSource pierce = level.damageSources().freeze();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                victim.getBoundingBox().inflate(WATER_RADIUS), e -> e != victim && e.isAlive())) {
            target.hurt(pierce, WATER_PIERCE_DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WATER_FREEZE_DURATION_TICKS, 5));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze()));
            target.addEffect(new MobEffectInstance(MoreBendingsEffects.BLEEDING, BLEEDING_DURATION_TICKS, 0));
            target.hurtMarked = true;
        }
    }

    // ==================== Terra: espinhos teleguiados (um por entidade mais próxima) ====================

    private static final int EARTH_SPIKE_TARGET_COUNT = 4;
    private static final double EARTH_SEARCH_RADIUS = 10.0;
    private static final float EARTH_SPIKE_DAMAGE = 7.0f;
    private static final int EARTH_TRAVEL_STEPS = 8; // ticks pro espinho "guiado" alcançar o alvo
    private static final int EARTH_STAGGER_TICKS = 3; // atraso entre um espinho disparar e o próximo

    private static final BlockState[] EARTH_GUARD_BLOCKS = {
            Blocks.STONE.defaultBlockState(),
            Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(),
    };

    /**
     * Diferente dos outros 3 arcos (instantâneos), este usa o {@link
     * AvatarFxScheduler} pra dar de fato uma sensação de "teleguiado":
     * a cada passo da animação a posição ATUAL do alvo é reconsultada (não
     * um ponto fixo salvo no início), então o espinho visivelmente
     * persegue/corrige o rumo se o alvo se mover antes do impacto. Um
     * espinho por entidade viva mais próxima (até {@link
     * #EARTH_SPIKE_TARGET_COUNT}), disparados em sequência escalonada, não
     * todos juntos.
     */
    private static void triggerEarthGuard(ServerLevel level, ServerPlayer victim) {
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.1f, 1.4f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                victim.getX(), victim.getY() + 0.2, victim.getZ(), 30, 1.0, 0.2, 1.0, 0.1);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                victim.getBoundingBox().inflate(EARTH_SEARCH_RADIUS), e -> e != victim && e.isAlive());
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(victim)));

        int count = Math.min(EARTH_SPIKE_TARGET_COUNT, targets.size());
        for (int i = 0; i < count; i++) {
            final LivingEntity target = targets.get(i);
            int startDelay = i * EARTH_STAGGER_TICKS;

            AvatarFxScheduler.schedule(startDelay, () -> {
                level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
                for (int step = 1; step <= EARTH_TRAVEL_STEPS; step++) {
                    final int s = step;
                    AvatarFxScheduler.schedule(step - 1, () -> guideEarthSpikeStep(level, victim, target, s));
                }
            });
        }
    }

    private static void guideEarthSpikeStep(ServerLevel level, ServerPlayer victim, LivingEntity target, int step) {
        if (!target.isAlive() || target.isRemoved()) {
            return; // alvo sumiu/morreu no meio da perseguição -- só desiste desse espinho
        }

        double progress = (double) step / EARTH_TRAVEL_STEPS;
        Vec3 from = victim.position().add(0, 0.3, 0);
        // Reconsulta a posição ATUAL do alvo a cada passo -- é isso que faz
        // o espinho parecer "guiado" em vez de mirar num ponto fixo do
        // início da animação.
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 point = from.lerp(to, progress);

        BlockState block = EARTH_GUARD_BLOCKS[ThreadLocalRandom.current().nextInt(EARTH_GUARD_BLOCKS.length)];
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block),
                point.x, point.y, point.z, 6, 0.2, 0.2, 0.2, 0.02);

        if (step != EARTH_TRAVEL_STEPS) {
            return;
        }

        // Impacto -- último passo da perseguição.
        target.hurt(level.damageSources().playerAttack(victim), EARTH_SPIKE_DAMAGE);
        target.hurtMarked = true;
        Vec3 push = target.position().subtract(victim.position());
        push = push.length() < 0.01 ? new Vec3(0, 1, 0) : push.normalize();
        target.push(push.x * 0.4, 0.35, push.z * 0.4);

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block),
                point.x, point.y, point.z, 16, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, point.x, point.y, point.z,
                SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
