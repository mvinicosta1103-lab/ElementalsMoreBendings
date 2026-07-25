package com.elementals.morebendings;

import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalElement;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudElement;
import com.elementals.morebendings.registry.ModAbilities;

public class CommonClass {

    public static void init() {
        Constants.LOG.info("Inicializando sub-bendings comuns (Gas, Plant, Mud, Crystal) na 1.21.1...");

        // Mud e Crystal agora são Elements de verdade, registrados no mod
        // base (ver MudElement/CrystalElement) — precisam ser instanciados
        // uma vez aqui pra entrarem no registro estático do Elementals.
        MudElement.register();
        CrystalElement.register();

        // Gas/Flying/Plant ainda usam o sistema antigo (PlayerSubbendingData)
        // — ainda não convertidos pro mesmo padrão de Element real.
        ModAbilities.registerAbilities();
    }
}