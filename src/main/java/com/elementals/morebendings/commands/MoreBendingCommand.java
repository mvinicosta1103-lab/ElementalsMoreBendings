package com.elementals.morebendings.commands;

import com.elementals.morebendings.bending.earthsubbendings.bone.BoneElement;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassElement;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaElement;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import com.elementals.morebendings.bending.earthsubbendings.petrification.PetrificationElement;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import com.elementals.morebendings.bending.airsubbendings.atmosphere.AtmosphereElement;
import com.elementals.morebendings.bending.airsubbendings.AirMasteryCheck;
import com.elementals.morebendings.bending.firesubbendings.FireMasteryCheck;
import com.elementals.morebendings.bending.avatarstate.AvatarElement;
import com.elementals.morebendings.bending.avatarstate.ServerAvatarManager;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.elements.fire.FireElement;
import dev.saperate.elementals.elements.water.WaterElement;
import com.elementals.morebendings.data.PlayerAvatarData;
import com.elementals.morebendings.data.PlayerSubbendingData;
import com.elementals.morebendings.data.SubbendingType;
import com.elementals.morebendings.registry.ModAttachments;
import com.mojang.brigadier.CommandDispatcher;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;
import dev.saperate.elementals.data.StateDataSaverAndLoader;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.air.AirElement;
import dev.saperate.elementals.network.packets.common.SyncLevelPacket;
import dev.saperate.elementals.network.packets.common.SyncUpgradeListPacket;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import commonnetwork.api.Network;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.elementals.morebendings.bending.airsubbendings.sound.SoundElement;
import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import com.elementals.morebendings.bending.airsubbendings.temperature.TemperatureElement;
import com.elementals.morebendings.bending.airsubbendings.voiding.VoidElement;
import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionElement;
import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaElement;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantElement;
import com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement;

/**
 * /morebending grant <player> <subbending>
 * /morebending remove <player> <subbending>
 * /morebending debug <player> <subbending>
 * /morebending avatar <player> <true|false>
 * /morebending serveravatar start|stop|status|set <player>
 *
 * Requer permissão de operador (nível 2), igual aos comandos vanilla de
 * /gamemode e /xp. <subbending> aceita: gas, plant, spirit, mud, crystal, bone, sand,
 * glass, petrification, lava, atmosphere, mist, sound, temperature, void, plasma (com autocomplete no jogo).
 *
 * "avatar" liga/desliga o Avatar State do jogador (ver {@link
 * PlayerAvatarData}) — igual ao Avatar de verdade, concede TODOS os 4
 * elementos-base e TODAS as sub-bendings deste addon de uma vez, sem
 * exigir os pré-requisitos normais. Desligar remove só o que o Avatar
 * State concedeu, preservando qualquer bending que o jogador já tinha
 * por fora.
 *
 * "serveravatar" é o sistema de Avatar-TÍTULO (ver {@link ServerAvatarManager}):
 * diferente do "avatar" acima, só um jogador do server inteiro é o Avatar
 * por vez, ele concede só os 4 elementos-base (não todas as sub-bendings)
 * e nunca é revogado — o título passa pra outro jogador (aleatório, dentre
 * os online) quando o Avatar atual morre.
 */
public class MoreBendingCommand {

