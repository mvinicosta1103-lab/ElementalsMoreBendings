package com.elementals.morebendings.effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MoreBendingsEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, "elementalsmorebendings");

    public static final DeferredHolder<MobEffect, CrushedStatusEffect> CRUSHED =
            EFFECTS.register("crushed", CrushedStatusEffect::new);

    // Usado pelo burst defensivo do Avatar perto de morrer -- ver
    // AvatarNearDeathGuardian (arcos de Ar e Água aplicam isso).
    public static final DeferredHolder<MobEffect, BleedingStatusEffect> BLEEDING =
            EFFECTS.register("bleeding", BleedingStatusEffect::new);

    private MoreBendingsEffects() {}

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}