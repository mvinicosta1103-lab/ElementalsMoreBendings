package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.items.scrolls.AtmosphereScrollItem;
import com.elementals.morebendings.items.scrolls.BoneScrollItem;
import com.elementals.morebendings.items.scrolls.CombustionScrollItem;
import com.elementals.morebendings.items.scrolls.CrystalScrollItem;
import com.elementals.morebendings.items.scrolls.GasScrollItem;
import com.elementals.morebendings.items.scrolls.GlassScrollItem;
import com.elementals.morebendings.items.scrolls.IceScrollItem;
import com.elementals.morebendings.items.scrolls.LavaScrollItem;
import com.elementals.morebendings.items.scrolls.MistScrollItem;
import com.elementals.morebendings.items.scrolls.MudScrollItem;
import com.elementals.morebendings.items.scrolls.PetrificationScrollItem;
import com.elementals.morebendings.items.scrolls.PlantScrollItem;
import com.elementals.morebendings.items.scrolls.PlasmaScrollItem;
import com.elementals.morebendings.items.scrolls.SandScrollItem;
import com.elementals.morebendings.items.scrolls.SoundScrollItem;
import com.elementals.morebendings.items.scrolls.SpiritScrollItem;
import com.elementals.morebendings.items.scrolls.TemperatureScrollItem;
import com.elementals.morebendings.items.scrolls.VoidScrollItem;
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

    // ---- Scrolls das sub-bendings (ver AbstractSubbendingScrollItem) ----
    // Air
    public static final Supplier<GasScrollItem> GAS_SCROLL = ITEMS.register("gas_scroll",
            () -> new GasScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<MistScrollItem> MIST_SCROLL = ITEMS.register("mist_scroll",
            () -> new MistScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<AtmosphereScrollItem> ATMOSPHERE_SCROLL = ITEMS.register("atmosphere_scroll",
            () -> new AtmosphereScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<SoundScrollItem> SOUND_SCROLL = ITEMS.register("sound_scroll",
            () -> new SoundScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<TemperatureScrollItem> TEMPERATURE_SCROLL = ITEMS.register("temperature_scroll",
            () -> new TemperatureScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<VoidScrollItem> VOID_SCROLL = ITEMS.register("void_scroll",
            () -> new VoidScrollItem(new Item.Properties().stacksTo(1)));

    // Earth
    public static final Supplier<MudScrollItem> MUD_SCROLL = ITEMS.register("mud_scroll",
            () -> new MudScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<CrystalScrollItem> CRYSTAL_SCROLL = ITEMS.register("crystal_scroll",
            () -> new CrystalScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<BoneScrollItem> BONE_SCROLL = ITEMS.register("bone_scroll",
            () -> new BoneScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<SandScrollItem> SAND_SCROLL = ITEMS.register("sand_scroll",
            () -> new SandScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<GlassScrollItem> GLASS_SCROLL = ITEMS.register("glass_scroll",
            () -> new GlassScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<PetrificationScrollItem> PETRIFICATION_SCROLL = ITEMS.register("petrification_scroll",
            () -> new PetrificationScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<LavaScrollItem> LAVA_SCROLL = ITEMS.register("lava_scroll",
            () -> new LavaScrollItem(new Item.Properties().stacksTo(1)));

    // Fire
    public static final Supplier<PlasmaScrollItem> PLASMA_SCROLL = ITEMS.register("plasma_scroll",
            () -> new PlasmaScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<CombustionScrollItem> COMBUSTION_SCROLL = ITEMS.register("combustion_scroll",
            () -> new CombustionScrollItem(new Item.Properties().stacksTo(1)));

    // Water
    public static final Supplier<PlantScrollItem> PLANT_SCROLL = ITEMS.register("plant_scroll",
            () -> new PlantScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<SpiritScrollItem> SPIRIT_SCROLL = ITEMS.register("spirit_scroll",
            () -> new SpiritScrollItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<IceScrollItem> ICE_SCROLL = ITEMS.register("ice_scroll",
            () -> new IceScrollItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}