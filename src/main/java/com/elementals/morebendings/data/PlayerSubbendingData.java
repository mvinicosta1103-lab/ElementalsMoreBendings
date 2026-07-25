package com.elementals.morebendings.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Guarda, por jogador:
 *  - quais sub-bendings (Gas, Flying, Plant, Mud, Crystal) ele já desbloqueou;
 *  - pra cada sub-bending, quantos "pontos de habilidade" ele tem pra gastar
 *    na skill tree dela e quais nós da árvore (ver {@link
 *    com.elementals.morebendings.bending.airsubbendings.gas.GasSkillTree})
 *    ele já comprou.
 *
 * Uma instância disso fica anexada a cada Player via Data Attachment (ver
 * {@link com.elementals.morebendings.registry.ModAttachments}), então
 * persiste entre logins/reinicializações do servidor.
 *
 * Por enquanto só o Gas realmente usa pontos/upgrades — os outros continuam
 * sendo só "tem ou não tem" (unlocked). Dá pra estender esse mesmo mapa pras
 * outras sub-bendings quando elas ganharem árvore própria também.
 */
public class PlayerSubbendingData {

    public static final Codec<PlayerSubbendingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("unlocked").forGetter(data -> data.unlocked.stream()
                    .map(SubbendingType::getId)
                    .collect(Collectors.toList())),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("points").forGetter(data -> {
                Map<String, Integer> out = new HashMap<>();
                data.points.forEach((type, value) -> out.put(type.getId(), value));
                return out;
            }),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).fieldOf("upgrades").forGetter(data -> {
                Map<String, List<String>> out = new HashMap<>();
                data.upgrades.forEach((type, nodes) -> out.put(type.getId(), List.copyOf(nodes)));
                return out;
            })
    ).apply(instance, PlayerSubbendingData::fromSaved));

    private final Set<SubbendingType> unlocked = EnumSet.noneOf(SubbendingType.class);
    private final Map<SubbendingType, Integer> points = new HashMap<>();
    private final Map<SubbendingType, Set<String>> upgrades = new HashMap<>();

    private static PlayerSubbendingData fromSaved(List<String> unlockedIds, Map<String, Integer> points,
                                                  Map<String, List<String>> upgrades) {
        PlayerSubbendingData data = new PlayerSubbendingData();
        for (String id : unlockedIds) {
            SubbendingType.byId(id).ifPresent(data.unlocked::add);
        }
        points.forEach((id, value) -> SubbendingType.byId(id).ifPresent(type -> data.points.put(type, value)));
        upgrades.forEach((id, nodes) -> SubbendingType.byId(id)
                .ifPresent(type -> data.upgrades.put(type, new HashSet<>(nodes))));
        return data;
    }

    public boolean has(SubbendingType type) {
        return unlocked.contains(type);
    }

    /** @return true se realmente mudou algo (false se o jogador já tinha) */
    public boolean grant(SubbendingType type) {
        return unlocked.add(type);
    }

    /** @return true se realmente mudou algo (false se o jogador não tinha) */
    public boolean revoke(SubbendingType type) {
        boolean changed = unlocked.remove(type);
        if (changed) {
            // perder a sub-bending também zera o progresso da árvore dela.
            points.remove(type);
            upgrades.remove(type);
        }
        return changed;
    }

    public List<SubbendingType> getAll() {
        return List.copyOf(unlocked);
    }

    // ---- progresso da skill tree (pontos + nós comprados) ----

    public int getPoints(SubbendingType type) {
        return points.getOrDefault(type, 0);
    }

    public void addPoints(SubbendingType type, int amount) {
        points.merge(type, amount, Integer::sum);
    }

    /** @return false se não tinha pontos suficientes (não desconta nada nesse caso) */
    public boolean spendPoints(SubbendingType type, int amount) {
        if (getPoints(type) < amount) {
            return false;
        }
        points.merge(type, -amount, Integer::sum);
        return true;
    }

    public boolean hasUpgrade(SubbendingType type, String upgradeName) {
        return upgrades.getOrDefault(type, Set.of()).contains(upgradeName);
    }

    public void unlockUpgrade(SubbendingType type, String upgradeName) {
        upgrades.computeIfAbsent(type, t -> new HashSet<>()).add(upgradeName);
    }

    public Set<String> getUnlockedUpgrades(SubbendingType type) {
        return Set.copyOf(upgrades.getOrDefault(type, Set.of()));
    }
}