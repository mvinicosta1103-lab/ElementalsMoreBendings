package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);

    public static final Supplier<ArmorItem> CRYSTAL_HELMET = ITEMS.register("crystal_helmet",
            () -> new ArmorItem(ModArmorMaterials.CRYSTAL, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));

    public static final Supplier<ArmorItem> CRYSTAL_CHESTPLATE = ITEMS.register("crystal_chestplate",
            () -> new ArmorItem(ModArmorMaterials.CRYSTAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(15))));

    public static final Supplier<ArmorItem> CRYSTAL_LEGGINGS = ITEMS.register("crystal_leggings",
            () -> new ArmorItem(ModArmorMaterials.CRYSTAL, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(15))));

    public static final Supplier<ArmorItem> CRYSTAL_BOOTS = ITEMS.register("crystal_boots",
            () -> new ArmorItem(ModArmorMaterials.CRYSTAL, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(15))));

    private ModItems() {
    }
}