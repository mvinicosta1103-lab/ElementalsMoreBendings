package com.elementals.morebendings.bending.common;

import dev.saperate.elementals.data.Bender;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * === CAUSA RAIZ DO BUG "lavaFlow não faz nada ao apertar a tecla" ===
 * decompilando dev.saperate.elementals.data.{Bender,PlayerData} e
 * dev.saperate.elementals.Elementals:
 * <p>
 * {@code PlayerData#boundAbilities} é um {@code Ability[12]} que NÃO é
 * salvo em NBT -- ele só existe em memória, e só é recalculado dentro de
 * {@link Bender#bindDefaultAbilities()}, que por sua vez só é chamado a
 * partir de {@code Bender#setElement(...)} (troca de elemento ativo) ou
 * de {@code Bender#addElement(...)} (só no caso especial de vir de
 * NoneElement). {@code Elementals#onPlayerJoin} só chama
 * {@code syncElements()} -- NUNCA {@code bindDefaultAbilities()}.
 * <p>
 * Consequência: se um jogador já tinha Lava como elemento ativo desde
 * antes de {@code LavaFlowAbility} existir, a última vez que
 * {@code boundAbilities} foi montado, o slot 8 (lavaFlow) nem existia
 * ainda em {@code LavaElement#bindableAbilities}. Como esse array vive
 * só em memória do lado do servidor e nada volta a recalculá-lo depois
 * disso, o slot 8 fica travado em {@code null} pra sempre pra esse
 * jogador -- mesmo depois de recompilar o mod com a ability nova --, até
 * ele trocar de elemento ativo e voltar pra Lava manualmente (o que força
 * um novo {@code bindDefaultAbilities()}).
 * <p>
 * Mesma classe de bug que {@link com.elementals.morebendings.bending.airsubbendings.common.CloudRootHealer}
 * já corrige pro caso de nó raiz não marcado como comprado -- só que aqui
 * o estado que "gruda" desatualizado é o array de binds, não o mapa de
 * upgrades comprados. Recalcular {@code boundAbilities} a cada login é
 * sempre seguro: não mexe em upgrades/compras, só sincroniza os slots de
 * tecla com a lista atual de {@code bindableAbilities} do elemento ativo
 * do jogador (a mesma coisa que já aconteceria se ele trocasse de
 * elemento e voltasse).
 */
public final class AbilityBindingHealer {

    private AbilityBindingHealer() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Bender bender = Bender.getBender(player);
        // Reconstrói os 12 slots de tecla a partir da lista ATUAL de
        // bindableAbilities do elemento ativo -- corrige qualquer slot
        // novo (ex: lavaFlow) que tenha ficado null por o jogador não ter
        // trocado de elemento desde que a ability foi adicionada.
        bender.bindDefaultAbilities();
    }
}