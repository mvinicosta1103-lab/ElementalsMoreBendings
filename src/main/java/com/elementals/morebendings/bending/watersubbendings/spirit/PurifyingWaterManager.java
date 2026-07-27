package com.elementals.morebendings.bending.watersubbendings.spirit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as capturas de {@code purifyingWater} ativas no servidor --
 * uma por vítima (não por caster, já que a mesma ativação pode pegar várias
 * criaturas de uma vez). Roda de forma independente do sistema de onTick do
 * mod base, dirigido pelo listener registrado em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod} no
 * NeoForge.EVENT_BUS ({@link ServerTickEvent.Post}) -- mesmo esquema do
 * {@code MudTrapManager}.
 *
 * REGRAS DE RESOLUÇÃO (ver {@link #resolve}), decididas a partir da
 * descrição original da ability -- ela cita Wither Skeleton nos dois
 * grupos (mortos-vivos "menores" que se dissolvem E como virando um Snow
 * Golem), o que é contraditório; aqui ele SÓ vira Snow Golem, já que essa
 * é a regra mais específica das duas:
 *
 *  - Skeleton, Zombie bebê, Husk, Blaze -> dissolvem (remoção silenciosa
 *    + partículas de "poeira espiritual", sem drop/mensagem de morte).
 *  - Witch, Zombie Villager (bebê ou adulto), Pillager, Vindicator ->
 *    viram Villager normal (preservando idade, via {@link Mob#convertTo}).
 *  - Wither Skeleton -> vira Snow Golem.
 *  - Iron Golem com vida menor que o máximo -> curado por completo (sem
 *    conversão nenhuma).
 *  - Qualquer outra criatura (ou um Iron Golem já com vida cheia) -> a
 *    captura simplesmente expira sem efeito nenhum.
 */
public final class PurifyingWaterManager {

    /** Tempo (em ticks) que a vítima fica brilhando/presa antes da resolução. */
    static final int CATCH_DELAY_TICKS = 60; // 3s

    private static final Map<UUID, Catch> ACTIVE = new HashMap<>();

    private PurifyingWaterManager() {
    }

    /** @return true se a vítima foi capturada agora (false se já estava capturada). */
    static boolean tryCatch(ServerLevel level, LivingEntity victim) {
        if (ACTIVE.containsKey(victim.getUUID())) {
            return false;
        }
        ACTIVE.put(victim.getUUID(), new Catch(level, victim, CATCH_DELAY_TICKS));
        return true;
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Catch>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Catch c = it.next().getValue();
            LivingEntity victim = c.victim;

            if (!victim.isAlive()) {
                it.remove();
                continue;
            }
            if (!victim.isInWaterOrBubble()) {
                // Saiu da água antes do tempo -- captura cancelada, sem efeito.
                it.remove();
                continue;
            }

            c.remainingTicks--;
            if (c.remainingTicks <= 0) {
                resolve(c.level, victim);
                it.remove();
            }
        }
    }

    private static void resolve(ServerLevel level, LivingEntity victim) {
        if (victim instanceof IronGolem golem) {
            if (golem.getHealth() < golem.getMaxHealth()) {
                golem.setHealth(golem.getMaxHealth());
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + golem.getBbHeight() * 0.5,
                        golem.getZ(), 16, 0.4, 0.5, 0.4, 0.0);
                level.playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
            return; // sem conversão -- só conserta
        }

        if (!(victim instanceof Mob mob)) {
            return; // captura expira sem efeito em qualquer coisa que não seja um Mob
        }

        if (mob instanceof WitherSkeleton) {
            convertTo(level, mob, EntityType.SNOW_GOLEM, false);
        } else if (mob instanceof Witch || mob instanceof Pillager || mob instanceof Vindicator || mob instanceof ZombieVillager) {
            convertTo(level, mob, EntityType.VILLAGER, true);
        } else if (mob instanceof Skeleton || mob instanceof Husk || mob instanceof Blaze
                || (mob instanceof Zombie && mob.isBaby() && !(mob instanceof ZombieVillager))) {
            dissolve(level, mob);
        }
        // qualquer outra criatura: sem efeito, captura só expira.
    }

    /** Remoção silenciosa (sem drop/mensagem de morte) + partículas de poeira espiritual. */
    private static void dissolve(ServerLevel level, Mob mob) {
        level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                24, 0.3, 0.4, 0.3, 0.03);
        level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                6, 0.2, 0.3, 0.2, 0.01);
        level.playSound(null, mob.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.0f, 1.0f);
        mob.discard();
    }

    /** Converte preservando idade (bebê) quando o resultado também é um {@link AgeableMob}. */
    private static void convertTo(ServerLevel level, Mob mob, EntityType<?> target, boolean preserveBaby) {
        boolean wasBaby = preserveBaby && mob.isBaby();
        @SuppressWarnings("unchecked")
        Mob converted = mob.convertTo((EntityType<? extends Mob>) target, true);
        if (converted == null) {
            return;
        }
        if (wasBaby && converted instanceof AgeableMob ageable) {
            ageable.setBaby(true);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, converted.getX(), converted.getY() + converted.getBbHeight() * 0.5,
                converted.getZ(), 16, 0.4, 0.5, 0.4, 0.0);
        level.playSound(null, converted.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.7f, 1.4f);
    }

    private static final class Catch {
        final ServerLevel level;
        final LivingEntity victim;
        int remainingTicks;

        Catch(ServerLevel level, LivingEntity victim, int remainingTicks) {
            this.level = level;
            this.victim = victim;
            this.remainingTicks = remainingTicks;
        }
    }
}