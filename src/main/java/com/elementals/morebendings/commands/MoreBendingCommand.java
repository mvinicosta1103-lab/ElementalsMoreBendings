package com.elementals.morebendings.commands;

import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.data.SubbendingType;
import com.elementals.morebendings.registry.ModAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /morebending grant <player> <subbending>
 * /morebending remove <player> <subbending>
 *
 * Requer permissão de operador (nível 2), igual aos comandos vanilla de
 * /gamemode e /xp. <subbending> aceita: gas, plant, mud, crystal (com
 * autocomplete no jogo).
 */
public class MoreBendingCommand {

    private static final SimpleCommandExceptionType UNKNOWN_SUBBENDING = new SimpleCommandExceptionType(
            Component.literal("Sub-bending desconhecida. Use: gas, plant, mud ou crystal."));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("morebending")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("subbending", StringArgumentType.word())
                                        .suggests(MoreBendingCommand::suggestSubbendings)
                                        .executes(ctx -> run(ctx, true)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("subbending", StringArgumentType.word())
                                        .suggests(MoreBendingCommand::suggestSubbendings)
                                        .executes(ctx -> run(ctx, false))))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestSubbendings(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(SubbendingType.ids(), builder);
    }

    private static int run(CommandContext<CommandSourceStack> ctx, boolean grant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String rawId = StringArgumentType.getString(ctx, "subbending");
        SubbendingType type = SubbendingType.byId(rawId).orElseThrow(UNKNOWN_SUBBENDING::create);

        PlayerSubbendingData data = target.getData(ModAttachments.SUBBENDINGS);
        boolean changed = grant ? data.grant(type) : data.revoke(type);

        CommandSourceStack source = ctx.getSource();
        String playerName = target.getName().getString();

        if (changed) {
            String verb = grant ? "concedida a" : "removida de";
            source.sendSuccess(() -> Component.literal(
                    type.getDisplayName() + " " + verb + " " + playerName + "."), true);

            target.sendSystemMessage(Component.literal(grant
                    ? "Você desbloqueou " + type.getDisplayName() + "!"
                    : "Você perdeu acesso a " + type.getDisplayName() + "."));
            return 1;
        } else {
            String reason = grant ? "já tinha" : "não tinha";
            source.sendFailure(Component.literal(playerName + " " + reason + " " + type.getDisplayName() + "."));
            return 0;
        }
    }
}