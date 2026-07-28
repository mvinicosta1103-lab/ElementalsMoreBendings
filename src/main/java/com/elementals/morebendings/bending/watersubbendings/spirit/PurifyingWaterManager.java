package com.elementals.morebendings.bending.watersubbendings.spirit;

import dev.saperate.elementals.elements.water.WaterElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
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
 * O QUE CONTA COMO "ÁGUA" (ver {@link #hasNaturalEnvironment}):
 *
 *  - A vítima estar em água/bolha, ou sendo molhada por chuva.
 *  - Um bloco de neve, gelo (todas as variantes -- inclusive Powder Snow
 *    e os dois caldeirões), kelp, grama/folhagem alta ou baixa, cacto, ou
 *    FOLHAS DE ÁRVORE em volta dela -- essa é a mesma lista que o mod
 *    base ({@code WaterElement#isBlockBendable}, com
 *    {@code canUseDiverseBlocks=true}) usa pras outras habilidades de
 *    Water considerarem um bloco "bendável" fora d'água de verdade.
 *  - FLORES (qualquer bloco na tag {@code minecraft:flowers}) -- não
 *    entram na lista do mod base, então são checadas à parte aqui.
 *
 * Se nada disso for encontrado num raio pequeno ao redor da vítima
 * ({@link #ENV_RADIUS}), a captura tenta um fallback: puxar 1 unidade de
 * água reservada do CASTER via {@link WaterElement#tryRetrieveWater}
 * (funciona com Water Pouch cheio OU um vidro de água comum -- mesmo
 * método usado pelo resto do mod base) e conjurar uma poça temporária
 * embaixo da vítima com {@link WaterElement#placeWater}. O bloco
 * original é salvo e restaurado quando a captura acaba (sucesso ou
 * cancelamento) -- ver {@link Catch#puddlePos}.
 *
 * Fluxo completo de uma captura, depois de aceita:
 *
 *  1. TRAVAR — a vítima é travada no lugar (posição fixa a cada tick +
 *     {@code setNoAi(true)} se for um {@link Mob}) e ganha Glowing.
 *  2. SUBIDA D'ÁGUA -- a cada tick, uma coluna de partículas de bolha/água
 *     sobe do chão até o topo da hitbox, ficando mais densa e mais
 *     "brilhante" conforme o tempo passa -- ver {@link #risingWaterEffect}.
 *  3. RESOLUÇÃO -- se o ambiente continuar válido até o fim de {@link
 *     #CATCH_DELAY_TICKS}, {@link #resolve} decide o que acontece; se
 *     deixar de ser válido antes (alguém quebrou a água/poça, por
 *     exemplo), {@link #cancelCatch} solta a vítima sem efeito.
 *
 * REGRAS DE RESOLUÇÃO -- ela cita Wither Skeleton nos dois grupos (mortos-
 * vivos "menores" que se dissolvem E como virando Snow Golem), o que é
 * contraditório; aqui ele SÓ vira Snow Golem, regra mais específica:
 *
 */
public final class PurifyingWaterManager {

    /** Tempo (em ticks) que a vítima fica travada/brilhando antes da resolução. */
    static final int CATCH_DELAY_TICKS = 60; // 3s

    /** Duração (em ticks) da animação de dissolução final, depois do CATCH_DELAY_TICKS. */
    private static final int DISSOLVE_TICKS = 16; // ~0.8s

    /** Raio (em blocos, horizontal e vertical) checado ao redor da vítima por neve/gelo/folhagem/etc. */
    private static final int ENV_RADIUS = 2;

    private static final Map<UUID, Catch> ACTIVE = new HashMap<>();

    private PurifyingWaterManager() {
    }

    /** @return true se a vítima foi capturada agora (false se já estava capturada ou não havia água disponível). */
    static boolean tryCatch(ServerLevel level, ServerPlayer caster, LivingEntity victim) {
        if (ACTIVE.containsKey(victim.getUUID())) {
            return false;
        }

        boolean hadAi = !(victim instanceof Mob mob) || !mob.isNoAi();
        Catch c = new Catch(level, victim, hadAi);

        if (!hasNaturalEnvironment(level, victim)) {
            // Sem água/neve/folhagem natural por perto -- tenta puxar do caster.
            if (!WaterElement.tryRetrieveWater(caster)) {
                return false; // nenhuma fonte disponível -- não captura
            }
            conjurePuddle(level, victim.blockPosition(), c);
        }

        if (victim instanceof Mob mob) {
            mob.setNoAi(true);
        }
        ACTIVE.put(victim.getUUID(), c);
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
                restorePuddle(c);
                it.remove();
                continue;
            }

            if (c.dissolving) {
                dissolveStep(c);
                if (c.dissolveTicks <= 0) {
                    restorePuddle(c);
                    victim.discard();
                    it.remove();
                }
                continue;
            }

            if (!isEnvironmentStillValid(c)) {
                // Ambiente deixou de valer (água/poça sumiu) -- captura cancelada, sem efeito.
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

    /**
     * @return true se a vítima está em água/chuva/bolha, ou se há neve, gelo,
     * kelp, grama/folhagem, cacto ou folhas de árvore num raio pequeno ao
     * redor dela -- mesma lista de blocos "bendáveis" do mod base, mais
     * flores checadas à parte.
     */
    private static boolean hasNaturalEnvironment(ServerLevel level, LivingEntity victim) {
        if (victim.isInWaterOrBubble() || victim.isInWaterRainOrBubble()) {
            return true;
        }

        BlockPos base = victim.blockPosition();
        int height = Math.max(1, (int) Math.ceil(victim.getBbHeight()));
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -ENV_RADIUS; x <= ENV_RADIUS; x++) {
            for (int z = -ENV_RADIUS; z <= ENV_RADIUS; z++) {
                for (int y = -1; y <= height; y++) {
                    mutable.set(base.getX() + x, base.getY() + y, base.getZ() + z);
                    if (WaterElement.isBlockBendable(mutable, level, false, true)) {
                        return true;
                    }
                    BlockState state = level.getBlockState(mutable);
                    if (state.is(BlockTags.FLOWERS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** @return true se a captura ainda pode continuar -- ambiente natural OU poça conjurada ainda presente. */
    private static boolean isEnvironmentStillValid(Catch c) {
        if (hasNaturalEnvironment(c.level, c.victim)) {
            return true;
        }
        return c.puddlePos != null && c.level.getFluidState(c.puddlePos).is(Fluids.WATER);
    }

    /**
     * Conjura uma poça temporária na posição informada usando {@link
     * WaterElement#placeWater}, salvando o bloco original em {@code c}
     * pra restaurar depois. No Nether o mod base trata água como "sempre
     * disponível" sem colocar bloco nenhum (ver {@link
     * WaterElement#placeWater}), então nesse caso não há nada pra
     * restaurar.
     */
    private static void conjurePuddle(ServerLevel level, BlockPos pos, Catch c) {
        if (level.dimension().equals(Level.NETHER)) {
            return;
        }
        BlockState saved = level.getBlockState(pos);
        if (WaterElement.placeWater(pos, level)) {
            c.puddlePos = pos.immutable();
            c.savedPuddleState = saved;
        }
    }

    /** Restaura o bloco original onde uma poça foi conjurada, se houver uma ativa. */
    private static void restorePuddle(Catch c) {
        if (c.puddlePos != null && c.savedPuddleState != null) {
            c.level.setBlock(c.puddlePos, c.savedPuddleState, 3);
            c.puddlePos = null;
            c.savedPuddleState = null;
        }
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

    /** Solta a vítima sem efeito -- restaura noAi e a poça conjurada (se houver). */
    private static void cancelCatch(Catch c) {
        if (c.hadAi && c.victim instanceof Mob mob) {
            mob.setNoAi(false);
        }
        restorePuddle(c);
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
            convertTo(c, EntityType.SNOW_GOLEM, false);
            return false;
        }
        if (mob instanceof Witch || mob instanceof Pillager || mob instanceof Vindicator || mob instanceof ZombieVillager) {
            convertTo(c, EntityType.VILLAGER, true);
            return false;
        }
        if (mob instanceof Skeleton || mob instanceof Husk || mob instanceof Blaze
                || (mob instanceof Zombie && mob.isBaby() && !(mob instanceof ZombieVillager))) {
            // Inicia a dissolução -- o Catch continua ativo (mantém noAi/poça) até discard().
            c.dissolving = true;
            c.dissolveTicks = DISSOLVE_TICKS;
            return true;
        }

        // qualquer outra criatura: sem efeito, captura só expira.
        cancelCatch(c);
        return false;
    }

    /** Converte preservando idade (bebê) quando o resultado também é um {@link AgeableMob}. */
    private static void convertTo(Catch c, EntityType<?> target, boolean preserveBaby) {
        Mob mob = (Mob) c.victim;
        ServerLevel level = c.level;

        boolean wasBaby = preserveBaby && mob.isBaby();
        mob.setNoAi(false); // restaura antes de converter, pro entity novo não nascer travado
        restorePuddle(c);

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

        /** Não-nulo se uma poça temporária foi conjurada com água do caster (ver {@link #conjurePuddle}). */
        BlockPos puddlePos;
        BlockState savedPuddleState;

        Catch(ServerLevel level, LivingEntity victim, boolean hadAi) {
            this.level = level;
            this.victim = victim;
            this.hadAi = hadAi;
            this.remainingTicks = CATCH_DELAY_TICKS;
        }
    }
}