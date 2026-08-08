package com.elementals.morebendings.bending.earthsubbendings.lava;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registro server-only de UUID -> {@link LavaSurfWaveVisualEntity} ativa
 * por jogador. Necessário porque {@link LavaSurfAbility} é uma instância
 * ÚNICA compartilhada por TODOS os lavabenders (ver {@code
 * LavaElement#addAbility}) -- não dá pra guardar a referência da onda
 * como campo de instância da ability, senão um segundo jogador surfando
 * ao mesmo tempo sobrescreveria a referência do primeiro. Mesmo motivo
 * pelo qual estados assim (agora removidos/renomeados: antigo {@code
 * LavaArmorState}) existiam nesse pacote.
 */
public final class LavaSurfState {

    private static final Map<UUID, LavaSurfWaveVisualEntity> active = new HashMap<>();

    public static void set(ServerPlayer player, LavaSurfWaveVisualEntity wave) {
        active.put(player.getUUID(), wave);
    }

    public static LavaSurfWaveVisualEntity get(ServerPlayer player) {
        return active.get(player.getUUID());
    }

    public static void clear(ServerPlayer player) {
        active.remove(player.getUUID());
    }

    private LavaSurfState() {
    }
}