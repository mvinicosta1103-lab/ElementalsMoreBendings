package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Aba própria do addon na tela criativa/inventário de receitas. Mesmo
 * esquema do mod base ({@code ElementalsItems#ELEMENTALS_TAB}), só que
 * aqui direto com a API do NeoForge já que o addon é single-loader.
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    public static final Supplier<CreativeModeTab> MORE_BENDINGS_TAB = CREATIVE_TABS.register("more_bendings_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.GAS_SCROLL.get()))
                    .title(Component.literal(Constants.MOD_NAME))
                    .displayItems((context, entries) -> {
                        // Scrolls -- Air
                        entries.accept(ModItems.GAS_SCROLL.get());
                        entries.accept(ModItems.MIST_SCROLL.get());
                        entries.accept(ModItems.ATMOSPHERE_SCROLL.get());
                        entries.accept(ModItems.SOUND_SCROLL.get());
                        entries.accept(ModItems.TEMPERATURE_SCROLL.get());
                        entries.accept(ModItems.VOID_SCROLL.get());
                        // Scrolls -- Earth
                        entries.accept(ModItems.MUD_SCROLL.get());
                        entries.accept(ModItems.CRYSTAL_SCROLL.get());
                        entries.accept(ModItems.BONE_SCROLL.get());
                        entries.accept(ModItems.SAND_SCROLL.get());
                        entries.accept(ModItems.GLASS_SCROLL.get());
                        entries.accept(ModItems.PETRIFICATION_SCROLL.get());
                        entries.accept(ModItems.LAVA_SCROLL.get());
                        // Scrolls -- Fire
                        entries.accept(ModItems.PLASMA_SCROLL.get());
                        entries.accept(ModItems.COMBUSTION_SCROLL.get());
                        // Scrolls -- Water
                        entries.accept(ModItems.PLANT_SCROLL.get());
                        entries.accept(ModItems.SPIRIT_SCROLL.get());
                        entries.accept(ModItems.ICE_SCROLL.get());
                    }).build());

    private ModCreativeTabs() {
    }
}