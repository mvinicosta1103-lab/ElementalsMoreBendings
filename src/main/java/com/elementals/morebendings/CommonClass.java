package com.elementals.morebendings;

import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassElement;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaElement;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import com.elementals.morebendings.bending.earthsubbendings.petrification.PetrificationElement;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandElement;
import com.elementals.morebendings.bending.airsubbendings.atmosphere.AtmosphereElement;
import com.elementals.morebendings.registry.ModAbilities;

public class CommonClass {

    public static void init() {
        Constants.LOG.info("Inicializando sub-bendings comuns (Gas, Plant, Mud, Crystal, Sand, Glass, Petrification, Lava) na 1.21.1...");

        // Mud, Crystal, Sand, Glass, Petrification e Lava agora são
        // Elements de verdade, registrados no mod base (ver
        // MudElement/CrystalElement/SandElement/GlassElement/
        // PetrificationElement/LavaElement) — precisam ser instanciados
        // uma vez aqui pra entrarem no registro estático do Elementals.
        // Glass precisa vir depois de Sand só por organização (não há
        // dependência de ordem de registro real, mas
        // GlassElement.canAcquire consulta SandElement.get() em runtime).

        // EARTH SUBBENDINGS
        MudElement.register();
        CrystalElement.register();
        SandElement.register();
        GlassElement.register();
        PetrificationElement.register();
        LavaElement.register();

        // AIR SUBBENDINGS
        AtmosphereElement.register();

        // Gas/Flying/Plant ainda usam o sistema antigo (PlayerSubbendingData)
        // — ainda não convertidos pro mesmo padrão de Element real.
        ModAbilities.registerAbilities();
    }
}