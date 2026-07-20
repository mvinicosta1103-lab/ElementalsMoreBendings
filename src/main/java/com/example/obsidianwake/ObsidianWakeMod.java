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
 * e acrescenta novos nós de topo na árvore de upgrades + registra as
 * habilidades novas como bindáveis.
 *
 * IMPORTANTE sobre keybinds: o Elementals só tem 4 slots de tecla por
 * elemento (bind1-bind4). O addon do Jsumpter já ocupa os 4 com lavaFlow,
 * lavaSpike, magmaArmor e lavaShuriken. Nenhuma habilidade nova adicionada
 * por outro mod ganha uma tecla automaticamente — isso é assim mesmo pro
 * addon original, não é um bug daqui. O jogador precisa abrir o mesmo menu
 * de vínculo de habilidades que já usou pras 4 originais e trocar um dos
 * slots pra uma das novas (obsidianForm, eruption, lavaSurf, moltenGrip,
 * obsidianPillar).
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

        Upgrade[] newNodes = new Upgrade[]{
                buildObsidianForm(),
                buildEruption(),
                buildLavaSurf(),
                buildMoltenGrip(),
                buildObsidianPillar()
        };

        for (Upgrade node : newNodes) {
            node.setParent(lava.root);
        }

        Upgrade[] expanded = Arrays.copyOf(lava.root.children, lava.root.children.length + newNodes.length);
        System.arraycopy(newNodes, 0, expanded, lava.root.children.length, newNodes.length);
        lava.root.children = expanded;

        lava.addAbility(new ObsidianFormAbility(), true);
        lava.addAbility(new EruptionAbility(), true);
        lava.addAbility(new LavaSurfAbility(), true);
        lava.addAbility(new MoltenGripAbility(), true);
        lava.addAbility(new ObsidianPillarAbility(), true);

        lava.root.calculateXPos();

        LOGGER.info("[ObsidianWake] 5 novas habilidades adicionadas à árvore de Lava " +
                "(obsidianForm, eruption, lavaSurf, moltenGrip, obsidianPillar). " +
                "Lembre-se: elas precisam ser vinculadas manualmente a um dos 4 slots de tecla.");
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
