package com.example.elementalmorebendings;

import com.example.elementalmorebendings.lava.abilities.EruptionAbility;
import com.example.elementalmorebendings.lava.abilities.LavaSurfAbility;
import com.example.elementalmorebendings.lava.abilities.MoltenGripAbility;
import com.example.elementalmorebendings.lava.abilities.ObsidianFormAbility;
import com.example.elementalmorebendings.lava.abilities.ObsidianPillarAbility;
import com.example.elementalmorebendings.plant.abilities.OvergrowthSpikesAbility;
import com.example.elementalmorebendings.plant.abilities.ThornBarrageAbility;
import com.example.elementalmorebendings.plant.abilities.VineArcAbility;
import com.example.elementalmorebendings.mud.abilities.MudElement;
import com.mojang.logging.LogUtils;
import com.example.elementalmorebendings.command.MoreBendingsCommand;
import com.example.elementalmorebendings.command.MoreBendingsElementRegistry;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;
import net.neoforged.neoforge.common.NeoForge;


import java.util.Arrays;

/**
 * ElementalMoreBendingsMod
 * <p>
 * Addon único que NÃO modifica o addon Elementals Subbending. Quando todos
 * os mods já terminaram de carregar (FMLLoadCompleteEvent — nesse ponto os
 * elementos "Lava" e "Plant" do addon já existem), ele pega os dois
 * Elements já registrados e acrescenta habilidades novas DENTRO dos ramos
 * que o addon do Jsumpter já criou, sem tocar no jar original.
 * <p>
 * IMPORTANTE — por que os nós ficam dentro dos ramos existentes e não
 * direto na raiz: a UpgradeTreeScreen (a tela da árvore, do próprio
 * Elementals) só sabe desenhar um número fixo de ramos saindo da raiz — o
 * código dela checa "if (len >= 1)", "if (len >= 2)", "if (len >= 3)" e
 * "if (len == 4)", sem nenhum caso pra mais que isso. Se a gente
 * adicionasse nós novos como filhos DIRETOS da raiz, root.children.length
 * mudaria de valor e a árvore pararia de desenhar corretamente (bug
 * original) — inclusive os nós que já existiam. Por isso cada habilidade
 * nova é anexada como um FILHO A MAIS de um nó já existente dentro de um
 * dos ramos originais — isso mantém root.children.length no valor
 * esperado, porque o desenho dentro de cada ramo é recursivo e não tem
 * esse limite.
 * <p>
 * Lava (5 habilidades novas: Obsidian Wake, Eruption, Lava Surf, Molten
 * Grip, Obsidian Pillar) e Plant (3 habilidades novas: Vine Arc, Thorn
 * Barrage, Overgrowth Spikes) ficam em pacotes separados
 * ({@code lava.abilities} e {@code plant.abilities}), cada um com sua
 * própria classe {@code AbilitySupport} package-private — não há conflito
 * porque os pacotes são diferentes.
 */
@Mod(ElementalMoreBendingsMod.MODID)
public class ElementalMoreBendingsMod {

