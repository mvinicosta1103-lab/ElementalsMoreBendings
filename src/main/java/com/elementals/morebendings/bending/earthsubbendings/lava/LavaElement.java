package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;

/**
 * Lava Bending — sub-bending de Earth, registrada como um {@link Element}
 * de verdade no mod base.
 *
 * === CAUSA RAIZ DO BUG (achada via decompilação da lib base) ===
 * dev.saperate.elementals.client.gui.UpgradeTreeScreen#render() desenha
 * a árvore de skills chamando, na raiz, EXATAMENTE:
 *   root.children[0] -> drawTree         (ramo "baixo")
 *   root.children[1] -> drawMirroredTree (ramo "esquerda")
 *   root.children[2] -> drawMirroredTree (ramo "direita")
 *   root.children[3] -> drawTree         (ramo "cima")
 * Não existe loop -- são 4 chamadas fixas, gated por
 * `if (len >= 1/2/3)` e `if (len == 4)`. Qualquer índice a partir do 4
 * simplesmente nunca é lido nem desenhado (nenhuma exceção é lançada;
 * addAbility() também não lança nada, então o patch de diagnóstico
 * anterior nunca pegaria nada de errado ali).
 *
 * TODOS os elementos originais (Water, Fire, Earth, Air, Blood, Metal,
 * Lightning) seguem a mesma convenção: o array passado pro construtor
 * de Element tem SEMPRE 4 Upgrades diretos. As demais habilidades entram
 * como `children` aninhados dentro desses 4 ramos -- aí sim o desenho é
 * recursivo e sem limite (drawTree/drawMirroredTree percorrem
 * `parent.children` em loop livre).
 *
 * O LavaElement original passava 7 Upgrades soltos direto na raiz, por
 * isso só lavaPool/lavaJet/magmaSpike (índices 0-2) apareciam e
 * lavaShuriken/lavaSurf/volcanicEruption/lavaArmor (índices 3-6) eram
 * ignorados silenciosamente pelo renderer.
 *
 * Correção: reagrupar em 4 ramos diretos, aninhando o resto como filhos
 * de cada ramo (igual ao padrão do jogo base).
 */
public class LavaElement extends Element {

    public static final String NAME = "Lava";

    public static final String LAVA_POOL = "lavaPool";
    public static final String LAVA_JET = "lavaJet";
    public static final String MAGMA_SPIKE = "magmaSpike";
    public static final String LAVA_SHURIKEN = "lavaShuriken";
    public static final String LAVA_SURF = "lavaSurf";
    public static final String VOLCANIC_ERUPTION = "volcanicEruption";
    public static final String LAVA_ARMOR = "lavaArmor";

    public LavaElement() {
        // Exatamente 4 filhos diretos na raiz -- é o máximo que
        // UpgradeTreeScreen#render() desenha (root.children[0..3]).
        // As outras 3 habilidades vão como `children` aninhados dentro
        // de um desses 4 ramos, o que as empurra mais pra fora na
        // mesma direção -- exatamente como Water/Fire/Earth/etc. fazem
        // com suas dezenas de upgrades.
        super(NAME, new Upgrade[]{
                new Upgrade(LAVA_POOL, new Upgrade[]{
                        new Upgrade(LAVA_ARMOR, 0)
                }, 0),
                new Upgrade(LAVA_JET, new Upgrade[]{
                        new Upgrade(LAVA_SURF, 0)
                }, 0),
                new Upgrade(MAGMA_SPIKE, new Upgrade[]{
                        new Upgrade(VOLCANIC_ERUPTION, 0)
                }, 0),
                new Upgrade(LAVA_SHURIKEN, 0)
        });

        addAbility(new LavaPoolAbility(), 0);
        addAbility(new LavaJetAbility(), 1);
        addAbility(new MagmaSpikeAbility(), 2);
        addAbility(new LavaShurikenAbility(), 3);
        addAbility(new LavaSurfAbility(), 4);
        addAbility(new VolcanicEruptionAbility(), 5);
        addAbility(new LavaArmorAbility(), 6);

        // Sem isso, Element#getKeybindSlotForUpgrade() sobe a árvore, não
        // acha nada em upgradeKeybinds e cai pro índice do RAMO da raiz
        // (0-3) em vez do índice real da ability (0-6). Como lavaArmor e
        // lavaSurf/volcanicEruption estão aninhados como filhos dentro de
        // outro ramo (ver construtor acima), isso fazia duas habilidades
        // diferentes mostrarem a mesma tecla na tooltip (ex: lavaPool e
        // lavaArmor os dois exibindo "R"). Registrando explicitamente
        // cada upgrade -> índice real da ability, exatamente como
        // WaterElement/FireElement/etc. fazem para cada uma das suas.
        registerUpgradeKeybind(LAVA_POOL, 0);
        registerUpgradeKeybind(LAVA_JET, 1);
        registerUpgradeKeybind(MAGMA_SPIKE, 2);
        registerUpgradeKeybind(LAVA_SHURIKEN, 3);
        registerUpgradeKeybind(LAVA_SURF, 4);
        registerUpgradeKeybind(VOLCANIC_ERUPTION, 5);
        registerUpgradeKeybind(LAVA_ARMOR, 6);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new LavaElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Earth E já comprou todos os nós da
     * árvore de skills de Earth (masterizou o elemento base).
     */
    public static boolean canAcquire(Bender bender) {
        Element earth = EarthElement.get();
        return bender.hasElement(earth) && earth.isSkillTreeComplete(bender);
    }

    public static boolean isLavaBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(LAVA_POOL)
                && bender.getData().canUseUpgrade(LAVA_JET)
                && bender.getData().canUseUpgrade(MAGMA_SPIKE)
                && bender.getData().canUseUpgrade(LAVA_SHURIKEN)
                && bender.getData().canUseUpgrade(LAVA_SURF)
                && bender.getData().canUseUpgrade(VOLCANIC_ERUPTION)
                && bender.getData().canUseUpgrade(LAVA_ARMOR);
    }
}