package com.elementals.morebendings.data;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cada sub-bending que este addon adiciona. O "id" é o que o jogador digita
 * no comando /morebending (ex: "gas", "plant", "mud", "crystal") e também
 * o que é salvo no NBT do jogador — não mude os ids depois que alguém já
 * tiver dados salvos, ou eles vão "perder" a sub-bending sem querer.
 */
public enum SubbendingType {
    GAS("Gas", "Gas Bending"),
    FLYING("Flying", "Flying"),
    PLANT("Plant", "Plant Bending"),
    MUD("Mud", "Mud Bending"),
    CRYSTAL("Crystal", "Crystal Bending"),
    BONE("Bone", "Bone Bending"),
    SAND("Sand", "Sand Bending"),
    GLASS("Glass", "Glass Bending"),
    PETRIFICATION("Petrification", "Petrification Bending"),
    LAVA("Lava", "Lava Bending"),
    ATMOSPHERE("Atmosphere", "Atmosphere Bending"),
    MIST("Mist", "Mist Bending");

    private final String id;
    private final String displayName;

    SubbendingType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<SubbendingType> byId(String id) {
        for (SubbendingType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Usado pelo autocomplete do comando /morebending. */
    public static List<String> ids() {
        return Arrays.stream(values())
                .map(SubbendingType::getId)
                .collect(Collectors.toList());
    }
}