    private static final SimpleCommandExceptionType UNKNOWN_SUBBENDING = new SimpleCommandExceptionType(
            Component.literal("Sub-bending desconhecida. Use: Gas, Flying, Plant, Spirit, Mud, Crystal, Bone, Sand, Glass, Petrification, Lava, Atmosphere, Mist, Sound, Temperature, Void, Plasma ou Combustion."));

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
                                        .executes(ctx -> run(ctx, false)))))
                .then(Commands.literal("debug")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("subbending", StringArgumentType.word())
                                        .suggests(MoreBendingCommand::suggestSubbendings)
                                        .executes(MoreBendingCommand::debug))))
                .then(Commands.literal("avatar")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("state", BoolArgumentType.bool())
                                        .executes(MoreBendingCommand::setAvatarState))))
                .then(Commands.literal("serveravatar")
                        .then(Commands.literal("start").executes(ServerAvatarManager::cmdStart))
                        .then(Commands.literal("stop").executes(ServerAvatarManager::cmdStop))
                        .then(Commands.literal("status").executes(ServerAvatarManager::cmdStatus))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ServerAvatarManager::cmdSet)))));
    }

    private static String eligibilityMessage(SubbendingType type) {
        return switch (type) {
            case MUD, CRYSTAL, SAND, PETRIFICATION, LAVA -> "precisa ter Earth e ter masterizado a árvore de Earth inteira";
            case ATMOSPHERE, GAS, MIST, SOUND, TEMPERATURE, VOID -> "precisa ter Air e ter masterizado a árvore de Air inteira";
            case PLANT, SPIRIT -> "precisa ter Water e ter masterizado a árvore de Water inteira";
            case PLASMA, COMBUSTION -> "precisa ter Fire e ter masterizado a árvore de Fire inteira";
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

    /**
     * Manda pro jogador o estado REAL que o servidor tem gravado agora — sem
     * isso a gente só está adivinhando por que a compra não vai pra frente.
     * Mostra: elemento ativo do bender, se o nó raiz está marcado como
     * comprado, e o resultado de canBuyUpgrade para cada nó filho direto.
     */
    private static int debug(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String rawId = StringArgumentType.getString(ctx, "subbending");
        SubbendingType type = SubbendingType.byId(rawId).orElseThrow(UNKNOWN_SUBBENDING::create);
        CommandSourceStack source = ctx.getSource();

        Bender bender = Bender.getBender(target);
        Element element = switch (type) {
            case GAS -> GasElement.get();
            case MIST -> MistElement.get();
            case MUD -> MudElement.get();
            case CRYSTAL -> CrystalElement.get();
            case ATMOSPHERE -> AtmosphereElement.get();
            case PLASMA -> PlasmaElement.get();
            case COMBUSTION -> CombustionElement.get();
            case BONE -> BoneElement.get();
            default -> null;
        };
        if (element == null) {
            source.sendFailure(Component.literal("Debug só implementado pra Gas/Mist/Mud/Crystal/Atmosphere/Plasma/Combustion/Bone por enquanto."));
            return 0;
        }

        PlayerData data = PlayerData.get(target);
        source.sendSuccess(() -> Component.literal(
                "elemento ativo (activeElementIndex): " + data.getElement().getName()
                        + " | tem " + type.getDisplayName() + "? " + bender.hasElement(element)), true);

        Upgrade rootChild = element.root.children[0];
        boolean rootBought = data.upgrades.getOrDefault(rootChild, false);
        source.sendSuccess(() -> Component.literal(
                "nó raiz '" + rootChild.name + "' marcado como comprado no servidor? " + rootBought), true);

        for (Upgrade child : rootChild.children) {
            boolean canBuy = PlayerData.canBuyUpgrade(data.upgrades, element, child.name,
                    new java.util.concurrent.atomic.AtomicInteger(data.level));
            source.sendSuccess(() -> Component.literal(
                    "  -> " + child.name + " | preço " + child.price
                            + " | comprável agora pelo servidor? " + canBuy), true);
        }
        source.sendSuccess(() -> Component.literal("level atual do jogador: " + data.level), true);
        return 1;
    }

    private static int run(CommandContext<CommandSourceStack> ctx, boolean grant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String rawId = StringArgumentType.getString(ctx, "subbending");
        SubbendingType type = SubbendingType.byId(rawId).orElseThrow(UNKNOWN_SUBBENDING::create);

        if (type == SubbendingType.MUD || type == SubbendingType.CRYSTAL
                || type == SubbendingType.BONE || type == SubbendingType.SAND
                || type == SubbendingType.GLASS || type == SubbendingType.PETRIFICATION
                || type == SubbendingType.LAVA || type == SubbendingType.ATMOSPHERE
                || type == SubbendingType.GAS || type == SubbendingType.MIST
                || type == SubbendingType.PLASMA || type == SubbendingType.COMBUSTION
                || type == SubbendingType.PLANT || type == SubbendingType.SPIRIT
                || type == SubbendingType.SOUND || type == SubbendingType.TEMPERATURE
                || type == SubbendingType.VOID) {
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

    /** Manda os pacotes de sincronização + persiste em disco. Chamar sempre
     * que o estado de upgrades do bender for alterado fora do fluxo normal
     * de BuyUpgradePacket/ToggleUpgradePacket (que já fazem isso sozinhos).
     * Público -- reaproveitado fora deste arquivo por {@code
     * AvatarBendingGrantAbility}/{@code AvatarBendingRemoveAbility}. */
    public static void syncAndPersist(Bender bender, ServerPlayer target) {
        Network.getNetworkHandler().sendToClient(SyncUpgradeListPacket.createFromBender(bender), target);
        Network.getNetworkHandler().sendToClient(SyncLevelPacket.createFromBender(bender), target);
        StateDataSaverAndLoader.getServerState(target.getServer()).setDirty();
    }

    /** Mapeamento sub-bending -> Element real correspondente. FLYING não tem Element de verdade (flag-only, ver PlayerSubbendingData). */
    private static Element elementFor(SubbendingType type) {
        return switch (type) {
            case MUD -> MudElement.get();
            case CRYSTAL -> CrystalElement.get();
            case BONE -> BoneElement.get();
            case SAND -> SandElement.get();
            case GLASS -> GlassElement.get();
            case PETRIFICATION -> PetrificationElement.get();
            case LAVA -> LavaElement.get();
            case ATMOSPHERE -> AtmosphereElement.get();
            case GAS -> GasElement.get();
            case MIST -> MistElement.get();
            case PLASMA -> PlasmaElement.get();
            case COMBUSTION -> CombustionElement.get();
            case PLANT -> PlantElement.get();
            case SPIRIT -> SpiritElement.get();
            case SOUND -> SoundElement.get();
            case TEMPERATURE -> TemperatureElement.get();
            case VOID -> VoidElement.get();
            default -> null;
        };
    }

    private static int runRealElement(CommandSourceStack source, ServerPlayer target, SubbendingType type, boolean grant) {
        return runRealElement(source, target, type, grant, false);
    }

    /**
     * @param bypassEligibility quando {@code true}, pula a checagem normal de
     *                          {@code canAcquire} (masterizar a árvore-base,
     *                          proximidade de Blood bender, etc.) — usado só
     *                          pelo Avatar State (ver {@link #grantAvatarState}),
     *                          que transcende esses pré-requisitos. Todo o
     *                          resto do fluxo (autoUnlockRoot por tipo, sync,
     *                          mensagens) continua idêntico ao grant normal.
     */
    private static int runRealElement(CommandSourceStack source, ServerPlayer target, SubbendingType type,
                                      boolean grant, boolean bypassEligibility) {
        Bender bender = Bender.getBender(target);
        Element element = elementFor(type);
        if (element == null) {
            throw new IllegalArgumentException("Sub-bending sem Element real: " + type);
        }
        String playerName = target.getName().getString();

        if (grant) {
            if (bender.hasElement(element)) {
                if (type == SubbendingType.ATMOSPHERE) {
                    AtmosphereElement.autoUnlockRoots(bender);
                    syncAndPersist(bender, target);
                    source.sendSuccess(() -> Component.literal(
                            "Nós raiz de Atmosphere sincronizados e persistidos pra " + playerName + "."), true);
                    target.sendSystemMessage(Component.literal(
                            "Sua árvore de Atmosphere Bending foi reparada -- tente comprar os upgrades de novo."));
                    return 1;
                }
                if (type == SubbendingType.GAS) {
                    GasElement.autoUnlockRoot(bender);
                    syncAndPersist(bender, target);
                    source.sendSuccess(() -> Component.literal(
                            "gasCloud (nó raiz) sincronizado e persistido pra " + playerName + "."), true);
                    target.sendSystemMessage(Component.literal(
                            "Sua árvore de Gas Bending foi reparada -- tente comprar os upgrades de novo."));
                    return 1;
                }
                if (type == SubbendingType.MIST) {
                    MistElement.autoUnlockRoot(bender);
                    syncAndPersist(bender, target);
                    source.sendSuccess(() -> Component.literal(
                            "mistCloud (nó raiz) sincronizado e persistido pra " + playerName + "."), true);
                    target.sendSystemMessage(Component.literal(
                            "Sua árvore de Mist Bending foi reparada -- tente comprar os upgrades de novo."));
                    return 1;
                }
                if (type == SubbendingType.PLASMA) {
                    PlasmaElement.autoUnlockRoot(bender);
                    syncAndPersist(bender, target);
                    source.sendSuccess(() -> Component.literal(
                            "plasmaClaws (nó raiz) sincronizado e persistido pra " + playerName + "."), true);
                    target.sendSystemMessage(Component.literal(
                            "Sua árvore de Plasma Bending foi reparada -- tente comprar os upgrades de novo."));
                    return 1;
                }
                if (type == SubbendingType.COMBUSTION) {
                    CombustionElement.autoUnlockRoot(bender);
                    syncAndPersist(bender, target);
                    source.sendSuccess(() -> Component.literal(
                            "combustionExplosion (nó raiz) sincronizado e persistido pra " + playerName + "."), true);
                    target.sendSystemMessage(Component.literal(
                            "Sua árvore de Combustion Bending foi reparada -- tente comprar os upgrades de novo."));
                    return 1;
                }
                if (type == SubbendingType.BONE) {
                    BoneElement.autoUnlockRoot(bender);
                    syncAndPersist(bender, target);
                    source.sendSuccess(() -> Component.literal(
                            "boneControl (nó raiz) sincronizado e persistido pra " + playerName + "."), true);
                    target.sendSystemMessage(Component.literal(
                            "Sua árvore de Bone Bending foi reparada -- tente comprar os upgrades de novo."));
                    return 1;
                }
                if (type == SubbendingType.GLASS) {
                    GlassElement.autoUnlockRoot(bender);
                    syncAndPersist(bender, target);
                    source.sendSuccess(() -> Component.literal(
                            "glassShards (nó raiz) sincronizado e persistido pra " + playerName + "."), true);
                    target.sendSystemMessage(Component.literal(
                            "Sua árvore de Glass Bending foi reparada -- tente comprar os upgrades de novo."));
                    return 1;
                }
                if (type == SubbendingType.PLANT) {
                    // Não precisa de autoUnlockRoot (os 4 ramos raiz de Plant já
                    // têm preço 0 e são compráveis direto pela UI). O problema
                    // aqui é outro: StateDataSaverAndLoader#load() restaura
                    // boundAbilities[3]/[4] (as teclas físicas de vineGrasp e
                    // rootSnare) a partir do NBT salvo (bind4/bind5) -- se esse
                    // save foi gravado antes de vineGrasp/rootSnare terem tecla
                    // própria, esses campos ficaram gravados como -1, e
                    // Element#getBindableAbility(-1) devolve null pra sempre,
                    // travando as duas teclas em branco mesmo com o upgrade já
                    // comprado.
                    //
                    // A primeira versão desse reparo chamava
                    // bender.bindDefaultAbilities() direto -- só que esse
                    // método (decompilado do jar base) sempre reconstrói as
                    // teclas do elemento que estiver em
                    // plrData.activeElementIndex NAQUELE MOMENTO, não
                    // necessariamente Plant. Se o jogador rodasse o comando
                    // com outro elemento ativo (ou activeElementIndex por
                    // qualquer razão não apontasse pro Plant), o "reparo"
                    // mexia nas teclas do elemento errado e Plant continuava
                    // com vineGrasp/rootSnare travados -- por isso o comando
                    // reportava sucesso sem consertar nada de verdade.
                    //
                    // Fix: usa setElement(int,boolean) -- que já existe na
                    // API base, já chama bindDefaultAbilities() internamente
                    // pro elemento que vira ativo, e já sincroniza o cliente
                    // -- pra forçar o bender pra Plant, reparar de verdade, e
                    // só então devolver pro elemento que estava ativo antes
                    // (repetindo o mesmo processo pra ele, então ele também
                    // sai com teclas frescas em vez de ficar potencialmente
                    // bagunçado por termos trocado o ativo no meio do
                    // caminho).
                    int plantIndex = bender.plrData.elements.indexOf(PlantElement.get());
                    if (plantIndex < 0) {
                        source.sendFailure(Component.literal(playerName
                                + " tem Plant marcado como adquirido mas o Element não está na lista"
                                + " do bender -- estado inconsistente demais pra esse comando reparar sozinho."));
                        return 0;
                    }

                    int originalIndex = bender.plrData.activeElementIndex;
                    bender.setElement(plantIndex, true);
                    if (originalIndex != plantIndex) {
                        bender.setElement(originalIndex, true);
                    }
                    syncAndPersist(bender, target);
                    source.sendSuccess(() -> Component.literal(
                            "Teclas de Plant Bending re-vinculadas e persistidas pra " + playerName + "."), true);
                    target.sendSystemMessage(Component.literal(
                            "Suas teclas de Plant Bending foram reparadas -- vineGrasp e rootSnare já devem responder."));
                    return 1;
                }
                source.sendFailure(Component.literal(playerName + " já tinha " + type.getDisplayName() + "."));
                return 0;
            }
            if (type == SubbendingType.BONE) {
                // Concessão via comando sobrepõe o evento histórico de "já ter
                // cruzado com um Blood bender" -- marca a flag como satisfeita
                // ANTES da checagem de elegibilidade logo abaixo, então
                // BoneElement.canAcquire nunca falha por causa dele (o
                // BloodProximityTracker não precisa ter detectado nada de
                // verdade). Continua exigindo Earth normalmente -- só esse
                // pré-requisito específico é dispensado pelo comando.
                target.getData(ModAttachments.SUBBENDINGS).setMetBloodBender(true);
            }
            boolean eligible = bypassEligibility || switch (type) {
                case MUD -> MudElement.canAcquire(bender);
                case CRYSTAL -> CrystalElement.canAcquire(bender);
                case BONE -> BoneElement.canAcquire(bender);
                case SAND -> SandElement.canAcquire(bender);
                case GLASS -> GlassElement.canAcquire(bender);
                case PETRIFICATION -> PetrificationElement.canAcquire(bender);
                case LAVA -> LavaElement.canAcquire(bender);
                case ATMOSPHERE -> AtmosphereElement.canAcquire(bender);
                case GAS -> GasElement.canAcquire(bender);
                case MIST -> MistElement.canAcquire(bender);
                case PLASMA -> PlasmaElement.canAcquire(bender);
                case COMBUSTION -> CombustionElement.canAcquire(bender);
                case PLANT -> PlantElement.canAcquire(bender);
                case SPIRIT -> SpiritElement.canAcquire(bender);
                case SOUND -> SoundElement.canAcquire(bender);
                case TEMPERATURE -> TemperatureElement.canAcquire(bender);
                case VOID -> VoidElement.canAcquire(bender);
                default -> false;
            };
            if (!eligible) {
                source.sendFailure(Component.literal(playerName + " " + eligibilityMessage(type)
                        + " antes de poder receber " + type.getDisplayName() + "."));

                if ((type == SubbendingType.GAS || type == SubbendingType.ATMOSPHERE || type == SubbendingType.MIST
                        || type == SubbendingType.SOUND || type == SubbendingType.TEMPERATURE
                        || type == SubbendingType.VOID)
                        && bender.hasElement(AirElement.get())) {
                    java.util.List<String> missing = AirMasteryCheck.missingRequirements(bender);
                    if (missing.isEmpty()) {
                        source.sendFailure(Component.literal(
                                "Diagnóstico: nenhum nó pendente foi encontrado -- "
                                        + playerName + " parece já ter tudo. "
                                        + "Pode ser algum outro motivo de canAcquire falhar."));
                    } else {
                        source.sendFailure(Component.literal("Nós de Air ainda faltando pra " + playerName + ":"));
                        for (String reason : missing) {
                            source.sendFailure(Component.literal(" - " + reason));
                        }
                    }
                }

                if ((type == SubbendingType.PLASMA || type == SubbendingType.COMBUSTION)
                        && bender.hasElement(FireElement.get())) {
                    java.util.List<String> missing = FireMasteryCheck.missingRequirements(bender);
                    if (missing.isEmpty()) {
                        source.sendFailure(Component.literal(
                                "Diagnóstico: nenhum nó pendente foi encontrado -- "
                                        + playerName + " parece já ter tudo. "
                                        + "Pode ser algum outro motivo de canAcquire falhar."));
                    } else {
                        source.sendFailure(Component.literal("Nós de Fire ainda faltando pra " + playerName + ":"));
                        for (String reason : missing) {
                            source.sendFailure(Component.literal(" - " + reason));
                        }
                    }
                }
                return 0;
            }
            bender.addElement(element, true);
            if (type == SubbendingType.ATMOSPHERE) {
                AtmosphereElement.autoUnlockRoots(bender);
            }
            if (type == SubbendingType.GAS) {
                GasElement.autoUnlockRoot(bender);
            }
            if (type == SubbendingType.MIST) {
                MistElement.autoUnlockRoot(bender);
            }
            if (type == SubbendingType.PLASMA) {
                PlasmaElement.autoUnlockRoot(bender);
            }
            if (type == SubbendingType.COMBUSTION) {
                CombustionElement.autoUnlockRoot(bender);
            }
            if (type == SubbendingType.BONE) {
                BoneElement.autoUnlockRoot(bender);
            }
            if (type == SubbendingType.GLASS) {
                GlassElement.autoUnlockRoot(bender);
            }
            syncAndPersist(bender, target);
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

    // ==================== Avatar State ====================

    /** {@code /morebending avatar <player> <true|false>}. */
    private static int setAvatarState(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean enable = BoolArgumentType.getBool(ctx, "state");
        CommandSourceStack source = ctx.getSource();
        String playerName = target.getName().getString();

        PlayerAvatarData avatarData = target.getData(ModAttachments.AVATAR);
        if (enable == avatarData.isAvatarState()) {
            source.sendFailure(Component.literal(playerName + " já estava "
                    + (enable ? "no" : "fora do") + " Avatar State."));
            return 0;
        }

        if (enable) {
            grantAvatarState(source, target, avatarData);
        } else {
            revokeAvatarState(target, avatarData);
        }
        avatarData.setAvatarState(enable);

        source.sendSuccess(() -> Component.literal(playerName + " agora "
                + (enable ? "ESTÁ" : "NÃO está mais") + " no Avatar State."), true);
        target.sendSystemMessage(enable
                ? Component.literal("Você entrou no Avatar State! Todas as bendings estão liberadas.")
                : Component.literal("Você saiu do Avatar State. As bendings concedidas por ele foram removidas."));
        return 1;
    }

    /**
     * Concede os 4 elementos-base (Air, Water, Earth, Fire) e todas as
     * sub-bendings deste addon que o jogador ainda não tem — pulando os
     * pré-requisitos normais de cada uma (ver {@code bypassEligibility} em
     * {@link #runRealElement(CommandSourceStack, ServerPlayer, SubbendingType, boolean, boolean)}),
     * já que o Avatar transcende esses requisitos. Só marca em {@code
     * avatarData} o que REALMENTE foi concedido agora (o jogador não tinha
     * antes) — é isso que permite {@link #revokeAvatarState} desfazer com
     * precisão, sem tocar em nada que já era do jogador por fora.
     */
    public static void grantAvatarState(CommandSourceStack source, ServerPlayer target, PlayerAvatarData avatarData) {
        Bender bender = Bender.getBender(target);

        grantCoreElementIfMissing(bender, avatarData, AirElement.get());
        grantCoreElementIfMissing(bender, avatarData, WaterElement.get());
        grantCoreElementIfMissing(bender, avatarData, EarthElement.get());
        grantCoreElementIfMissing(bender, avatarData, FireElement.get());
        // O Avatar Element em si (as 8 super-moves) -- sem isso, ligar o
        // Avatar State só reforçava os 4 elementos-base e nunca dava as
        // abilities avatarEarthquake/avatarInfernoNova/etc.
        grantCoreElementIfMissing(bender, avatarData, AvatarElement.get());

        for (SubbendingType type : SubbendingType.values()) {
            if (type == SubbendingType.FLYING) {
                continue; // sem Element de verdade -- tratado à parte, abaixo
            }
            Element element = elementFor(type);
            if (element == null || bender.hasElement(element)) {
                continue; // já tinha por fora do Avatar -- não mexe, não marca como concedido por ele
            }
            runRealElement(source, target, type, true, true);
            avatarData.markGrantedElement(element.getName());
        }

        // Flying é flag-only (sem Element) -- concedida direto pelos dados de sub-bending, não via runRealElement.
        PlayerSubbendingData subData = target.getData(ModAttachments.SUBBENDINGS);
        if (subData.grant(SubbendingType.FLYING)) {
            avatarData.markGrantedFlag(SubbendingType.FLYING);
        }

        syncAndPersist(bender, target);
    }

    private static void grantCoreElementIfMissing(Bender bender, PlayerAvatarData avatarData, Element element) {
        if (!bender.hasElement(element)) {
            bender.addElement(element, true);
            avatarData.markGrantedElement(element.getName());
        }
    }

    /**
     * Desfaz só o que {@link #grantAvatarState} concedeu (ver o rastreamento
     * em {@code avatarData}) -- qualquer bending que o jogador já tinha
     * antes de entrar no Avatar State, ou que adquiriu legitimamente
     * enquanto estava nele, NÃO está nesses conjuntos e continua intocada.
     */
    public static void revokeAvatarState(ServerPlayer target, PlayerAvatarData avatarData) {
        Bender bender = Bender.getBender(target);

        for (String elementName : avatarData.getGrantedElements()) {
            Element element = Element.getElement(elementName);
            if (element != null && bender.hasElement(element)) {
                bender.removeElement(element, true);
            }
        }
        avatarData.clearGrantedElements();

        PlayerSubbendingData subData = target.getData(ModAttachments.SUBBENDINGS);
        for (SubbendingType type : avatarData.getGrantedFlagSubbendings()) {
            subData.revoke(type);
        }
        avatarData.clearGrantedFlagSubbendings();

        syncAndPersist(bender, target);
    }
}