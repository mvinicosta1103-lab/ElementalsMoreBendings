package com.elementals.morebendings.bending.avatarstate;

import com.elementals.morebendings.data.ServerAvatarSavedData;
import com.mojang.brigadier.context.CommandContext;
import commonnetwork.api.Network;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.StateDataSaverAndLoader;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.air.AirElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.elements.fire.FireElement;
import dev.saperate.elementals.elements.water.WaterElement;
import dev.saperate.elementals.network.packets.common.SyncLevelPacket;
import dev.saperate.elementals.network.packets.common.SyncUpgradeListPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sistema de "Avatar-título": diferente do {@link AvatarStateManager} (que é
 * um boost TEMPORÁRIO que qualquer bender completo pode ligar/desligar),
 * este aqui é um TÍTULO -- só um jogador do server inteiro é o Avatar por
 * vez, ele o mantém até morrer (não até desconectar), e ao morrer o título
 * passa aleatoriamente pra outro jogador online (ver {@link #onAvatarDeath}).
 * <p>
 * Regras específicas pedidas:
 * <ul>
 *     <li>Virar o Avatar concede só os 4 elementos-base (Air, Water, Earth,
 *     Fire) -- NUNCA todas as sub-bendings de uma vez (isso é o que {@link
 *     AvatarStateManager} já faz, e continua existindo separado).</li>
 *     <li>Os 4 elementos concedidos NUNCA são revogados, nem quando o
 *     título passa pra outro jogador -- ver {@link #grantCoreElements}, que
 *     não guarda rastreamento nenhum pra desfazer depois (diferente de
 *     {@code PlayerAvatarData}). Título é transitório, progressão é
 *     permanente.</li>
 *     <li>Enquanto for o Avatar atual, o jogador pula a exigência de
 *     masterizar o elemento-base pra pegar sub-bendings via scroll -- ver o
 *     bypass em {@code AbstractSubbendingScrollItem#use}.</li>
 *     <li>Se o Avatar atual (que já tem os 4 elementos-base por causa do
 *     título) ligar o {@link AvatarStateManager} de verdade e depois
 *     desligar, ele não perde nada: {@code revokeAvatarState} só desfaz o
 *     que {@code grantAvatarState} concedeu, e como os 4 elementos-base já
 *     eram dele por fora (concedidos por este sistema), eles nunca entram
 *     no rastreamento de "concedido pelo Avatar State" pra começo de
 *     conversa -- nenhuma mudança extra foi necessária nesse ponto.</li>
 * </ul>
 */
public final class ServerAvatarManager {

    private ServerAvatarManager() {
    }

    public static boolean isCurrentAvatar(ServerPlayer player) {
        return isCurrentAvatar(player.getServer(), player.getUUID());
    }

    public static boolean isCurrentAvatar(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return false;
        }
        UUID current = ServerAvatarSavedData.get(server).getCurrentAvatar();
        return current != null && current.equals(uuid);
    }

    public static UUID getCurrentAvatarUuid(MinecraftServer server) {
        return ServerAvatarSavedData.get(server).getCurrentAvatar();
    }

    // ==================== Comandos ====================

    /** {@code /morebending serveravatar start} -- escolhe um Avatar aleatório dentre os online agora e liga o sistema. */
    public static int cmdStart(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        ServerAvatarSavedData data = ServerAvatarSavedData.get(server);

        if (data.getCurrentAvatar() != null) {
            source.sendFailure(Component.literal("Já existe um Avatar no server. Use '/morebending serveravatar set <player>' pra trocar."));
            return 0;
        }

        ServerPlayer chosen = pickRandomOnline(server, null);
        if (chosen == null) {
            source.sendFailure(Component.literal("Nenhum jogador online pra virar o Avatar."));
            return 0;
        }

        data.markSystemStarted();
        assignAvatar(server, chosen, "O sistema de Avatar foi ativado!");
        source.sendSuccess(() -> Component.literal(chosen.getName().getString() + " é o novo Avatar."), true);
        return 1;
    }

    /** {@code /morebending serveravatar set <player>} -- força um jogador específico a virar o Avatar (não revoga de ninguém, só concede). */
    public static int cmdSet(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        ServerAvatarSavedData data = ServerAvatarSavedData.get(server);
        data.markSystemStarted();
        assignAvatar(server, target, null);
        source.sendSuccess(() -> Component.literal(target.getName().getString() + " agora é o Avatar."), true);
        return 1;
    }

    /** {@code /morebending serveravatar stop} -- desliga o sistema (ninguém é o Avatar; os 4 elementos-base concedidos NÃO são removidos de quem já tinha). */
    public static int cmdStop(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        ServerAvatarSavedData data = ServerAvatarSavedData.get(server);

        UUID previous = data.getCurrentAvatar();
        data.setCurrentAvatar(null);

        if (previous != null) {
            ServerPlayer previousPlayer = server.getPlayerList().getPlayer(previous);
            if (previousPlayer != null) {
                previousPlayer.displayClientMessage(Component.literal(
                        "§7Você não é mais o Avatar do server. (Você continua com tudo que já tinha.)"), true);
            }
        }

        source.sendSuccess(() -> Component.literal("Sistema de Avatar desligado."), true);
        return 1;
    }

    /** {@code /morebending serveravatar status}. */
    public static int cmdStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        UUID current = getCurrentAvatarUuid(server);
        if (current == null) {
            source.sendSuccess(() -> Component.literal("Não há Avatar no momento."), true);
            return 1;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(current);
        String name = player != null ? player.getName().getString() : current.toString() + " (offline)";
        source.sendSuccess(() -> Component.literal("Avatar atual: " + name), true);
        return 1;
    }

    // ==================== Sucessão por morte ====================

    /**
     * Registrado via NeoForge.EVENT_BUS em ElementalsMoreBendingsMod. Se
     * quem morreu era o Avatar atual, escolhe outro jogador online
     * aleatoriamente pra assumir o título -- se ninguém mais estiver
     * online, o título fica vago até o próximo login (ver {@link
     * #onPlayerLoggedIn}).
     */
    public static void onAvatarDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer dead)) {
            return;
        }
        MinecraftServer server = dead.getServer();
        if (server == null) {
            return;
        }
        ServerAvatarSavedData data = ServerAvatarSavedData.get(server);
        if (!dead.getUUID().equals(data.getCurrentAvatar())) {
            return;
        }

        data.setCurrentAvatar(null);
        broadcast(server, "§7" + dead.getName().getString() + " morreu e deixou de ser o Avatar.");

        ServerPlayer next = pickRandomOnline(server, dead.getUUID());
        if (next != null) {
            assignAvatar(server, next, null);
        }
        // Se ninguém mais estiver online, o título fica vago -- o próximo a
        // logar assume automaticamente, ver onPlayerLoggedIn.
    }

    /**
     * Se o sistema já foi iniciado e o título está vago (ninguém online
     * quando o último Avatar morreu), o próximo jogador a entrar assume
     * automaticamente -- senão o título ficaria preso pra sempre.
     */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerAvatarSavedData data = ServerAvatarSavedData.get(server);
        if (data.isSystemStarted() && data.getCurrentAvatar() == null) {
            assignAvatar(server, player, "O título de Avatar estava vago -- ele passou pra você!");
        }
    }

    // ==================== Internos ====================

    private static void assignAvatar(MinecraftServer server, ServerPlayer player, String privateMessageOverride) {
        ServerAvatarSavedData data = ServerAvatarSavedData.get(server);
        data.setCurrentAvatar(player.getUUID());
        grantCoreElements(player);

        player.displayClientMessage(Component.literal(privateMessageOverride != null
                ? "§b" + privateMessageOverride
                : "§bVocê agora é o Avatar do server!"), false);
        broadcast(server, "§b" + player.getName().getString() + " é o novo Avatar do server!");
    }

    /**
     * Concede só os 4 elementos-base pra quem virou o Avatar, sem
     * rastreamento nenhum pra revogar depois -- uma vez concedido, fica
     * pra sempre com o jogador, mesmo que o título passe pra outra pessoa.
     */
    private static void grantCoreElements(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        addIfMissing(bender, AirElement.get());
        addIfMissing(bender, WaterElement.get());
        addIfMissing(bender, EarthElement.get());
        addIfMissing(bender, FireElement.get());
        syncAndPersist(bender, player);
    }

    private static void addIfMissing(Bender bender, Element element) {
        if (!bender.hasElement(element)) {
            bender.addElement(element, true);
        }
    }

    private static void syncAndPersist(Bender bender, ServerPlayer target) {
        Network.getNetworkHandler().sendToClient(SyncUpgradeListPacket.createFromBender(bender), target);
        Network.getNetworkHandler().sendToClient(SyncLevelPacket.createFromBender(bender), target);
        StateDataSaverAndLoader.getServerState(target.getServer()).setDirty();
    }

    private static ServerPlayer pickRandomOnline(MinecraftServer server, UUID exclude) {
        List<ServerPlayer> candidates = server.getPlayerList().getPlayers().stream()
                .filter(p -> exclude == null || !p.getUUID().equals(exclude))
                .toList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private static void broadcast(MinecraftServer server, String message) {
        Component component = Component.literal(message);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.sendSystemMessage(component);
        }
    }
}