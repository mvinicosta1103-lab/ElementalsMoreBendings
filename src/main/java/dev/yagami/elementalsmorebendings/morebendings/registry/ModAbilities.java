package com.elementals.morebendings.registry;

import com.elementals.morebendings.bending.gas.GasSuffocateAbility;
import com.elementals.morebendings.bending.plant.PlantVineWhipAbility;
import com.elementals.morebendings.bending.mud.MudSurgeAbility;
import com.elementals.morebendings.bending.crystal.CrystalShardAbility;

public class ModAbilities {

    public static void registerAbilities() {
        GasSuffocateAbility.register();
        PlantVineWhipAbility.register();
        MudSurgeAbility.register();
        CrystalShardAbility.register();
    }
}
