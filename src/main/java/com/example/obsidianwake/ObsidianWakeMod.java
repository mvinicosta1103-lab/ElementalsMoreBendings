package com.example.obsidianwake;

import com.example.obsidianwake.abilities.EruptionAbility;
import com.example.obsidianwake.abilities.LavaSurfAbility;
import com.example.obsidianwake.abilities.MoltenGripAbility;
import com.example.obsidianwake.abilities.ObsidianFormAbility;
import com.example.obsidianwake.abilities.ObsidianPillarAbility;
import com.mojang.logging.LogUtils;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

import java.util.Arrays;

/**
 * Este mod não modifica o addon Elementals Subbending. Quando todos os mods
 * já terminaram de carregar (FMLLoadCompleteEvent — nesse ponto o
 * LavaElement do addon já existe), ele pega o Element "Lava" já registrado
 * e acrescenta as novas habilidades DENTRO dos ramos que o addon do
 * Jsumpter já criou (lavaFlow, lavaSpike, magmaArmor, lavaShuriken) +
 * registra as habilidades novas como bindáveis.
 *
 * IMPORTANTE — por que os nós ficam dentro dos ramos existentes e não
 * direto na raiz: a UpgradeTreeScreen (a tela da árvore, do próprio
 * Elementals) só sabe desenhar 4 ramos saindo da raiz (um pra cima, um pra
 * esquerda, um pra direita, um pra baixo — o código dela literalmente checa
 * "if (len >= 1)", "if (len >= 2)", "if (len >= 3)" e "if (len == 4)", sem
 * nenhum caso pra mais de 4). O addon do Jsumpter já usa os 4 slots com
 * lavaFlow, lavaSpike, magmaArmor e lavaShuriken. Se a gente adicionasse
 * nós novos como filhos DIRETOS da raiz (root.children), o array passaria
 * de 4 para 9 itens — e aí NENHUM nó de índice 4 em diante é desenhado
 * (bug original), e pior: como len deixa de ser exatamente 4, a condição
 * "if (len == 4)" também vira falsa, e o próprio lavaShuriken (índice 3,
 * original do addon) some da tela junto. Por isso agora cada habilidade
 * nova é anexada como um FILHO A MAIS de um nó já existente dentro de um
 * dos 4 ramos — isso mantém root.children.length em 4 e a árvore desenha
 * normalmente, porque o desenho dentro de cada ramo é recursivo e não tem
 * esse limite.
 */
@Mod(ObsidianWakeMod.MODID)
public class ObsidianWakeMod {

    public static final String MODID = "obsidianwake";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ObsidianWakeMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onLoadComplete);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        Element lava = Element.getElement("Lava");

        if (lava == null || !"Lava".equals(lava.getName())) {
            LOGGER.warn("[ObsidianWake] Elemento 'Lava' não encontrado — o addon Elementals " +
                    "Subbending está instalado? As habilidades do Obsidian Wake não serão adicionadas.");
            return;
        }

        // Anexa cada nó novo dentro de um dos 4 ramos que o addon do
        // Jsumpter já criou, em vez de como filho direto da raiz.
        Upgrade obsidianForm = buildObsidianForm();
        Upgrade eruption = buildEruption();
        Upgrade lavaSurf = buildLavaSurf();
        Upgrade moltenGrip = buildMoltenGrip();
        Upgrade obsidianPillar = buildObsidianPillar();

        attachToBranch(lava.root, "lavaFlow", obsidianForm);
        attachToBranch(lava.root, "lavaSpike", eruption);
        attachToBranch(lava.root, "magmaArmor", lavaSurf);
        attachToBranch(lava.root, "lavaShuriken", moltenGrip);
        attachToBranch(lava.root, "lavaFlow", obsidianPillar); // 2º nó no mesmo ramo, sem problema

        // O addon do Jsumpter já ocupa os slots 0-3 (Ability1-4 = R, G, V, [C]),
        // chamando addAbility(ability, true) pra lavaFlow, lavaSpike, magmaArmor
        // e lavaShuriken nessa ordem. Por isso fixamos as 5 habilidades novas
        // explicitamente nos slots 4 a 8 (Ability5-9), em vez de usar
        // addAbility(ability, true) — que só ia empilhar no próximo índice livre
        // e não deixaria claro qual tecla cada uma usa.
        lava.addAbility(new ObsidianFormAbility(), 4);
        lava.addAbility(new EruptionAbility(), 5);
        lava.addAbility(new LavaSurfAbility(), 6);
        lava.addAbility(new MoltenGripAbility(), 7);
        lava.addAbility(new ObsidianPillarAbility(), 8);

        // Registra qual slot cada ramo novo usa, pra tooltip da árvore
        // ("Use: %tecla%") mostrar a tecla certa em vez de herdar a do
        // ramo onde o nó está pendurado (lavaFlow/lavaSpike/etc).
        lava.registerUpgradeKeybind("obsidianForm", 4);
        lava.registerUpgradeKeybind("eruption", 5);
        lava.registerUpgradeKeybind("lavaSurf", 6);
        lava.registerUpgradeKeybind("moltenGrip", 7);
        lava.registerUpgradeKeybind("obsidianPillar", 8);

        lava.root.calculateXPos();

        LOGGER.info("[ObsidianWake] 5 novas habilidades adicionadas à árvore de Lava " +
                "(obsidianForm=Ability5, eruption=Ability6, lavaSurf=Ability7, " +
                "moltenGrip=Ability8, obsidianPillar=Ability9). Configure essas teclas " +
                "em Opções > Controles, se ainda não tiverem uma tecla atribuída.");
    }

    /**
     * Anexa newNode como um filho a mais do nó já existente chamado
     * branchRootName (um dos 4 filhos diretos da raiz, ou qualquer
     * descendente dele). Preserva root.children.length == 4, que é o que
     * a UpgradeTreeScreen exige pra desenhar o ramo corretamente.
     */
    private static void attachToBranch(Upgrade root, String branchRootName, Upgrade newNode) {
        Upgrade branch = root.getUpgradeByNameRecursive(branchRootName);
        if (branch == null) {
            LOGGER.warn("[ObsidianWake] Ramo '{}' não encontrado na árvore de Lava — " +
                    "nó '{}' não foi adicionado.", branchRootName, newNode.name);
            return;
        }
        Upgrade[] expanded = Arrays.copyOf(branch.children, branch.children.length + 1);
        expanded[branch.children.length] = newNode;
        branch.children = expanded;
        newNode.setParent(branch);
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
}