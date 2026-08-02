package com.elementals.morebendings.bending.earthsubbendings.crystal;

import com.elementals.morebendings.network.packets.SyncCrystalArmorPacket;
import commonnetwork.api.Dispatcher;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * "crystalArmor" — toggle passivo (mesmo esquema de {@code EchoSenseAbility}).
 * Aperta uma vez pra vestir a couraça de cristal, que fica ativa
 * indefinidamente -- sem precisar segurar Shift -- até apertar de novo, o
 * chi acabar (ver {@link CrystalArmorManager}) ou perder Crystal.
 *
 * Enquanto ativa: Resistência + Seismic Sense. O modelo visual é
 * responsabilidade de {@code CrystalArmorRenderLayer} no cliente -- este
 * toggle só manda {@link SyncCrystalArmorPacket} pra todo mundo saber quem
 * está com a armadura ligada.
 */
public class CrystalArmorAbility implements Ability {

    static final float TICK_CHI_COST = 0.3f;
    static final int EFFECT_REFRESH_TICKS = 20; // 1s, reaplicado a cada tick ativo pelo Manager
    static final int RESISTANCE_AMPLIFIER = 0; // Resistência I (-20% de dano)

    private static final Set<UUID> ACTIVE = new HashSet<>();

    @Override
    public boolean activatesOnPress() {
        return true;
    }

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        UUID id = caster.getUUID();
        boolean nowActive;
        if (ACTIVE.contains(id)) {
            ACTIVE.remove(id);
            nowActive = false;
        } else {
            ACTIVE.add(id);
            nowActive = true;
        }

        playToggleFeedback(level, player, nowActive);
        if (nowActive) {
            CrystalArmorSetManager.equip(caster);
        } else {
            CrystalArmorSetManager.unequip(caster);
            removeEffects(player);
        }

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
        Player player = bender.player;
        if (player instanceof ServerPlayer caster) {
            deactivate(caster.getUUID());
            CrystalArmorSetManager.unequip(caster);
            removeEffects(player);
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.contains(playerId);
    }

    public static void deactivate(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    static void broadcastSync(ServerPlayer player, boolean active) {
        SyncCrystalArmorPacket packet = new SyncCrystalArmorPacket(player.getUUID(), active);
        for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
            Dispatcher.sendToClient(packet, online);
        }
    }

    private static void removeEffects(Player player) {
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(dev.saperate.elementals.effects.ElementalsStatusEffects.SEISMIC_SENSE.get());
    }

    private void playToggleFeedback(ServerLevel level, Player player, boolean nowActive) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                nowActive ? SoundEvents.AMETHYST_BLOCK_PLACE : SoundEvents.AMETHYST_BLOCK_BREAK,
                SoundSource.PLAYERS, 0.8f, nowActive ? 0.7f : 1.1f);
        if (nowActive) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                    player.getX(), player.getY() + 0.9, player.getZ(), 16, 0.3, 0.5, 0.3, 0.0);
        }
    }
}