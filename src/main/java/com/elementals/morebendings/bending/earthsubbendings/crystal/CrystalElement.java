package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.elements.earth.EarthElement;

/**
 * Crystal Bending — sub-bending de Earth, mesmo padrão de {@link
 * com.elementals.morebendings.bending.earthsubbendings.mud.MudElement}:
 * Element de verdade, registrada no mod base, gated atrás da masterização
 * de Earth.
 *
 * Exatamente 4 filhos diretos na raiz -- mesmo teto de {@code MudElement}
 * (ver o comentário detalhado em {@code LavaElement} sobre a limitação do
 * {@code UpgradeTreeScreen#render()} do mod base, que só desenha
 * {@code root.children[0..3]}). crystalStep, crystalArmor e crystalMastery
 * (as 3 habilidades cujos ícones já estavam prontos em resources/, mas
 * nunca tinham sido implementadas) entram como {@code children} aninhados
 * dentro de crystalShard/crystalWall/crystalSpike, respectivamente --
 * exatamente como LavaElement faz com lavaArmor/lavaSurf/volcanicEruption/
 * obsidianCrust.
 */
public class CrystalElement extends Element {

    public static final String NAME = "Crystal";

    public static final String CRYSTAL_SHARD = "crystalShard";
    public static final String CRYSTAL_SPIKE = "crystalSpike";
    public static final String CRYSTAL_WALL = "crystalWall";
    public static final String CRYSTAL_PRISON = "crystalPrison";
    public static final String CRYSTAL_STEP = "crystalStep";
    public static final String CRYSTAL_ARMOR = "crystalArmor";
    /** Nó passivo -- sem Ability própria, ver {@link #hasMastery}. */
    public static final String CRYSTAL_MASTERY = "crystalMastery";

    public CrystalElement() {
        super(NAME, new Upgrade[]{
                new Upgrade(CRYSTAL_SHARD, new Upgrade[]{
                        new Upgrade(CRYSTAL_STEP, 0)   // grátis -- ver CrystalStepAbility
                }, 0),
                new Upgrade(CRYSTAL_SPIKE, new Upgrade[]{
                        new Upgrade(CRYSTAL_MASTERY, 0) // grátis -- passivo, ver hasMastery()
                }, 0),
                new Upgrade(CRYSTAL_WALL, new Upgrade[]{
                        new Upgrade(CRYSTAL_ARMOR, 0)  // grátis -- ver CrystalArmorAbility
                }, 0),
                new Upgrade(CRYSTAL_PRISON, 0)         // grátis -- ver CrystalPrisonAbility
        });
        addAbility(new CrystalShardAbility(), 0);
        addAbility(new CrystalSpikeAbility(), 1);
        addAbility(new CrystalWallAbility(), 2);
        addAbility(new CrystalPrisonAbility(), 3);
        addAbility(new CrystalStepAbility(), 4);
        addAbility(new CrystalArmorAbility(), 5);
        // crystalMastery não tem Ability/keybind -- é consultada direto via
        // canUseUpgrade() por CrystalShardAbility/CrystalSpikeAbility pra
        // dar bônus de dano depois que o jogador compra esse nó.

        // Registro explícito dos slots de bind (mesmo motivo documentado em
        // LavaElement): sem isso, getKeybindSlotForUpgrade() dos ramos
        // aninhados (crystalStep, crystalArmor) cairia pro índice do RAMO
        // da raiz em vez do índice real da ability, fazendo duas
        // habilidades diferentes mostrarem a mesma tecla na tooltip.
        registerUpgradeKeybind(CRYSTAL_SHARD, 0);
        registerUpgradeKeybind(CRYSTAL_SPIKE, 1);
        registerUpgradeKeybind(CRYSTAL_WALL, 2);
        registerUpgradeKeybind(CRYSTAL_PRISON, 3);
        registerUpgradeKeybind(CRYSTAL_STEP, 4);
        registerUpgradeKeybind(CRYSTAL_ARMOR, 5);
    }

    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new CrystalElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    public static boolean canAcquire(Bender bender) {
        Element earth = EarthElement.get();
        return bender.hasElement(earth) && earth.isSkillTreeComplete(bender);
    }

    public static boolean isCrystalBender(Bender bender) {
        return bender.hasElement(get());
    }

    /** @return true se o bender tiver Crystal e já tiver comprado o nó passivo crystalMastery. */
    public static boolean hasMastery(Bender bender) {
        return bender.hasElement(get()) && bender.getData().canUseUpgrade(CRYSTAL_MASTERY);
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade(CRYSTAL_SHARD)
                && bender.getData().canUseUpgrade(CRYSTAL_SPIKE)
                && bender.getData().canUseUpgrade(CRYSTAL_WALL)
                && bender.getData().canUseUpgrade(CRYSTAL_PRISON)
                && bender.getData().canUseUpgrade(CRYSTAL_STEP)
                && bender.getData().canUseUpgrade(CRYSTAL_ARMOR)
                && bender.getData().canUseUpgrade(CRYSTAL_MASTERY);
    }
}