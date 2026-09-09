package com.elementals.morebendings.bending.avatarstate;

import com.elementals.morebendings.bending.airsubbendings.atmosphere.AtmosphereElement;
import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import com.elementals.morebendings.bending.airsubbendings.sound.SoundElement;
import com.elementals.morebendings.bending.airsubbendings.temperature.TemperatureElement;
import com.elementals.morebendings.bending.airsubbendings.voiding.VoidElement;
import com.elementals.morebendings.bending.earthsubbendings.bone.BoneElement;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassElement;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaElement;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import com.elementals.morebendings.bending.earthsubbendings.petrification.PetrificationElement;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionElement;
import com.elementals.morebendings.bending.firesubbendings.plasma.PlasmaElement;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantElement;
import com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement;
import com.elementals.morebendings.commands.MoreBendingCommand;
import com.elementals.morebendings.data.SubbendingType;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.air.AirElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.elements.fire.FireElement;
import dev.saperate.elementals.elements.water.WaterElement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Constrói e aplica as opções de Energybend (4 elementos-base + qualquer
 * sub-bending deste addon que já faça sentido pro alvo) mostradas na GUI
 * do Avatar State (ver {@code AvatarBendingChoiceScreen} no lado cliente,
 * {@link AvatarBendingFreezeManager} pro congelamento do alvo enquanto
 * escolhe). Substitui a antiga seleção cíclica fixa de {@code
 * AvatarBendingSelection} -- agora o Avatar pode escolher QUALQUER
 * elemento/sub-bending a cada uso, não só o que estava "selecionado".
 * <p>
 * FLYING é a única sub-bending de fora da lista: diferente das outras, ela
 * não tem um {@code Element} de verdade no mod base (é só uma flag em
 * {@code PlayerSubbendingData}, ver {@code FlyingElement}), então não se
 * encaixa no mesmo fluxo de concessão/remoção via {@code Bender}.
 */
public final class AvatarBendingOptions {

    private static final float GRANT_CHI_COST = 20.0f;
    private static final float REMOVE_CHI_COST = 25.0f;

    /** Formato de fio do pacote (ver {@code OpenAvatarBendingChoicePacket}): entradas separadas por ";;", campos por "|". */
    private static final String ENTRY_SEP = ";;";
    private static final String FIELD_SEP_REGEX = "\\|";
    private static final String FIELD_SEP = "|";

    private AvatarBendingOptions() {
    }

    // ---------------------------------------------------------------
    // Fluxo de abertura (chamado pelas Abilities de Grant/Remove)
    // ---------------------------------------------------------------

    /**
     * Acha o alvo mirado, congela ele e manda a GUI de escolha pro caster.
     * Chamado por {@code AvatarBendingGrantAbility}/{@code
     * AvatarBendingRemoveAbility} -- {@code grantMode} decide só o TÍTULO e
     * a validação mostrados na GUI; a aplicação de verdade só acontece
     * depois, quando a escolha volta ({@code ChooseAvatarBendingPacket}).
     */
    public static void beginChoice(ServerPlayer caster, boolean grantMode) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer target = AvatarBendingTargeting.raycastPlayerTarget(caster, level);
        if (target == null) {
            caster.displayClientMessage(Component.literal("§7No player in sight."), true);
            return;
        }
        if (target == caster) {
            caster.displayClientMessage(Component.literal("§7You can't use this on yourself."), true);
            return;
        }

        AvatarBendingFreezeManager.begin(caster, target);

