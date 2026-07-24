package com.elementals.morebendings.registry;

import com.elementals.morebendings.bending.airsubbendings.gas.GasSuffocateAbility;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantVineWhipAbility;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudSurgeAbility;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardAbility;

public class ModAbilities {

    public static void registerAbilities() {
        GasSuffocateAbility.register();
        PlantVineWhipAbility.register();
        MudSurgeAbility.register();
        CrystalShardAbility.register();
    }
}
