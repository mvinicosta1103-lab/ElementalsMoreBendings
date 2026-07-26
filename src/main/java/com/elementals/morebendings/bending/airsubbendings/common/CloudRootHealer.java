package com.elementals.morebendings.bending.airsubbendings.common;

import com.elementals.morebendings.bending.airsubbendings.gas.GasElement;
import com.elementals.morebendings.bending.airsubbendings.mist.MistElement;
import commonnetwork.api.Network;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;
import dev.saperate.elementals.data.StateDataSaverAndLoader;
import dev.saperate.elementals.elements.Upgrade;
import dev.saperate.elementals.network.packets.common.SyncUpgradeListPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * "gasCloud"/"mistCloud" são os ÚNICOS filhos diretos da raiz sintética
 * de {@link GasElement}/{@link MistElement} (ver {@code autoUnlockRoot}
 * em ambas as classes). Enquanto esse nó não estiver marcado como
 * comprado no mapa de upgrades do jogador, {@code PlayerData
 * #canBuyUpgrade} nunca "desce" pra dentro da árvore -- gasCloudSizeI,
 * gasVentI, gasSpecialization (e o mesmo pro lado Mist) ficam
 * permanentemente "não comprável", mesmo com level de sobra.
 * <p>
 * Antes disso só era corrigido manualmente, rodando de novo
 * {@code /morebending grant <player> gas} (o branch de reparo em
 * {@code MoreBendingCommand} já chama {@code autoUnlockRoot} pra quem
 * já tem o elemento). Só que isso depende de um operador lembrar de
 * rodar o comando toda vez que a árvore "trava" -- o que acontece pra
 * qualquer bender que tenha ganhado Gas/Mist antes desse hack existir,
 * ou cujo save tenha sido restaurado/migrado sem o nó raiz junto.
 * <p>
 * Esta classe remove essa dependência: a cada login, se o jogador já é
 * Gas ou Mist bender mas o nó raiz correspondente não está marcado como
 * comprado, a gente corrige e sincroniza na hora -- sem precisar de
 * intervenção manual nenhuma.
 */
public final class CloudRootHealer {

    private CloudRootHealer() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Bender bender = Bender.getBender(player);
        boolean changed = false;

        if (GasElement.isGasBender(bender) && !isRootUnlocked(bender, GasElement.get().root.children[0])) {
            GasElement.autoUnlockRoot(bender);
            changed = true;
        }
        if (MistElement.isMistBender(bender) && !isRootUnlocked(bender, MistElement.get().root.children[0])) {
            MistElement.autoUnlockRoot(bender);
            changed = true;
        }

        if (changed) {
            Network.getNetworkHandler().sendToClient(SyncUpgradeListPacket.createFromBender(bender), player);
            StateDataSaverAndLoader.getServerState(player.getServer()).setDirty();
        }
    }

    private static boolean isRootUnlocked(Bender bender, Upgrade rootChild) {
        PlayerData data = bender.getData();
        return data.upgrades.getOrDefault(rootChild, false);
    }
}