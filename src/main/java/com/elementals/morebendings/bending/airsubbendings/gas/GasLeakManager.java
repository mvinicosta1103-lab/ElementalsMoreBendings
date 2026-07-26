package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Dono de todas as nuvens residuais de {@link GasLeakAbility} ativas no
 * servidor. Roda de forma independente do sistema de onTick do mod base --
 * dirigido pelo listener registrado em
 * {@link com.elementals.morebendings.ElementalsMoreBendingsMod} no
 * NeoForge.EVENT_BUS ({@link ServerTickEvent.Post}), mesmo esquema do
 * {@code LavaPoolManager}/{@code MudTrapManager}.
 *
 * Motivo de existir: {@link AreaEffectCloud} vanilla aplica efeito em
 * QUALQUER LivingEntity dentro do raio, incluindo o owner/thrower. Como a
 * regra do Gas é "o dobrador nunca é afetado pelo próprio gás", a
 * aplicação de efeito não pode ficar por conta da cloud -- é feita aqui,
 * manualmente, todo tick, escaneando entidades vivas dentro do raio da
 * nuvem e pulando o caster.
 */
public final class GasLeakManager {

    private static final int EFFECT_DURATION_TICKS = 40; // reforçado a cada tick enquanto estiver na nuvem
    private static final List<Entry> ACTIVE = new ArrayList<>();

    private GasLeakManager() {
    }

    public static void register(ServerLevel level, AreaEffectCloud cloud, ServerPlayer caster) {
        ACTIVE.add(new Entry(level, cloud, caster.getUUID()));
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Entry> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (!entry.cloud.isAlive() || entry.cloud.isRemoved()) {
                it.remove();
                continue;
            }
            entry.tick();
        }
    }

    private static final class Entry {
        final ServerLevel level;
        final AreaEffectCloud cloud;
        final UUID casterUuid;

        Entry(ServerLevel level, AreaEffectCloud cloud, UUID casterUuid) {
            this.level = level;
            this.cloud = cloud;
            this.casterUuid = casterUuid;
        }

        void tick() {
            double radius = cloud.getRadius();
            AABB area = cloud.getBoundingBox().inflate(radius);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity.isAlive() && !entity.getUUID().equals(casterUuid)
                            && cloud.distanceToSqr(entity) <= radius * radius);
            for (LivingEntity entity : nearby) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, EFFECT_DURATION_TICKS, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, EFFECT_DURATION_TICKS, 0));
            }
        }
    }
}