package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Material "crystal" pra armadura dada por crystalArmor. Durabilidade/defesa
 * copiadas do Diamante (armadura de cristal é sólida, mas o objetivo aqui é
 * o visual + Resistência/Seismic Sense que já vêm da própria Ability, não
 * um upgrade de PvP) -- ajuste os números do `defense` se quiser diferente.
 *
 * ATENÇÃO: esta é a API de ArmorMaterial baseada em Registry/Holder do
 * 1.21.1. Se o pacote exato (net.minecraft.world.item.equipment.ArmorMaterial
 * vs net.minecraft.world.item.ArmorMaterial) não bater no seu IntelliJ,
 * é só deixar o autocomplete corrigir o import -- a estrutura do record
 * (durabilityMultiplier, defense map, enchantmentValue, equipSound,
 * toughness, knockbackResistance, repairIngredient, layers) é a mesma.
 */
public class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Constants.MOD_ID);

    public static final Supplier<ArmorMaterial> CRYSTAL = ARMOR_MATERIALS.register("crystal", () -> {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);

        List<ArmorMaterial.Layer> layers = List.of(
                new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "crystal")));

        return new ArmorMaterial(
                33, // durabilityMultiplier (mesmo do diamante)
                defense,
                10, // enchantmentValue
                SoundEvents.ARMOR_EQUIP_DIAMOND,
                2.0f, // toughness
                0.0f, // knockbackResistance
                ItemTags.REPAIRS_DIAMOND_ARMOR, // qualquer TagKey<Item> serve, não precisa ser real
                layers
        );
    });

    private ModArmorMaterials() {
    }
}