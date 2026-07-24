package com.elementals.morebendings.client;

import java.util.ArrayList;
import java.util.List;

public class ClientGasProgress {
    public static int points = 0;
    public static List<String> unlockedUpgrades = new ArrayList<>();

    public static void update(int newPoints, List<String> upgrades) {
        points = newPoints;
        unlockedUpgrades = upgrades;
    }
}