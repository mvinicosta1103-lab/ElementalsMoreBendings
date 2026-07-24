package com.elementals.morebendings.data;

import com.mojang.serialization.Codec;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Guarda quais sub-bendings (Gas, Plant, Mud, Crystal) um jogador específico
 * já desbloqueou. Uma instância disso fica anexada a cada Player via
 * Data Attachment (ver {@link com.elementals.morebendings.registry.ModAttachments}),
 * então persiste entre logins/reinicializações do servidor.
 */
public class PlayerSubbendingData {

    /** Converte pra/de uma lista de ids (Strings) — formato salvo no NBT. */
    public static final Codec<PlayerSubbendingData> CODEC = Codec.STRING.listOf().xmap(
            ids -> {
                PlayerSubbendingData data = new PlayerSubbendingData();
                for (String id : ids) {
                    SubbendingType.byId(id).ifPresent(data.unlocked::add);
                }
                return data;
            },
            data -> data.unlocked.stream()
                    .map(SubbendingType::getId)
                    .collect(Collectors.toList())
    );

    private final Set<SubbendingType> unlocked = EnumSet.noneOf(SubbendingType.class);

    public boolean has(SubbendingType type) {
        return unlocked.contains(type);
    }

    /** @return true se realmente mudou algo (false se o jogador já tinha) */
    public boolean grant(SubbendingType type) {
        return unlocked.add(type);
    }

    /** @return true se realmente mudou algo (false se o jogador não tinha) */
    public boolean revoke(SubbendingType type) {
        return unlocked.remove(type);
    }

    public List<SubbendingType> getAll() {
        return List.copyOf(unlocked);
    }
}