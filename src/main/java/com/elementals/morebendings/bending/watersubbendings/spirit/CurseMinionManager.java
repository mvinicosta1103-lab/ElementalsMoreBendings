package com.elementals.morebendings.bending.watersubbendings.spirit;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
 * tempo). Mesmo esquema de {@code MudTrapManager} / {@code
 * PurifyingWaterManager}: dirigido por {@link ServerTickEvent.Post},
 * registrado em {@link com.elementals.morebendings.ElementalsMoreBendingsMod}.
 *
 * A cada {@link #RETARGET_INTERVAL_TICKS}, a vítima troca de alvo entre o
 * caster e qualquer outro mob/player vivo dentro de {@link #RETARGET_RADIUS}
 * blocos dela -- é isso que produz o "ataca você E os outros mobs/players
 * por perto" da descrição original, em vez de travar num único alvo pelo
 * resto da duração.
 */
public final class CurseMinionManager {

    static final int CURSE_DURATION_TICKS = 200; // 10s
    private static final int RETARGET_INTERVAL_TICKS = 30; // 1.5s
    private static final double RETARGET_RADIUS = 10.0;

    private static final Map<UUID, Curse> ACTIVE = new HashMap<>();

    private CurseMinionManager() {
    }

    public static void curse(ServerLevel level, ServerPlayer caster, Mob victim) {
        Curse curse = new Curse(level, caster.getUUID(), CURSE_DURATION_TICKS);
        ACTIVE.put(victim.getUUID(), curse);
        victim.setTarget(caster); // primeiro alvo é sempre o caster
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

            curse.remainingTicks--;
            if (curse.remainingTicks <= 0) {
                it.remove();
                continue;
            }

            curse.ticksUntilRetarget--;
            if (curse.ticksUntilRetarget <= 0) {
                curse.ticksUntilRetarget = RETARGET_INTERVAL_TICKS;
                retarget(curse, victim);
            }
        }
    }

    private static void retarget(Curse curse, Mob victim) {
        ServerPlayer caster = curse.level.getServer().getPlayerList().getPlayer(curse.casterId);

        AABB area = new AABB(victim.position(), victim.position()).inflate(RETARGET_RADIUS);
        List<LivingEntity> nearby = curse.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != victim && entity.isAlive()
                        && (entity instanceof net.minecraft.world.entity.player.Player || entity instanceof Mob));

        LivingEntity newTarget;
        if (nearby.isEmpty()) {
            newTarget = caster; // ninguém por perto -- volta a atacar o caster
        } else {
            int index = victim.getRandom().nextInt(nearby.size() + 1);
            newTarget = index == nearby.size() ? caster : nearby.get(index);
        }

        if (newTarget == null || !newTarget.isAlive()) {
            return;
        }

        victim.setTarget(newTarget);
        curse.level.sendParticles(ParticleTypes.ANGRY_VILLAGER, victim.getX(), victim.getY() + victim.getBbHeight() * 0.8,
                victim.getZ(), 4, 0.25, 0.2, 0.25, 0.0);
    }

    private static Mob findMob(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof Mob mob ? mob : null;
    }

    private static final class Curse {
        final ServerLevel level;
        final UUID casterId;
        int remainingTicks;
        int ticksUntilRetarget = RETARGET_INTERVAL_TICKS;

        Curse(ServerLevel level, UUID casterId, int remainingTicks) {
            this.level = level;
            this.casterId = casterId;
            this.remainingTicks = remainingTicks;
        }
    }
}