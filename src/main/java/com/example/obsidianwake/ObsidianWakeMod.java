package com.example.obsidianwake;

import com.example.obsidianwake.abilities.ObsidianFormAbility;
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
 * Este mod não modifica o addon Elementals Subbending. Em vez disso, quando
 * todos os mods já terminaram de carregar (FMLLoadCompleteEvent — nesse
 * ponto o LavaElement do addon já existe), ele pega o Element "Lava" já
 * registrado e:
 *   1) acrescenta um novo nó "obsidianForm" na árvore de upgrades dele
 *   2) registra a ObsidianFormAbility como uma habilidade bindável do Lava
 *
 * Se o addon Elementals Subbending não estiver instalado (ou não tiver
 * criado o elemento "Lava"), este mod simplesmente não faz nada — não
 * derruba o jogo.
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
                    "Subbending está instalado? A habilidade Obsidian Wake não será adicionada.");
            return;
        }

        Upgrade lavaFlowNode = lava.root.getUpgradeByNameRecursive("lavaFlow");
        if (lavaFlowNode == null) {
            LOGGER.warn("[ObsidianWake] Nó 'lavaFlow' não encontrado na árvore da Lava.");
            return;
        }

        Upgrade obsidianForm = new Upgrade("obsidianForm", new Upgrade[]{
                new Upgrade("obsidianFormRadiusI", new Upgrade[]{
                        new Upgrade("obsidianFormRadiusII", 2)
                }, 1),
                new Upgrade("obsidianFormSpeedI", 1)
        }, 2);
        obsidianForm.setParent(lavaFlowNode);

        Upgrade[] expanded = Arrays.copyOf(lavaFlowNode.children, lavaFlowNode.children.length + 1);
        expanded[expanded.length - 1] = obsidianForm;
        lavaFlowNode.children = expanded;

        lava.addAbility(new ObsidianFormAbility(), true);
        lava.root.calculateXPos();

        LOGGER.info("[ObsidianWake] Habilidade Obsidian Wake adicionada ao ramo lavaFlow da Lava.");
    }

}
