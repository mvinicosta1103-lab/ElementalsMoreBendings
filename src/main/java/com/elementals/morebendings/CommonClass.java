package com.elementals.morebendings;

import com.elementals.morebendings.registry.ModAbilities;

public class CommonClass {

    public static void init() {
        Constants.LOG.info("Inicializando sub-bendings comuns (Gas, Plant, Mud, Crystal) na 1.21.1...");
        ModAbilities.registerAbilities();
    }
}
