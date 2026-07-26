package com.elementals.morebendings;

import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassElement;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaElement;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import com.elementals.morebendings.bending.earthsubbendings.petrification.PetrificationElement;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import com.elementals.morebendings.bending.airsubbendings.atmosphere.AtmosphereElement;
import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.registry.ModAbilities;

public class CommonClass {

    public static void init() {
        Constants.LOG.info("Inicializando sub-bendings comuns (Plant, Mud, Crystal, Sand, Glass, Petrification, Lava, Atmosphere, Gas) na 1.21.1...");

        // EARTH SUBBENDINGS
        MudElement.register();
        CrystalElement.register();
        SandElement.register();
        GlassElement.register();
        PetrificationElement.register();
        LavaElement.register();

        // AIR SUBBENDINGS
        AtmosphereElement.register();
        GasElement.register();

        // Flying/Plant ainda usam o sistema antigo (PlayerSubbendingData)
        // — ainda não convertidos pro mesmo padrão de Element real.
        ModAbilities.registerAbilities();
    }
}