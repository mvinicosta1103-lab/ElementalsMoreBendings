package com.elementals.morebendings.bending.avatarstate;

import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.air.AirElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.elements.fire.FireElement;
import dev.saperate.elementals.elements.water.WaterElement;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda, só em memória (mesmo esquema de {@code SpecializationCycle}),
 * qual dos 4 elementos-base está "selecionado" no momento pra cada
 * jogador -- é esse elemento que {@link AvatarBendingGrantAbility} concede
 * e {@link AvatarBendingRemoveAbility} remove (ver {@code
 * CycleAvatarBendingPacket}, tecla dedicada). Não precisa estar no Avatar
 * State pra ciclar (só pra de fato conceder/remover, ver as abilities) --
 * ciclar é só troca de estado local, sem custo.
 * <p>
 * Não persiste entre logins nem é sincronizado ao cliente -- o feedback
 * de "qual elemento está selecionado agora" é só uma mensagem na action
 * bar (ver os handlers dos pacotes), igual o resto do mod faz pra estado
 * efêmero desse tipo.
 */
public final class AvatarBendingSelection {

    /** Ordem fixa de ciclagem -- sempre começa em AIR se o jogador nunca apertou a tecla. */
    private static final Element[] ORDER = {
            AirElement.get(), WaterElement.get(), EarthElement.get(), FireElement.get()
    };

    private static final Map<UUID, Element> SELECTED = new HashMap<>();

    private AvatarBendingSelection() {
    }

    /** @return o elemento atualmente selecionado (AIR por padrão, se nunca ciclou). */
    public static Element current(ServerPlayer player) {
        return SELECTED.getOrDefault(player.getUUID(), ORDER[0]);
    }

    /** @return o novo elemento selecionado, após avançar um passo na ordem fixa. */
    public static Element cycle(ServerPlayer player) {
        Element current = current(player);
        int nextIndex = (indexOf(current) + 1) % ORDER.length;
        Element next = ORDER[nextIndex];
        SELECTED.put(player.getUUID(), next);
        return next;
    }

    private static int indexOf(Element element) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == element) {
                return i;
            }
        }
        return 0;
    }

    /** Nome em PT-BR pra mensagens -- {@code Element#getName()} só devolve o id interno (ex.: "fire"). */
    public static String displayName(Element element) {
        if (element == AirElement.get()) return "Ar";
        if (element == WaterElement.get()) return "Água";
        if (element == EarthElement.get()) return "Terra";
        if (element == FireElement.get()) return "Fogo";
        return element.getName();
    }

    public static void onPlayerLoggedOut(UUID playerId) {
        SELECTED.remove(playerId);
    }
}