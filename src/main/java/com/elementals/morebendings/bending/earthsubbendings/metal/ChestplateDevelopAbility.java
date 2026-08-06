package com.elementals.morebendings.bending.earthsubbendings.metal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * "chestplateDevelop" — filho da árvore de {@link MetalElement} (Metal
 * Mastery). Instantânea, mesmo esquema de {@code PetrifyingTouchAbility}:
 * OBRIGATÓRIO liberar {@code currAbility} de volta pra {@code null} em
 * todo caminho de saída de {@link #onCall} e em {@link #onRemove}.
 *
 * Comportamento é o já descrito em en_us.json (chave
 * upgrade.elementals.chestplateDevelop.description): junta {@link
 * #INGOTS_REQUIRED} lingotes de ferro do inventário do bender e os
 * "desenvolve" numa peitoral de ferro de verdade, equipando na hora --
 * só funciona se o slot do peito já não estiver ocupado.
 */
public class ChestplateDevelopAbility implements Ability {

    private static final int INGOTS_REQUIRED = 3;
    private static final float CHI_COST = 15.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!caster.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            caster.displayClientMessage(
                    Component.literal("Seu peito já está ocupado por outra peça de armadura."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!consumeIronIngots(caster.getInventory(), INGOTS_REQUIRED)) {
            caster.displayClientMessage(
                    Component.literal("Você precisa de " + INGOTS_REQUIRED + " lingotes de ferro no inventário."),
                    true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            // Chi insuficiente DEPOIS de já ter consumido os lingotes --
            // devolve pro jogador pra não puni-lo duas vezes por uma
            // única falha (chi baixo já é a punição).
            caster.getInventory().add(new ItemStack(Items.IRON_INGOT, INGOTS_REQUIRED));
            bender.setCurrAbility(null);
            return;
        }

        caster.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.CRIT,
                caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.4, 0.5, 0.4, 0.02);

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    /**
     * @return true e consome exatamente {@code amount} lingotes de ferro se
     * havia o suficiente espalhado pelo inventário; false e não mexe em
     * nada caso contrário (tudo ou nada -- nunca consome parcialmente).
     */
    private boolean consumeIronIngots(Inventory inventory, int amount) {
        int available = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.IRON_INGOT)) {
                available += inventory.getItem(i).getCount();
            }
        }
        if (available < amount) {
            return false;
        }

        int remaining = amount;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.IRON_INGOT)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        return true;
    }
}