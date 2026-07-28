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
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as capturas de {@code purifyingWater} ativas no servidor --
 * uma por vítima (não por caster, já que a mesma ativação pode pegar várias
 * criaturas de uma vez). Dirigido por {@link ServerTickEvent.Post},
 * registrado em {@link com.elementals.morebendings.ElementalsMoreBendingsMod}.
 *
 * Fluxo completo de uma captura:
 *
 *  1. TRAVAR — assim que pega a vítima, ela é travada no lugar (posição
 *     fixa a cada tick + {@code setNoAi(true)} se for um {@link Mob}) e
 *     ganha Glowing. Ela literalmente "para" -- não anda, não ataca, não
 *     foge -- enquanto durar a captura.
 *  2. SUBIDA D'ÁGUA -- a cada tick, uma coluna de partículas de bolha/água
 *     sobe do chão até o topo da hitbox, ficando mais densa e mais
 *     "brilhante" (mistura de bolha + faísca) conforme o tempo passa --
 *     ver {@link #risingWaterEffect}.
 *  3. RESOLUÇÃO -- se a vítima continuar na água até o fim de {@link
 *     #CATCH_DELAY_TICKS}, {@link #resolve} decide o que acontece; se
 *     sair da água antes, {@link #cancelCatch} solta ela sem efeito
 *     (restaura {@code noAi}).
 *
 * REGRAS DE RESOLUÇÃO -- ela cita Wither Skeleton nos dois grupos (mortos-
 * vivos "menores" que se dissolvem E como virando Snow Golem), o que é
 * contraditório; aqui ele SÓ vira Snow Golem, regra mais específica:
 *
 *  - Skeleton, Zombie bebê, Husk, Blaze -> DISSOLVEM: antes de sumir de
 *    verdade, passam por {@link #dissolveEffect} -- uma explosão crescente
 *    de partículas de alma/luz em volta do corpo, dando a sensação de que
 *    o mob está se desfazendo -- só então é removido (sem drop/mensagem).
 *  - Witch, Zombie Villager (bebê ou adulto), Pillager, Vindicator ->
 *    viram Villager normal (preservando idade, via {@link Mob#convertTo}).
 *  - Wither Skeleton -> vira Snow Golem.
 *  - Iron Golem com vida menor que o máximo -> curado por completo (sem
 *    conversão).
 *  - Qualquer outra criatura (ou Iron Golem já com vida cheia) -> a
 *    captura só expira, mob é solto (noAi restaurado) sem efeito nenhum.
 */
public final class PurifyingWaterManager {

    /** Tempo (em ticks) que a vítima fica travada/brilhando antes da resolução. */
    static final int CATCH_DELAY_TICKS = 60; // 3s

    /** Duração (em ticks) da animação de dissolução final, depois do CATCH_DELAY_TICKS. */
    private static final int DISSOLVE_TICKS = 16; // ~0.8s

    private static final Map<UUID, Catch> ACTIVE = new HashMap<>();

    private PurifyingWaterManager() {
    }

    /** @return true se a vítima foi capturada agora (false se já estava capturada). */
    static boolean tryCatch(ServerLevel level, LivingEntity victim) {
        if (ACTIVE.containsKey(victim.getUUID())) {
            return false;
        }
        boolean hadAi = !(victim instanceof Mob mob) || !mob.isNoAi();
        if (victim instanceof Mob mob) {
            mob.setNoAi(true);
        }
        ACTIVE.put(victim.getUUID(), new Catch(level, victim, hadAi));
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

            if (c.dissolving) {
                dissolveStep(c);
                if (c.dissolveTicks <= 0) {
                    victim.discard();
                    it.remove();
                }
                continue;
            }

            if (!victim.isInWaterOrBubble()) {
                // Saiu da água antes do tempo -- captura cancelada, sem efeito.
                cancelCatch(c);
                it.remove();
                continue;
            }

            holdInPlace(victim);
            double progress = 1.0 - (c.remainingTicks / (double) CATCH_DELAY_TICKS);
            risingWaterEffect(c.level, victim, progress);

            c.remainingTicks--;
            if (c.remainingTicks <= 0) {
                if (!resolve(c)) {
                    // não precisa dissolver (curou/converteu/não fez nada) -- libera já
                    it.remove();
                }
                // se resolve() iniciou a dissolução, o Catch continua ativo (c.dissolving = true)
            }
        }
    }

    /** Trava a vítima na posição atual, sem deixar ela se mexer nem cair. */
    private static void holdInPlace(LivingEntity victim) {
        victim.setDeltaMovement(Vec3.ZERO);
        victim.fallDistance = 0;
        victim.hasImpulse = false;
    }

    /** Coluna de bolhas subindo + brilho crescente ao redor da vítima. */
    private static void risingWaterEffect(ServerLevel level, LivingEntity victim, double progress) {
        double height = victim.getBbHeight();
        double topY = victim.getY() + height * Math.min(1.0, progress + 0.15);

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_COLUMN_UP,
                victim.getX(), victim.getY(), victim.getZ(), 1, 0.25, 0.0, 0.25, 0.0);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                victim.getX(), topY, victim.getZ(), 2 + (int) (progress * 4), 0.3, 0.1, 0.3, 0.02);

        // Brilho crescente: mais partículas de luz conforme se aproxima da resolução.
        int glowCount = 1 + (int) (progress * 6);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                victim.getX(), victim.getY() + height * 0.5, victim.getZ(),
                glowCount, 0.25, height * 0.4, 0.25, 0.01);

        if (victim.tickCount % 10 == 0) {
            level.playSound(null, victim.blockPosition(), SoundEvents.CONDUIT_AMBIENT,
                    SoundSource.NEUTRAL, 0.4f, 1.0f + (float) progress * 0.5f);
        }
    }

    /** Animação de "sumindo" -- usada antes de discard() em mobs que dissolvem. */
    private static void dissolveStep(Catch c) {
        LivingEntity victim = c.victim;
        holdInPlace(victim);
        double progress = 1.0 - (c.dissolveTicks / (double) DISSOLVE_TICKS);
        double height = victim.getBbHeight();

        int count = 4 + (int) (progress * 20); // cresce até a explosão final
        c.level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                victim.getX(), victim.getY() + height * 0.5, victim.getZ(), count, 0.3, 0.4, 0.3, 0.03 + progress * 0.05);
        c.level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                victim.getX(), victim.getY() + height * 0.5, victim.getZ(), count / 3, 0.25, 0.35, 0.25, 0.02);

        if (c.dissolveTicks == DISSOLVE_TICKS) {
            c.level.playSound(null, victim.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 1.0f, 1.0f);
        }
        c.dissolveTicks--;
    }

    /** Solta a vítima sem efeito -- restaura noAi. */
    private static void cancelCatch(Catch c) {
        if (c.hadAi && c.victim instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    /**
     * @return true se a captura deve continuar ativa (entrou em fase de dissolução);
     * false se já pode ser removida do mapa agora (curou, converteu, ou não fez nada).
     */
    private static boolean resolve(Catch c) {
        LivingEntity victim = c.victim;
        ServerLevel level = c.level;

        if (victim instanceof IronGolem golem) {
            if (golem.getHealth() < golem.getMaxHealth()) {
                golem.setHealth(golem.getMaxHealth());
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + golem.getBbHeight() * 0.5,
                        golem.getZ(), 16, 0.4, 0.5, 0.4, 0.0);
                level.playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
            cancelCatch(c);
            return false;
        }

        if (!(victim instanceof Mob mob)) {
            cancelCatch(c);
            return false; // captura expira sem efeito em qualquer coisa que não seja um Mob
        }

        if (mob instanceof WitherSkeleton) {
            convertTo(level, mob, EntityType.SNOW_GOLEM, false);
            return false;
        }
        if (mob instanceof Witch || mob instanceof Pillager || mob instanceof Vindicator || mob instanceof ZombieVillager) {
            convertTo(level, mob, EntityType.VILLAGER, true);
            return false;
        }
        if (mob instanceof Skeleton || mob instanceof Husk || mob instanceof Blaze
                || (mob instanceof Zombie && mob.isBaby() && !(mob instanceof ZombieVillager))) {
            // Inicia a dissolução -- o Catch continua ativo (mantém noAi) até discard().
            c.dissolving = true;
            c.dissolveTicks = DISSOLVE_TICKS;
            return true;
        }

        // qualquer outra criatura: sem efeito, captura só expira.
        cancelCatch(c);
        return false;
    }

    /** Converte preservando idade (bebê) quando o resultado também é um {@link AgeableMob}. */
    private static void convertTo(ServerLevel level, Mob mob, EntityType<?> target, boolean preserveBaby) {
        boolean wasBaby = preserveBaby && mob.isBaby();
        mob.setNoAi(false); // restaura antes de converter, pro entity novo não nascer travado
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
        final boolean hadAi;
        int remainingTicks;
        boolean dissolving = false;
        int dissolveTicks = 0;

        Catch(ServerLevel level, LivingEntity victim, boolean hadAi) {
            this.level = level;
            this.victim = victim;
            this.hadAi = hadAi;
            this.remainingTicks = CATCH_DELAY_TICKS;
        }
    }
}