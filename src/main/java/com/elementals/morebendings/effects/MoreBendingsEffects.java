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
            EFFECTS.register("Crushed", CrushedStatusEffect::new);

    private MoreBendingsEffects() {}

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}