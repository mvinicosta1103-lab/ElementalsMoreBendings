package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Material "crystal" pra armadura dada por crystalArmor.
 *
 * IMPORTANTE: essa assinatura é específica da 1.21.1 -- a partir da 1.21.2
 * a Mojang reescreveu ArmorMaterial inteiro (virou net.minecraft.world.item
 * .equipment.ArmorMaterial, baseado em EquipmentModel, sem Layer). Se um dia
 * atualizarem o mod pra 1.21.2+, este arquivo precisa ser reescrito do zero,
 * não só ajustado.
 */
public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Constants.MOD_ID);

    public static final Holder<ArmorMaterial> CRYSTAL = ARMOR_MATERIALS.register("crystal", () -> {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 6); // usado por mobs (lobo/cavalo), não pelo jogador

        List<ArmorMaterial.Layer> layers = List.of(
                new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "crystal")));

        return new ArmorMaterial(
                defense,
                10,                                    // enchantmentValue
                SoundEvents.ARMOR_EQUIP_DIAMOND,        // equipSound -- já é Holder<SoundEvent> nesta versão
                () -> Ingredient.of(Items.AMETHYST_SHARD), // repairIngredient
                layers,
                2.0f,  // toughness
                0.0f   // knockbackResistance
        );
    });

    private ModArmorMaterials() {
    }
}