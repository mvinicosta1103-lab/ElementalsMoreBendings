package com.elementals.morebendings.registry;

import com.elementals.morebendings.bending.watersubbendings.plant.PlantVineWhipAbility;

public class ModAbilities {

    public static void registerAbilities() {
        // Gas não passa mais por aqui: GasCloudAbility agora é Ability de
        // verdade, adicionada direto no construtor de GasElement (ver
        // CommonClass.init(), que chama GasElement.register()).
        PlantVineWhipAbility.register();
    }
}