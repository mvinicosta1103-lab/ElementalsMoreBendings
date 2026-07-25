package com.elementals.morebendings.registry;

import com.elementals.morebendings.bending.airsubbendings.gas.GasSuffocateAbility;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantVineWhipAbility;

public class ModAbilities {

    public static void registerAbilities() {
        // Mud e Crystal não passam mais por aqui: MudSurgeAbility e
        // CrystalShardAbility agora são Ability de verdade, adicionadas
        // direto no construtor de MudElement/CrystalElement (ver
        // CommonClass.init(), que chama MudElement.register() /
        // CrystalElement.register() antes desta linha).
        GasSuffocateAbility.register();
        PlantVineWhipAbility.register();
    }
}