    public static final String MODID = "elementalmorebendings";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ElementalMoreBendingsMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onLoadComplete);
        NeoForge.EVENT_BUS.addListener(MoreBendingsCommand::onRegisterCommands);

    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        registerMudElement();
        extendLavaTree();
        extendPlantTree();
    }

    private void registerMudElement() {
        new MudElement();
        MoreBendingsElementRegistry.register("Mud");
        LOGGER.info("[ElementalMoreBendings] Elemento 'Mud' registrado com sucesso.");
    }

    // ------------------------------------------------------------------
    // Lava
    // ------------------------------------------------------------------

    private void extendLavaTree() {
        Element lava = Element.getElement("Lava");

        if (lava == null || !"Lava".equals(lava.getName())) {
            LOGGER.warn("[ElementalMoreBendings] Elemento 'Lava' não encontrado — o addon " +
                    "Elementals Subbending está instalado? As habilidades novas de Lava não " +
                    "serão adicionadas.");
            return;
        }

        Upgrade obsidianForm = buildObsidianForm();
        Upgrade eruption = buildEruption();
        Upgrade lavaSurf = buildLavaSurf();
        Upgrade moltenGrip = buildMoltenGrip();
        Upgrade obsidianPillar = buildObsidianPillar();

        attachToBranch(lava.root, "lavaFlow", obsidianForm, "Lava");
        attachToBranch(lava.root, "lavaSpike", eruption, "Lava");
        attachToBranch(lava.root, "magmaArmor", lavaSurf, "Lava");
        attachToBranch(lava.root, "lavaShuriken", moltenGrip, "Lava");
        attachToBranch(lava.root, "lavaFlow", obsidianPillar, "Lava"); // 2º nó no mesmo ramo, sem problema

        // O addon do Jsumpter já ocupa os slots 0-3 (Ability1-4) com
        // lavaFlow, lavaSpike, magmaArmor e lavaShuriken. As 5 novas ficam
        // fixas nos slots 4-8.
        lava.addAbility(new ObsidianFormAbility(), 4);
        lava.addAbility(new EruptionAbility(), 5);
        lava.addAbility(new LavaSurfAbility(), 6);
        lava.addAbility(new MoltenGripAbility(), 7);
        lava.addAbility(new ObsidianPillarAbility(), 8);

        lava.registerUpgradeKeybind("obsidianForm", 4);
        lava.registerUpgradeKeybind("eruption", 5);
        lava.registerUpgradeKeybind("lavaSurf", 6);
        lava.registerUpgradeKeybind("moltenGrip", 7);
        lava.registerUpgradeKeybind("obsidianPillar", 8);

        lava.root.calculateXPos();

        LOGGER.info("[ElementalMoreBendings] 5 novas habilidades adicionadas à árvore de Lava " +
                "(obsidianForm=Ability5, eruption=Ability6, lavaSurf=Ability7, " +
                "moltenGrip=Ability8, obsidianPillar=Ability9). Configure essas teclas em " +
                "Opções > Controles, se ainda não tiverem uma tecla atribuída.");
    }

    private static Upgrade buildObsidianForm() {
        return new Upgrade("obsidianForm", new Upgrade[]{
                new Upgrade("obsidianFormRadiusI", new Upgrade[]{
                        new Upgrade("obsidianFormRadiusII", 2)
                }, 1),
                new Upgrade("obsidianFormSpeedI", 1)
        }, 2);
    }

    private static Upgrade buildEruption() {
        return new Upgrade("eruption", new Upgrade[]{
                new Upgrade("eruptionRadiusI", new Upgrade[]{
                        new Upgrade("eruptionRadiusII", 2)
                }, 1),
                new Upgrade("eruptionPowerI", 2)
        }, 3);
    }

    private static Upgrade buildLavaSurf() {
        return new Upgrade("lavaSurf", new Upgrade[]{
                new Upgrade("lavaSurfSpeedI", 1)
        }, 2);
    }

    private static Upgrade buildMoltenGrip() {
        return new Upgrade("moltenGrip", new Upgrade[]{
                new Upgrade("moltenGripRangeI", new Upgrade[]{
                        new Upgrade("moltenGripRangeII", 2)
                }, 1),
                new Upgrade("moltenGripPowerI", 1)
        }, 2);
    }

    private static Upgrade buildObsidianPillar() {
        return new Upgrade("obsidianPillar", new Upgrade[]{
                new Upgrade("obsidianPillarTallI", 2)
        }, 3);
    }

    // ------------------------------------------------------------------
    // Plant
    // ------------------------------------------------------------------

    private void extendPlantTree() {
        Element plant = Element.getElement("Plant");

        if (plant == null || !"Plant".equals(plant.getName())) {
            LOGGER.warn("[ElementalMoreBendings] Elemento 'Plant' não encontrado — o addon " +
                    "Elementals Subbending está instalado? As habilidades novas de Plant não " +
                    "serão adicionadas.");
            return;
        }

        Upgrade vineArc = buildVineArc();
        Upgrade thornBarrage = buildThornBarrage();
        Upgrade overgrowthSpikes = buildOvergrowthSpikes();

        attachToBranch(plant.root, "vineWhip", vineArc, "Plant");
        attachToBranch(plant.root, "rootSnare", thornBarrage, "Plant");
        attachToBranch(plant.root, "plantHealing", overgrowthSpikes, "Plant");

        // Plant original já ocupa os slots 1-3 (vineWhip, rootSnare,
        // plantHealing). As 3 novas ficam nos slots 4-6.
        plant.addAbility(new VineArcAbility(), 4);
        plant.addAbility(new ThornBarrageAbility(), 5);
        plant.addAbility(new OvergrowthSpikesAbility(), 6);

        plant.registerUpgradeKeybind("vineArc", 4);
        plant.registerUpgradeKeybind("thornBarrage", 5);
        plant.registerUpgradeKeybind("overgrowthSpikes", 6);

        plant.root.calculateXPos();

        LOGGER.info("[ElementalMoreBendings] 3 novas habilidades adicionadas à árvore de Plant " +
                "(vineArc=Ability4, thornBarrage=Ability5, overgrowthSpikes=Ability6). Configure " +
                "essas teclas em Opções > Controles, se ainda não tiverem uma tecla atribuída.");
    }

    private static Upgrade buildVineArc() {
        return new Upgrade("vineArc", new Upgrade[]{
                new Upgrade("vineArcRangeI", 1),
                new Upgrade("vineArcPowerI", 2)
        }, 2);
    }

    private static Upgrade buildThornBarrage() {
        return new Upgrade("thornBarrage", new Upgrade[]{
                new Upgrade("thornBarrageRangeI", 1),
                new Upgrade("thornBarragePowerI", 2)
        }, 3);
    }

    private static Upgrade buildOvergrowthSpikes() {
        return new Upgrade("overgrowthSpikes", new Upgrade[]{
                new Upgrade("overgrowthSpikesRadiusI", 1),
                new Upgrade("overgrowthSpikesPowerI", 2)
        }, 3);
    }

    // ------------------------------------------------------------------
    // Compartilhado
    // ------------------------------------------------------------------

    /**
     * Anexa newNode como um filho a mais do nó já existente chamado
     * branchRootName. Preserva root.children.length no valor original, que
     * é o que a UpgradeTreeScreen exige pra desenhar a árvore corretamente.
     */
    private static void attachToBranch(Upgrade root, String branchRootName, Upgrade newNode, String treeLabel) {
        Upgrade branch = root.getUpgradeByNameRecursive(branchRootName);
        if (branch == null) {
            LOGGER.warn("[ElementalMoreBendings] Ramo '{}' não encontrado na árvore de {} — " +
                    "nó '{}' não foi adicionado.", branchRootName, treeLabel, newNode.name);
            return;
        }
        Upgrade[] expanded = Arrays.copyOf(branch.children, branch.children.length + 1);
        expanded[branch.children.length] = newNode;
        branch.children = expanded;
        newNode.setParent(branch);
    }
}