package com.elementals.morebendings.bending.airsubbendings.common;

import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Guarda, só em memória (não persiste entre logins -- mesmo esquema dos
 * outros mapas de cooldown/estado deste mod, ex.: {@code HeavyFogAbility
 * #lastUse}), qual especialização de Gas/Mist está ATIVA no momento pra
 * cada jogador.
 * <p>
 * Diferente de "comprada" ({@link GasElement#hasUpgrade}/{@link
 * MistElement#hasUpgrade}): as três especializações de cada árvore NÃO
 * são mais mutuamente exclusivas na COMPRA (o jogador pode comprar
 * Suffocate, Leak E Ignite ao mesmo tempo, por exemplo -- a árvore em si
 * já permitia isso, nada travava). Esta classe decide qual delas de fato
 * produz efeito quando a habilidade é usada, alternada pela tecla de
 * "Cycle Specialization" (ver {@code ModKeyMappings}/{@code
 * CycleSpecializationPacket}).
 * <p>
 * Se o jogador nunca apertou a tecla (ou relogou -- estado não persiste),
 * {@link #resolve} assume o primeiro caminho comprado na ordem fixa
 * abaixo, pra habilidade funcionar de primeira sem precisar ensinar o
 * jogador a trocar antes de sequer usar.
 */
public final class SpecializationCycle {

    private static final String[] GAS_ORDER = {
            GasElement.GAS_SUFFOCATE, GasElement.GAS_LEAK, GasElement.GAS_IGNITE
    };
    private static final String[] MIST_ORDER = {
            MistElement.MIST_CHOKE, MistElement.MIST_VEIL, MistElement.MIST_FREEZE
    };

    private static final Map<UUID, String> activeGas = new HashMap<>();
    private static final Map<UUID, String> activeMist = new HashMap<>();

    private SpecializationCycle() {
    }

    public static boolean isGasActive(ServerPlayer player, String upgradeName) {
        return upgradeName.equals(resolve(player, GAS_ORDER, activeGas, GasElement::hasUpgrade));
    }

    public static boolean isMistActive(ServerPlayer player, String upgradeName) {
        return upgradeName.equals(resolve(player, MIST_ORDER, activeMist, MistElement::hasUpgrade));
    }

    /** @return o novo caminho ativo, ou null se o jogador não comprou nenhuma especialização de Gas ainda. */
    public static String cycleGas(ServerPlayer player) {
        return cycle(player, GAS_ORDER, activeGas, GasElement::hasUpgrade);
    }

    /** @return o novo caminho ativo, ou null se o jogador não comprou nenhuma especialização de Mist ainda. */
    public static String cycleMist(ServerPlayer player) {
        return cycle(player, MIST_ORDER, activeMist, MistElement::hasUpgrade);
    }

    private static String resolve(ServerPlayer player, String[] order, Map<UUID, String> map, BiPredicate<ServerPlayer, String> owns) {
        UUID id = player.getUUID();
        String current = map.get(id);
        if (current != null && owns.test(player, current)) {
            return current;
        }
        for (String candidate : order) {
            if (owns.test(player, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String cycle(ServerPlayer player, String[] order, Map<UUID, String> map, BiPredicate<ServerPlayer, String> owns) {
        List<String> owned = new ArrayList<>();
        for (String candidate : order) {
            if (owns.test(player, candidate)) {
                owned.add(candidate);
            }
        }
        if (owned.isEmpty()) {
            map.remove(player.getUUID());
            return null;
        }

        String current = resolve(player, order, map, owns);
        int nextIndex = (owned.indexOf(current) + 1) % owned.size();
        String next = owned.get(nextIndex);
        map.put(player.getUUID(), next);
        return next;
    }
}