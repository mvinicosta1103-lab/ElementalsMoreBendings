package com.elementals.morebendings.bending.earthsubbendings.crystal;

import com.elementals.morebendings.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda a armadura REAL do jogador (em memória, por UUID) enquanto ele
 * está vestindo o set de crystalArmor, e devolve ela intacta ao desligar.
 *
 * O set de cristal recebe Curse of Binding + Unbreakable na hora de vestir
 * -- isso trava a remoção manual pelo inventário (só sai morrendo, quebrando
 * -- impossível, já que é Unbreakable -- ou em criativo). unequip() via
 * código do servidor (chamado pelo toggle da Ability/pelo Manager) continua
 * funcionando normalmente, a maldição só bloqueia interação de GUI do
 * jogador.
 */
public final class CrystalArmorSetManager {

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final Map<UUID, ItemStack[]> STASHED = new HashMap<>();

    private CrystalArmorSetManager() {
    }

    public static void equip(ServerPlayer player) {
        UUID id = player.getUUID();
        if (STASHED.containsKey(id)) {
            return; // já equipado -- não pisa na armadura guardada
        }

        ItemStack[] previous = new ItemStack[SLOTS.length];
        for (int i = 0; i < SLOTS.length; i++) {
            previous[i] = player.getItemBySlot(SLOTS[i]).copy();
        }
        STASHED.put(id, previous);

        applyCrystalSet(player);
    }

    /** Veste as 4 peças (curse of binding + unbreakable já aplicados). Não mexe em STASHED. */
    private static void applyCrystalSet(ServerPlayer player) {
        player.setItemSlot(EquipmentSlot.HEAD, lockedPiece(player, ModItems.CRYSTAL_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, lockedPiece(player, ModItems.CRYSTAL_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, lockedPiece(player, ModItems.CRYSTAL_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, lockedPiece(player, ModItems.CRYSTAL_BOOTS.get()));
    }

    private static ItemStack lockedPiece(ServerPlayer player, net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        Holder<Enchantment> bindingCurse = player.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.BINDING_CURSE);
        stack.enchant(bindingCurse, 1);
        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false)); // false = não mostra "Unbreakable" no tooltip
        return stack;
    }

    public static void unequip(ServerPlayer player) {
        UUID id = player.getUUID();
        ItemStack[] previous = STASHED.remove(id);
        if (previous == null) {
            return; // não estava equipado por este manager -- não mexe em nada
        }
        for (int i = 0; i < SLOTS.length; i++) {
            player.setItemSlot(SLOTS[i], previous[i]);
        }
    }

    public static boolean isWearingCrystalSet(UUID playerId) {
        return STASHED.containsKey(playerId);
    }

    /** Chamado no logout -- devolve a armadura real na hora, sem esperar o toggle. */
    public static void restoreOnLogout(ServerPlayer player) {
        unequip(player);
    }

    /**
     * Chamado no respawn -- a morte zera os slots de armadura (o set de
     * cristal cai no chão junto com o resto do inventário, Curse of Binding
     * não impede isso). Se a Ability ainda estava marcada como ativa
     * (crystalArmor não é desligado pela morte), reveste um set novo por
     * cima dos slots vazios sem mexer no que já está em STASHED -- a
     * armadura real do jogador continua guardada em memória de antes de
     * morrer, intacta.
     */
    public static void reapplyAfterRespawn(ServerPlayer player) {
        if (isWearingCrystalSet(player.getUUID())) {
            applyCrystalSet(player);
        }
    }
}