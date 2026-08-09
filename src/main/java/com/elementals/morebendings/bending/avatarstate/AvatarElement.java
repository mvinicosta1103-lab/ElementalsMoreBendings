package com.elementals.morebendings.bending.avatarstate;

import com.elementals.morebendings.bending.avatarstate.moves.AvatarCycloneAbility;
import com.elementals.morebendings.bending.avatarstate.moves.AvatarEarthquakeAbility;
import com.elementals.morebendings.bending.avatarstate.moves.AvatarInfernoNovaAbility;
import com.elementals.morebendings.bending.avatarstate.moves.AvatarMaelstromAbility;
import com.elementals.morebendings.bending.avatarstate.moves.AvatarMeteorStormAbility;
import com.elementals.morebendings.bending.avatarstate.moves.AvatarSkyfallAbility;
import com.elementals.morebendings.bending.avatarstate.moves.AvatarStonePillarsAbility;
import com.elementals.morebendings.bending.avatarstate.moves.AvatarTidalWaveAbility;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.Upgrade;

/**
 * "Avatar Element" — só existe enquanto o jogador está no Avatar State
 * (concedido/revogado por {@link AvatarStateManager}, nunca adquirido do
 * jeito normal). Dá acesso a 8 super-moves -- 2 de cada elemento-base
 * (Ar, Água, Fogo, Terra) -- todos MUITO mais fortes/amplos que as
 * versões normais do mod base (ex: earthquake em vez de earth kick, onda
 * gigante em vez de water wave comum). Não dá acesso direto a nenhuma
 * sub-bending deste addon -- só os 4 elementos-base, na versão "Avatar".
 * <p>
 * Estrutura da árvore: exatamente 4 filhos diretos na raiz (limite da UI,
 * mesma regra documentada em {@code LavaElement}/{@code PlantElement}) --
 * um por elemento-base, cada um com a segunda ability daquele elemento
 * aninhada como filho único.
 */
public class AvatarElement extends Element {

    public static final String NAME = "Avatar";

    public AvatarElement() {
        super(NAME, new Upgrade[]{
                new Upgrade("avatarEarthquake", new Upgrade[]{
                        new Upgrade("avatarStonePillars", 0)
                }, 0),
                new Upgrade("avatarTidalWave", new Upgrade[]{
                        new Upgrade("avatarMaelstrom", 0)
                }, 0),
                new Upgrade("avatarInfernoNova", new Upgrade[]{
                        new Upgrade("avatarMeteorStorm", 0)
                }, 0),
                new Upgrade("avatarCyclone", new Upgrade[]{
                        new Upgrade("avatarSkyfall", 0)
                }, 0),
        });
        addAbility(new AvatarEarthquakeAbility(), 0);
        addAbility(new AvatarStonePillarsAbility(), 1);
        addAbility(new AvatarTidalWaveAbility(), 2);
        addAbility(new AvatarMaelstromAbility(), 3);
        addAbility(new AvatarInfernoNovaAbility(), 4);
        addAbility(new AvatarMeteorStormAbility(), 5);
        addAbility(new AvatarCycloneAbility(), 6);
        addAbility(new AvatarSkyfallAbility(), 7);

        registerUpgradeKeybind("avatarEarthquake", 0);
        registerUpgradeKeybind("avatarStonePillars", 1);
        registerUpgradeKeybind("avatarTidalWave", 2);
        registerUpgradeKeybind("avatarMaelstrom", 3);
        registerUpgradeKeybind("avatarInfernoNova", 4);
        registerUpgradeKeybind("avatarMeteorStorm", 5);
        registerUpgradeKeybind("avatarCyclone", 6);
        registerUpgradeKeybind("avatarSkyfall", 7);
    }

    /** Registra a instância única no mod base. Chame uma vez, no load do mod. */
    public static void register() {
        if (Element.getElementList().stream().noneMatch(e -> e.getName().equalsIgnoreCase(NAME))) {
            new AvatarElement();
        }
    }

    public static Element get() {
        return Element.getElement(NAME);
    }

    public static boolean isAvatarBender(Bender bender) {
        return bender.hasElement(get());
    }

    @Override
    public boolean isSkillTreeComplete(Bender bender) {
        return bender.hasElement(this)
                && bender.getData().canUseUpgrade("avatarEarthquake")
                && bender.getData().canUseUpgrade("avatarStonePillars")
                && bender.getData().canUseUpgrade("avatarTidalWave")
                && bender.getData().canUseUpgrade("avatarMaelstrom")
                && bender.getData().canUseUpgrade("avatarInfernoNova")
                && bender.getData().canUseUpgrade("avatarMeteorStorm")
                && bender.getData().canUseUpgrade("avatarCyclone")
                && bender.getData().canUseUpgrade("avatarSkyfall");
    }
}