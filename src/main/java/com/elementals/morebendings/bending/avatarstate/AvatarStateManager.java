package com.elementals.morebendings.bending.avatarstate;

import com.elementals.morebendings.commands.MoreBendingCommand;
import com.elementals.morebendings.data.PlayerAvatarData;
import com.elementals.morebendings.network.packets.SyncAvatarStatePacket;
import com.elementals.morebendings.registry.ModAttachments;
import commonnetwork.api.Dispatcher;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.air.AirElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.elements.fire.FireElement;
import dev.saperate.elementals.elements.water.WaterElement;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Avatar State "de verdade" -- ligado pelo próprio jogador (tecla, ver
 * {@code ToggleAvatarStatePacket}), diferente de {@code /morebending
 * avatar} (que é o comando de operador pra forçar ligar/desligar em
 * qualquer um). Reaproveita {@link MoreBendingCommand#grantAvatarState}/
 * {@link MoreBendingCommand#revokeAvatarState} pra conceder/revogar as
 * bendings (mesmo rastreamento via {@link PlayerAvatarData}, então nunca
 * tira algo que o jogador já tinha por fora), e por cima disso aplica o
 * "boost" (efeitos de status) e o efeito visual (partículas dos 4
 * elementos girando + olhos brilhantes, ver {@code AvatarStateEyesLayer}).
 * <p>
 * Só pode ser ligado por quem já domina os 4 elementos-base (Air, Water,
 * Earth, Fire) -- ninguém "ganha" o Avatar sem antes ser um bender
 * completo por conta própria; ver {@link #isEligible(ServerPlayer)}.
 */
public final class AvatarStateManager {

    private static final Set<UUID> ACTIVE = new HashSet<>();

    // Reforçado a cada ~4s (80 ticks) enquanto ativo, com folga de sobra
    // pra nunca deixar o efeito cair antes do próximo reforço.
    private static final int EFFECT_DURATION_TICKS = 140;
    private static final int EFFECT_REFRESH_INTERVAL = 80;

    private AvatarStateManager() {
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.contains(player.getUUID());
    }

    public static boolean isEligible(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender.hasElement(AirElement.get())
                && bender.hasElement(WaterElement.get())
                && bender.hasElement(EarthElement.get())
                && bender.hasElement(FireElement.get());
    }

    /** @return o novo estado (true = ligou, false = desligou/não conseguiu ligar). */
    public static boolean toggle(ServerPlayer player) {
        if (isActive(player)) {
            deactivate(player);
            return false;
        }
        return activate(player);
    }

    public static boolean activate(ServerPlayer player) {
        if (isActive(player)) {
            return true;
        }
        if (!isEligible(player)) {
            player.displayClientMessage(Component.literal(
                    "§7Você precisa dominar os 4 elementos-base (Ar, Água, Terra e Fogo) antes de entrar no Avatar State."), true);
            return false;
        }

        PlayerAvatarData avatarData = player.getData(ModAttachments.AVATAR);
        if (!avatarData.isAvatarState()) {
            MoreBendingCommand.grantAvatarState(player.createCommandSourceStack().withSuppressedOutput(), player, avatarData);
            avatarData.setAvatarState(true);
        }

        ACTIVE.add(player.getUUID());
        applyBuffs(player);
        spawnActivationBurst(player);
        broadcastSync(player, true);
        player.displayClientMessage(Component.literal("§bVocê entrou no Avatar State!"), true);
        return true;
    }

    public static void deactivate(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }
        ACTIVE.remove(player.getUUID());

        PlayerAvatarData avatarData = player.getData(ModAttachments.AVATAR);
        if (avatarData.isAvatarState()) {
            MoreBendingCommand.revokeAvatarState(player, avatarData);
            avatarData.setAvatarState(false);
        }
        removeBuffs(player);
        broadcastSync(player, false);
        player.displayClientMessage(Component.literal("§7Você saiu do Avatar State."), true);
    }

    private static void applyBuffs(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_DURATION_TICKS, 0, true, false));
    }

    private static void removeBuffs(ServerPlayer player) {
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.REGENERATION);
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.WATER_BREATHING);
        // Speed é deixada decair sozinha (some em poucos segundos) pra não
        // cortar o movimento do jogador de forma abrupta ao desligar.
    }

    private static void broadcastSync(ServerPlayer player, boolean active) {
        SyncAvatarStatePacket packet = new SyncAvatarStatePacket(player.getUUID(), active);
        for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
            Dispatcher.sendToClient(packet, online);
        }
    }

    private static void spawnActivationBurst(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                40, 0.4, 0.7, 0.4, 0.06);
    }

    /**
     * Registrado via NeoForge.EVENT_BUS em ElementalsMoreBendingsMod. Reforça
     * os efeitos periodicamente (senão expirariam) e desenha o anel dos 4
     * elementos girando ao redor de quem está no Avatar State -- ver
     * {@link #spawnElementRing}.
     */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        Iterator<UUID> it = ACTIVE.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }
            if (player.tickCount % EFFECT_REFRESH_INTERVAL == 0) {
                applyBuffs(player);
            }
            if (player.tickCount % 2 == 0) {
                spawnElementRing(player);
            }
        }
    }

    /**
     * Anel dos 4 elementos-base girando ao redor do corpo, cada um em um
     * quadrante (90° de diferença), subindo lentamente enquanto giram --
     * Ar (nuvem), Água (respingo), Terra (poeira de pedra/terra) e Fogo
     * (chama). Referência visual: olhos brilhantes + elementos girando ao
     * redor do Avatar.
     */
    private static void spawnElementRing(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = 1.3;
        double baseY = player.getY() + 0.1;
        double bob = Math.sin(player.tickCount * 0.1) * 0.5 + 0.6; // 0.1..1.1, sobe e desce girando
        double angle = Math.toRadians((player.tickCount * 6.0) % 360.0);

        spawnRingParticle(level, player, ParticleTypes.CLOUD, angle, radius, baseY + bob);
        spawnRingParticle(level, player, ParticleTypes.SPLASH, angle + Math.PI / 2, radius, baseY + bob);
        spawnRingParticle(level, player, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                angle + Math.PI, radius, baseY + bob);
        spawnRingParticle(level, player, ParticleTypes.FLAME, angle + 3 * Math.PI / 2, radius, baseY + bob);
    }

    private static void spawnRingParticle(ServerLevel level, ServerPlayer player,
                                          net.minecraft.core.particles.ParticleOptions type,
                                          double angle, double radius, double y) {
        double x = player.getX() + Math.cos(angle) * radius;
        double z = player.getZ() + Math.sin(angle) * radius;
        level.sendParticles(type, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    /** Limpa o UUID de quem desconecta com o Avatar State ligado (sem tentar revogar bendings -- já persistem no NBT). */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ACTIVE.remove(sp.getUUID());
        }
    }
}