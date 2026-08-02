package com.elementals.morebendings.bending.earthsubbendings.crystal;

import com.elementals.morebendings.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda a armadura REAL do jogador (em memória, por UUID) enquanto ele
 * está vestindo o set de crystalArmor, e devolve ela intacta ao desligar.
 * Não usa Data Attachment/NBT de propósito -- é estado transitório, do
 * mesmo jeito que EchoSenseAbility/PlasmaBoostState guardam Set<UUID> em
 * memória; se o servidor cair com alguém de armadura equipada, o pior
 * caso é ela continuar com o set de cristal até logar de novo (ver
 * onPlayerLoggedOut, que já cobre o caso comum de desconexão normal).
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

        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRYSTAL_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRYSTAL_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRYSTAL_LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CRYSTAL_BOOTS.get()));
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
}