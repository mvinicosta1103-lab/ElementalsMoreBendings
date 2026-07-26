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
    GAS("gas", "Gas Bending"),
    FLYING("flying", "Flying"),
    PLANT("plant", "Plant Bending"),
    MUD("mud", "Mud Bending"),
    CRYSTAL("crystal", "Crystal Bending"),
    BONE("bone", "Bone Bending"),
    SAND("sand", "Sand Bending"),
    GLASS("glass", "Glass Bending"),
    PETRIFICATION("petrification", "Petrification Bending");

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