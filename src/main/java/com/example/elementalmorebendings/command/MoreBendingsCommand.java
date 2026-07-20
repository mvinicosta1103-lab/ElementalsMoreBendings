package com.example.elementalmorebendings.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.StateDataSaverAndLoader;
import dev.saperate.elementals.elements.Element;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * MoreBendingsCommand
 * <p>
 * Comando próprio ("/morebendings grant") que espelha a lógica do
 * "/subbending grant" do jar base (dev.jayden.elementalssubbending), mas
 * sem tocar no jar original. Existe porque a lista de sugestões do
 * comando base (SUBBENDINGS) é hardcoded dentro do jar deles e não inclui
 * elementos de addons — então "Mud" (e o que vier depois) nunca aparece
 * no Tab, mesmo que Element.getElement("Mud") já funcione.
 * <p>
 * A lógica de concessão (grant) é idêntica à do jar base: busca o Element
 * pelo nome, dá pro Bender via addElement + bindDefaultAbilities. A única
 * diferença real é de onde vem a lista de sugestões do Tab
 * (MoreBendingsElementRegistry, que cresce conforme novos elementos do
 * addon são registrados).
 */
public final class MoreBendingsCommand {

    private MoreBendingsCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("morebendings")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    MoreBendingsElementRegistry.getRegisteredNames()
                                                            .forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> grant(
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "element")))
                                        )
                                )
                        )
        );
    }

    private static int grant(ServerPlayer player, String elementName) {
        Element element = Element.getElement(elementName);
        if (element == null) {
            player.displayClientMessage(Component.literal("Unknown sub-bending: " + elementName), false);
            return 0;
        }

        Bender bender = Bender.getBender(player);
        bender.addElement(element, true);
        bender.bindDefaultAbilities();
        StateDataSaverAndLoader.getServerState(player.getServer()).setDirty();
        player.displayClientMessage(Component.literal("Granted " + elementName + " bending."), false);
        return 1;
    }
}