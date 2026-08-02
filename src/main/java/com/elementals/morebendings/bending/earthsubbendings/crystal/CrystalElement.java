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
 * (ver o comentário detalhado lá sobre a limitação do
 * {@code UpgradeTreeScreen#render()} do mod base, que só desenha
 * {@code root.children[0..3]}). Com crystalShard, crystalSpike,
 * crystalWall e crystalPrison, Crystal chega no limite: qualquer
 * habilidade futura (dash/step, armor, mastery -- já tem ícone pronto em
 * resources/) precisa entrar como {@code children} aninhado dentro de um
 * desses 4, não como um 5º Upgrade solto aqui.
 */
public class CrystalElement extends Element {

    public static final String NAME = "Crystal";

    public CrystalElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("crystalShard", 0),   // grátis -- ver CrystalShardAbility
                new Upgrade("crystalSpike", 0),   // grátis -- ver CrystalSpikeAbility
                new Upgrade("crystalWall", 0),    // grátis -- ver CrystalWallAbility
                new Upgrade("crystalPrison", 0)   // grátis -- ver CrystalPrisonAbility
        });
        addAbility(new CrystalShardAbility(), 0);
        addAbility(new CrystalSpikeAbility(), 1);
        addAbility(new CrystalWallAbility(), 2);
        addAbility(new CrystalPrisonAbility(), 3);
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

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade("crystalShard")
                && bender.getData().canUseUpgrade("crystalSpike")
                && bender.getData().canUseUpgrade("crystalWall")
                && bender.getData().canUseUpgrade("crystalPrison");
    }
}