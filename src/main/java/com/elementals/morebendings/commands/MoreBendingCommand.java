package com.elementals.morebendings.commands;

import com.elementals.morebendings.bending.earthsubbendings.bone.BoneElement;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassElement;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.data.SubbendingType;
import com.elementals.morebendings.registry.ModAttachments;
import com.mojang.brigadier.CommandDispatcher;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
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
 * /gamemode e /xp. <subbending> aceita: gas, plant, mud, crystal, bone, sand,
 * glass (com autocomplete no jogo).
 */
public class MoreBendingCommand {

    private static final SimpleCommandExceptionType UNKNOWN_SUBBENDING = new SimpleCommandExceptionType(
            Component.literal("Sub-bending desconhecida. Use: gas, plant, mud, crystal, bone, sand ou glass."));

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

    /** Mensagem de motivo pra cada regra de elegibilidade -- usada tanto pro
     * grant quanto pra deixar claro pro operador o que falta pro jogador. */
    private static String eligibilityMessage(SubbendingType type) {
        return switch (type) {
            case MUD, CRYSTAL, SAND -> "precisa ter Earth e ter masterizado a árvore de Earth inteira";
            case BONE -> "precisa ter Earth e já ter estado a até "
                    + (int) BoneElement.BLOOD_PROXIMITY_RANGE + " blocos de um Blood bender em algum momento";
            case GLASS -> "precisa ter obtido Sand Bending antes";
            default -> "não atende aos requisitos";
        };
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestSubbendings(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(SubbendingType.ids(), builder);
    }

    private static int run(CommandContext<CommandSourceStack> ctx, boolean grant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String rawId = StringArgumentType.getString(ctx, "subbending");
        SubbendingType type = SubbendingType.byId(rawId).orElseThrow(UNKNOWN_SUBBENDING::create);

        // Mud, Crystal, Bone, Sand e Glass já são Elements de verdade (ver
        // MudElement/CrystalElement/BoneElement/SandElement/GlassElement) —
        // precisam passar pelo Bender do mod base, cada um com seu próprio
        // pré-requisito de aquisição, em vez do PlayerSubbendingData antigo.
        if (type == SubbendingType.MUD || type == SubbendingType.CRYSTAL
                || type == SubbendingType.BONE || type == SubbendingType.SAND
                || type == SubbendingType.GLASS) {
            return runRealElement(ctx.getSource(), target, type, grant);
        }

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

    /**
     * Caminho pras sub-bendings que já são {@code Element} de verdade
     * (Mud, Crystal, Bone, Sand, Glass). Cada uma tem sua própria regra de
     * elegibilidade (ver {@link #eligibilityMessage}); sem ela, o comando
     * falha com uma mensagem explicando o motivo, e nada é alterado no
     * jogador.
     */
    private static int runRealElement(CommandSourceStack source, ServerPlayer target, SubbendingType type, boolean grant) {
        Bender bender = Bender.getBender(target);
        Element element = switch (type) {
            case MUD -> MudElement.get();
            case CRYSTAL -> CrystalElement.get();
            case BONE -> BoneElement.get();
            case SAND -> SandElement.get();
            case GLASS -> GlassElement.get();
            default -> throw new IllegalArgumentException("Sub-bending sem Element real: " + type);
        };
        String playerName = target.getName().getString();

        if (grant) {
            if (bender.hasElement(element)) {
                source.sendFailure(Component.literal(playerName + " já tinha " + type.getDisplayName() + "."));
                return 0;
            }
            boolean eligible = switch (type) {
                case MUD -> MudElement.canAcquire(bender);
                case CRYSTAL -> CrystalElement.canAcquire(bender);
                case BONE -> BoneElement.canAcquire(bender);
                case SAND -> SandElement.canAcquire(bender);
                case GLASS -> GlassElement.canAcquire(bender);
                default -> false;
            };
            if (!eligible) {
                source.sendFailure(Component.literal(playerName + " " + eligibilityMessage(type)
                        + " antes de poder receber " + type.getDisplayName() + "."));
                return 0;
            }
            bender.addElement(element, true);
            source.sendSuccess(() -> Component.literal(type.getDisplayName() + " concedida a " + playerName + "."), true);
            target.sendSystemMessage(Component.literal("Você desbloqueou " + type.getDisplayName() + "!"));
            return 1;
        } else {
            if (!bender.hasElement(element)) {
                source.sendFailure(Component.literal(playerName + " não tinha " + type.getDisplayName() + "."));
                return 0;
            }
            bender.removeElement(element, true);
            source.sendSuccess(() -> Component.literal(type.getDisplayName() + " removida de " + playerName + "."), true);
            target.sendSystemMessage(Component.literal("Você perdeu acesso a " + type.getDisplayName() + "."));
            return 1;
        }
    }
}