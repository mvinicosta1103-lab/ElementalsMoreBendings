package com.elementals.morebendings.bending.watersubbendings.ice;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.water.WaterElement;

/**
 * Ice Bending — sub-bending de Water, mesmo padrão de {@link
 * com.elementals.morebendings.bending.watersubbendings.spirit.SpiritElement}
 * e {@link com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement}:
 * Element de verdade, registrado no mod base, gated atrás da masterização
 * de Water. Focada em ataque (espinhos + projéteis), 3 habilidades raiz:
 *
 *  - iceSpike: AoE ofensiva de curto alcance -- erupção de espinhos de
 *    gelo no chão ao redor do ponto mirado, que congelam e arremessam
 *    quem estiver em cima. Mesmo esquema de {@code CrystalSpikeAbility}
 *    (bloco trocado + {@link IceSpikeVisualEntity} por cima, revertido
 *    depois de {@link IceSpikeManager}). Ver {@link IceSpikeAbility}.
 *  - iceShard: barragem de estilhaços de gelo de verdade ({@link
 *    IceShardEntity}), mesmo esquema de {@code CrystalShardEntity} --
 *    cada um é uma entidade com hitbox própria, que pode errar o alvo.
 *  - frostNova: rajada instantânea de gelo ao redor do caster -- dano +
 *    imobilização curta (lentidão pesada), e congela água próxima. Não
 *    precisa de bloco/entidade nenhum, é só uma AoE instantânea.
 *
 * iceMastery é um nó passivo aninhado embaixo de iceSpike (sem Ability
 * própria) que dá bônus de dano/raio pra iceSpike e iceShard -- mesmo
 * esquema de {@code crystalMastery}.
 */
public class IceElement extends Element {

    public static final String NAME = "Ice";

    public static final String ICE_SPIKE = "iceSpike";
    public static final String ICE_SHARD = "iceShard";
    public static final String FROST_NOVA = "frostNova";
    /** Nó passivo -- sem Ability própria, ver {@link #hasMastery}. */
    public static final String ICE_MASTERY = "iceMastery";
    /** Nó filho de {@link #ICE_SHARD} -- ver {@link IceFormAbility}. */
    public static final String ICE_FORM = "iceForm";
    /** Nó filho de {@link #FROST_NOVA} -- ver {@link IcePrisonAbility}. */
    public static final String ICE_PRISON = "icePrison";
    /**
     * Novo nó RAIZ -- ver {@link IceRingAbility}. Anel de gelo que gira ao
     * redor do bender: Left Click arremessa um estilhaço por vez (até
     * {@code IceRingAbility.MAX_SHARDS}), Right Click descarrega todos os
     * estilhaços restantes de uma vez e desfaz o anel.
     */
    public static final String ICE_RING = "iceRing";

    public IceElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(ICE_SPIKE, new Upgrade[]{
                        new Upgrade(ICE_MASTERY, 0) // grátis -- passivo, ver hasMastery()
                }, 0),
                new Upgrade(ICE_SHARD, new Upgrade[]{
                        new Upgrade(ICE_FORM, 0)   // ver IceFormAbility
                }, 0),
                new Upgrade(FROST_NOVA, new Upgrade[]{
                        new Upgrade(ICE_PRISON, 0) // ver IcePrisonAbility
                }, 0),
                new Upgrade(ICE_RING, 0) // ver IceRingAbility -- raiz nova, sem filhos
        });
        addAbility(new IceSpikeAbility(), 0);
        addAbility(new IceShardAbility(), 1);
        addAbility(new FrostNovaAbility(), 2);
        addAbility(new IceFormAbility(), 3);
        addAbility(new IcePrisonAbility(), 4);
        addAbility(new IceRingAbility(), 5);
        // iceMastery não tem Ability/keybind -- é consultada direto via
        // canUseUpgrade() por IceSpikeAbility/IceShardAbility pra dar
        // bônus depois que o jogador compra esse nó.

        // Sem isso, Element#getKeybindSlotForUpgrade() sobe a árvore, não
        // acha nada em upgradeKeybinds e cai pro índice do RAMO da raiz
        // (0-2) em vez do índice real da ability (0-4) -- iceForm e
        // icePrison (aninhados) mostrariam a mesma tecla de iceShard/
        // frostNova na tooltip. Registrando explicitamente cada upgrade
        // -> índice real da ability, exatamente como MudElement/LavaElement fazem.
        registerUpgradeKeybind(ICE_SPIKE, 0);
        registerUpgradeKeybind(ICE_SHARD, 1);
        registerUpgradeKeybind(FROST_NOVA, 2);
        registerUpgradeKeybind(ICE_FORM, 3);
        registerUpgradeKeybind(ICE_PRISON, 4);
        registerUpgradeKeybind(ICE_RING, 5);
        // NOTA: índice 5 é uma 6ª tecla dedicada -- confira
        // dev.saperate.elementals.elements.Ability.MAX_KEYBINDS na sua
        // versão do mod base. Se o cap for menor que 6, troque o índice
        // acima por um já usado (ex: reaproveitar 4) ou vincule iceRing a
        // um slot livre direto pela árvore de skills no jogo, do mesmo
        // jeito que o addon do Obsidian Wake resolve isso pro Lava.
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new IceElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    /**
     * @return true se o jogador já tem Water E já comprou todos os nós da
     * árvore de skills de Water (masterizou o elemento base) -- mesma
     * regra de Plant/Spirit/Crystal.
     */
    public static boolean canAcquire(Bender bender) {
        Element water = WaterElement.get();
        return bender.hasElement(water) && water.isSkillTreeComplete(bender);
    }

    public static boolean isIceBender(Bender bender) {
        return bender.hasElement(get());
    }

    /** @return true se o bender tiver Ice e já tiver comprado o nó passivo iceMastery. */
    public static boolean hasMastery(Bender bender) {
        return bender.hasElement(get()) && bender.getData().canUseUpgrade(ICE_MASTERY);
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(ICE_SPIKE)
                && bender.getData().canUseUpgrade(ICE_SHARD)
                && bender.getData().canUseUpgrade(FROST_NOVA)
                && bender.getData().canUseUpgrade(ICE_MASTERY)
                && bender.getData().canUseUpgrade(ICE_FORM)
                && bender.getData().canUseUpgrade(ICE_PRISON)
                && bender.getData().canUseUpgrade(ICE_RING);
    }
}