        String blob = buildOptionsBlob(target);
        commonnetwork.api.Dispatcher.sendToClient(
                new com.elementals.morebendings.network.packets.OpenAvatarBendingChoicePacket(
                        grantMode, target.getUUID(), target.getName().getString(), blob),
                caster);
    }

    // ---------------------------------------------------------------
    // Construção da lista de opções
    // ---------------------------------------------------------------

    public static String baseSymbol(Element element) {
        if (element == WaterElement.get()) return "WATER";
        if (element == EarthElement.get()) return "EARTH";
        if (element == FireElement.get()) return "FIRE";
        return "AIR";
    }

    public static Element baseFromSymbol(String symbol) {
        return switch (symbol) {
            case "WATER" -> WaterElement.get();
            case "EARTH" -> EarthElement.get();
            case "FIRE" -> FireElement.get();
            default -> AirElement.get();
        };
    }

    public static String baseDisplayName(Element element) {
        if (element == WaterElement.get()) return "Water";
        if (element == EarthElement.get()) return "Earth";
        if (element == FireElement.get()) return "Fire";
        return "Air";
    }

    /** Elemento-base do qual cada sub-bending depende (pré-requisito de {@code canAcquire}) -- ver os pacotes de cada Element. */
    private static Element parentBase(SubbendingType type) {
        return switch (type) {
            case PLANT, SPIRIT, ICE -> WaterElement.get();
            case MUD, CRYSTAL, BONE, SAND, GLASS, PETRIFICATION, LAVA -> EarthElement.get();
            case PLASMA, COMBUSTION -> FireElement.get();
            default -> AirElement.get(); // GAS, ATMOSPHERE, MIST, SOUND, TEMPERATURE, VOID
        };
    }

    /**
     * Monta o blob de opções pro alvo: os 4 elementos-base sempre aparecem;
     * cada sub-bending só aparece se o alvo já dominar o elemento-base do
     * qual ela depende (FLYING nunca aparece, ver a doc da classe).
     */
    public static String buildOptionsBlob(ServerPlayer target) {
        Bender bender = Bender.getBender(target);
        List<String> entries = new ArrayList<>();

        for (Element base : new Element[]{AirElement.get(), WaterElement.get(), EarthElement.get(), FireElement.get()}) {
            entries.add("B:" + baseSymbol(base) + FIELD_SEP + baseDisplayName(base) + FIELD_SEP + bender.hasElement(base));
        }

        for (SubbendingType type : SubbendingType.values()) {
            if (type == SubbendingType.FLYING) {
                continue;
            }
            if (!bender.hasElement(parentBase(type))) {
                continue;
            }
            Element element = MoreBendingCommand.elementFor(type);
            boolean hasIt = element != null && bender.hasElement(element);
            entries.add("S:" + type.getId() + FIELD_SEP + type.getDisplayName() + FIELD_SEP + hasIt);
        }

        return String.join(ENTRY_SEP, entries);
    }

    /** Uma opção já decodificada do lado cliente (ver {@code AvatarBendingChoiceScreen}). */
    public record Option(String id, String displayName, boolean alreadyHas) {
    }

    public static List<Option> parseOptionsBlob(String blob) {
        List<Option> options = new ArrayList<>();
        if (blob == null || blob.isEmpty()) {
            return options;
        }
        for (String entry : blob.split(ENTRY_SEP)) {
            String[] fields = entry.split(FIELD_SEP_REGEX, 3);
            if (fields.length != 3) {
                continue;
            }
            options.add(new Option(fields[0], fields[1], Boolean.parseBoolean(fields[2])));
        }
        return options;
    }

    // ---------------------------------------------------------------
    // Aplicação da escolha final (chamado por ChooseAvatarBendingPacket)
    // ---------------------------------------------------------------

    /**
     * Aplica a opção escolhida (id no formato {@code B:<SYMBOL>} ou
     * {@code S:<subbendingId>}, ver {@link #buildOptionsBlob}). Concede o
     * chi (verificado só depois de validar que a ação faz sentido, pra não
     * cobrar o Avatar por uma escolha que ia falhar de qualquer jeito) e
     * manda as mensagens/efeitos pro caster e pro alvo.
     *
     * @return true se a mudança foi aplicada com sucesso.
     */
    public static boolean apply(ServerPlayer caster, ServerPlayer target, String chosenId, boolean grant) {
        if (chosenId.startsWith("B:")) {
            return applyBase(caster, target, baseFromSymbol(chosenId.substring(2)), grant);
        }
        if (chosenId.startsWith("S:")) {
            return applySub(caster, target, chosenId.substring(2), grant);
        }
        return false;
    }

    private static boolean applyBase(ServerPlayer caster, ServerPlayer target, Element element, boolean grant) {
        Bender casterBender = Bender.getBender(caster);
        Bender targetBender = Bender.getBender(target);
        String name = baseDisplayName(element);
        String targetName = target.getName().getString();

        if (grant && targetBender.hasElement(element)) {
            caster.displayClientMessage(Component.literal("§7" + targetName + " already masters " + name + "."), true);
            return false;
        }
        if (!grant && !targetBender.hasElement(element)) {
            caster.displayClientMessage(Component.literal("§7" + targetName + " doesn't master " + name + "."), true);
            return false;
        }

        if (!casterBender.reduceChi(grant ? GRANT_CHI_COST : REMOVE_CHI_COST)) {
            caster.displayClientMessage(Component.literal("§7Not enough chi."), true);
            return false;
        }

        if (grant) {
            targetBender.addElement(element, true);
        } else {
            targetBender.removeElement(element, true);
        }
        MoreBendingCommand.syncAndPersist(targetBender, target);

        playFx(caster, target, grant);
        caster.displayClientMessage(Component.literal(
                (grant ? "§bYou granted " : "§cYou removed ") + name + (grant ? " to " : " from ") + targetName + "."), true);
        target.displayClientMessage(Component.literal(grant
                ? "§bThe Avatar granted you the bending of " + name + "!"
                : "§cThe Avatar stripped you of your " + name + " bending!"), true);
        return true;
    }

    /**
     * Reaproveita {@code /morebending grant|remove} (ver {@link
     * MoreBendingCommand}) em vez de duplicar as regras específicas de cada
     * sub-bending (auto-unlock de nó raiz, o passe automático de Bone no
     * requisito de Blood proximity, etc.) -- roda com uma fonte de comando
     * elevada e sem eco no console/log. A elegibilidade (mastery da árvore
     * base) já é checada ANTES de cobrar chi, então o Avatar nunca paga por
     * uma tentativa que o comando ia recusar de qualquer jeito.
     */
    private static boolean applySub(ServerPlayer caster, ServerPlayer target, String subId, boolean grant) {
        Optional<SubbendingType> typeOpt = SubbendingType.byId(subId);
        if (typeOpt.isEmpty()) {
            return false;
        }
        SubbendingType type = typeOpt.get();
        Element element = MoreBendingCommand.elementFor(type);
        if (element == null) {
            return false;
        }

        Bender casterBender = Bender.getBender(caster);
        Bender targetBender = Bender.getBender(target);
        String name = type.getDisplayName();
        String targetName = target.getName().getString();

        if (grant) {
            if (targetBender.hasElement(element)) {
                caster.displayClientMessage(Component.literal("§7" + targetName + " already masters " + name + "."), true);
                return false;
            }
            if (!isEligible(targetBender, type)) {
                caster.displayClientMessage(Component.literal(
                        "§7" + targetName + " isn't ready for " + name + " yet."), true);
                return false;
            }
        } else if (!targetBender.hasElement(element)) {
            caster.displayClientMessage(Component.literal("§7" + targetName + " doesn't master " + name + "."), true);
            return false;
        }

        if (!casterBender.reduceChi(grant ? GRANT_CHI_COST : REMOVE_CHI_COST)) {
            caster.displayClientMessage(Component.literal("§7Not enough chi."), true);
            return false;
        }

        CommandSourceStack source = caster.getServer().createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        String command = "morebending " + (grant ? "grant" : "remove") + " "
                + target.getGameProfile().getName() + " " + type.getId();
        int result;
        try {
            var parseResults = caster.getServer().getCommands().getDispatcher().parse(command, source);
            result = caster.getServer().getCommands().getDispatcher().execute(parseResults);
        } catch (Exception e) {
            result = 0;
        }
        if (result <= 0) {
            // Não deveria acontecer (já validamos hasElement/elegibilidade
            // acima), mas por segurança não deixa o Avatar sem chi à toa.
            caster.displayClientMessage(Component.literal("§7Something went wrong -- try again."), true);
            return false;
        }

        playFx(caster, target, grant);
        caster.displayClientMessage(Component.literal(
                (grant ? "§bYou granted " : "§cYou removed ") + name + (grant ? " to " : " from ") + targetName + "."), true);
        return true;
    }

    /**
     * Mesma checagem de {@code canAcquire} usada por {@code
     * /morebending grant} -- BONE é sempre elegível aqui porque o comando
     * reaproveitado já seta {@code metBloodBender} antes de checar (mesmo
     * passe automático que o comando normal já dá).
     */
    private static boolean isEligible(Bender bender, SubbendingType type) {
        if (type == SubbendingType.BONE) {
            return true;
        }
        return switch (type) {
            case MUD -> MudElement.canAcquire(bender);
            case CRYSTAL -> CrystalElement.canAcquire(bender);
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
    }

    /** FX simplificado (burst único no alvo) -- trocou o feixe multi-tick original pela confirmação instantânea da GUI. */
    private static void playFx(ServerPlayer caster, ServerPlayer target, boolean grant) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        if (grant) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.5f);
            level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(0xFFD700).toVector3f(), 1.3f),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 40, 0.4, 0.6, 0.4, 0.02);
        } else {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 0.7f);
            level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(0x400040).toVector3f(), 1.3f),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 24, 0.4, 0.6, 0.4, 0.02);
        }
    }
}