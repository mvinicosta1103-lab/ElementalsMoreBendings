package com.elementals.morebendings.bending.watersubbendings.spirit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todas as maldições de {@code curseMinion} ativas no servidor --
 * uma por vítima amaldiçoada (chaveada pelo UUID do {@link Mob}, não do
 * caster, já que vários casters podem amaldiçoar mobs diferentes ao mesmo
 * tempo). Dirigido por {@link ServerTickEvent.Post}, registrado em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod}.
 *
 * COMPORTAMENTO: o mob amaldiçoado vira efetivamente um aliado do caster
 * enquanto a maldição durar --
 *
 *  - Ao ser amaldiçoado, para imediatamente de atacar o caster (se já
 *    estava fazendo isso) e passa a caçar outros mobs/players hostis por
 *    perto, alternando de alvo entre eles periodicamente.
 *  - O caster NUNCA é um alvo válido -- se a IA vanilla da criatura tentar
 *    voltar a mirar nele (ex: um Zombie que tem goal própria de perseguir
 *    o jogador mais perto), o Manager detecta e limpa o alvo em todo tick,
 *    não só no intervalo de retarget.
 *  - Se não houver ninguém por perto pra atacar, o alvo simplesmente fica
 *    null (a criatura fica parada/neutra) até aparecer alguém ou a
 *    maldição acabar.
 */
public final class CurseMinionManager {

    static final int CURSE_DURATION_TICKS = 200; // 10s
    private static final int RETARGET_INTERVAL_TICKS = 30; // 1.5s
    private static final double RETARGET_RADIUS = 12.0;

    private static final Map<UUID, Curse> ACTIVE = new HashMap<>();

    private CurseMinionManager() {
    }

    public static void curse(ServerLevel level, ServerPlayer caster, Mob victim) {
        Curse curse = new Curse(level, caster.getUUID());
        ACTIVE.put(victim.getUUID(), curse);

        // Para de atacar o caster imediatamente, se já estava.
        if (victim.getTarget() == caster) {
            victim.setTarget(null);
        }
        // Já tenta escolher um alvo aliado-friendly (outro mob/player) na hora.
        retarget(curse, victim, caster);
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Curse>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Curse> entry = it.next();
            Curse curse = entry.getValue();

            Mob victim = findMob(curse.level, entry.getKey());
            if (victim == null || !victim.isAlive()) {
                it.remove();
                continue;
            }

            ServerPlayer caster = curse.level.getServer().getPlayerList().getPlayer(curse.casterId);

            curse.remainingTicks--;
            if (curse.remainingTicks <= 0) {
                it.remove();
                continue; // maldição acaba -- IA vanilla volta ao normal sozinha
            }

            // Todo tick: se por acaso o alvo virou o caster de novo (goal vanilla
            // reagindo a ele estar perto/atacando), limpa na hora.
            if (caster != null && victim.getTarget() == caster) {
                victim.setTarget(null);
            }

            curse.ticksUntilRetarget--;
            if (curse.ticksUntilRetarget <= 0 || (victim.getTarget() == null || !victim.getTarget().isAlive())) {
                curse.ticksUntilRetarget = RETARGET_INTERVAL_TICKS;
                retarget(curse, victim, caster);
            }
        }
    }

    /** Escolhe um novo alvo entre mobs/players próximos, excluindo sempre o caster. */
    private static void retarget(Curse curse, Mob victim, ServerPlayer caster) {
        AABB area = new AABB(victim.position(), victim.position()).inflate(RETARGET_RADIUS);
        List<LivingEntity> nearby = curse.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != victim && entity != caster && entity.isAlive()
                        && (entity instanceof Player || entity instanceof Mob));

        if (nearby.isEmpty()) {
            victim.setTarget(null); // ninguém por perto -- fica parado, sem atacar o caster
            return;
        }

        LivingEntity newTarget = nearby.get(victim.getRandom().nextInt(nearby.size()));
        victim.setTarget(newTarget);
        curse.level.sendParticles(ParticleTypes.ANGRY_VILLAGER, victim.getX(), victim.getY() + victim.getBbHeight() * 0.8,
                victim.getZ(), 4, 0.25, 0.2, 0.25, 0.0);
        curse.level.playSound(null, victim.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.4f, 1.6f);
    }

    private static Mob findMob(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof Mob mob ? mob : null;
    }

    private static final class Curse {
        final ServerLevel level;
        final UUID casterId;
        int remainingTicks = CURSE_DURATION_TICKS;
        int ticksUntilRetarget = RETARGET_INTERVAL_TICKS;

        Curse(ServerLevel level, UUID casterId) {
            this.level = level;
            this.casterId = casterId;
        }
    }
}