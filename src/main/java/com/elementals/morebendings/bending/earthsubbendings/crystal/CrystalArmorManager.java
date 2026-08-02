package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.effects.ElementalsStatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * Dirige o {@code crystalArmor} ativo -- tick a tick, drena chi, garante que
 * os 4 slots de armadura continuam com a peça de cristal (ver
 * {@link CrystalArmorSetManager#enforceEquipped}) e reaplica Resistência +
 * Seismic Sense. Desliga sozinho quem ficar sem chi ou perder Crystal.
 */
public final class CrystalArmorManager {

    private CrystalArmorManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!CrystalArmorAbility.isActive(id)) {
                continue;
            }

            Bender bender = Bender.getBender(player);
            if (bender == null || !CrystalElement.isCrystalBender(bender)) {
                CrystalArmorAbility.deactivate(id);
                CrystalArmorSetManager.unequip(player);
                continue;
            }

            if (!bender.reduceChi(CrystalArmorAbility.TICK_CHI_COST)) {
                CrystalArmorAbility.deactivate(id);
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.removeEffect(ElementalsStatusEffects.SEISMIC_SENSE.get());
                CrystalArmorSetManager.unequip(player);
                continue;
            }

            CrystalArmorSetManager.enforceEquipped(player);

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                    CrystalArmorAbility.EFFECT_REFRESH_TICKS, CrystalArmorAbility.RESISTANCE_AMPLIFIER,
                    false, false, true));
            player.addEffect(new MobEffectInstance(ElementalsStatusEffects.SEISMIC_SENSE.get(),
                    CrystalArmorAbility.EFFECT_REFRESH_TICKS, 0,
                    false, false, true));
        }
    }
}