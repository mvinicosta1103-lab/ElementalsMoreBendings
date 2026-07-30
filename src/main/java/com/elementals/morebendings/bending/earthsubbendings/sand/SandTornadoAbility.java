package com.elementals.morebendings.bending.earthsubbendings.sand;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "sandTornado" — primeira habilidade raiz da árvore de Sand (ver
 * {@link SandElement}; as outras três são {@code sandBlast}, {@code
 * sandQuicksand} e {@code sandWave}).
 *
 * Conjura uma coluna giratória de areia no ponto mirado no chão: suga os
 * blocos de {@code SAND}/{@code RED_SAND} próximos (ver {@link
 * SandTornadoState#begin}) e prende quem estiver perto dela num redemoinho
 * -- cegando, empurrando pra dentro/pra cima e causando dano leve enquanto
 * dura (ver {@link SandTornadoState#tick}).
 *
 * Canalizada, no mesmo esquema da {@code MudTrapAbility} deste addon:
 *  - {@link #activatesOnPress()} é {@code true} -- dispara IMEDIATAMENTE ao
 *    apertar a tecla, sem precisar soltar (senão jogadores que seguram a
 *    tecla esperando o efeito nunca disparariam o {@code onCall}, que por
 *    padrão só roda ao SOLTAR).
 *  - Só continua ativa enquanto o jogador ficar agachado -- por isso
 *    exigimos agachar já no instante do cast (ver checagem em {@link
 *    #onCall}), senão o tornado nasceria e morreria no mesmo tick.
 *  - Enquanto ativa, é o {@link SandTornadoManager} (dirigido pelo listener
 *    de {@code ServerTickEvent.Post} registrado em
 *    {@code ElementalsMoreBendingsMod}) que cuida do giro/sucção/dano a
 *    cada tick -- {@link #onTick} aqui só verifica se deve soltar.
 */
public class SandTornadoAbility implements Ability {

    private static final double RANGE = 12.0;

    @Override
    public boolean activatesOnPress() {
        return true;
    }

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (SandTornadoManager.hasActiveTornado(caster)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!player.isShiftKeyDown()) {
            // Mesma ressalva da MudTrapAbility: sem isso o tornado nasceria e
            // morreria no mesmo tick (onTick veria isShiftKeyDown()==false e
            // cancelaria tudo antes do jogador perceber qualquer coisa).
            caster.displayClientMessage(
                    Component.literal("Segure Shift para manter o tornado de areia."), true);
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        BlockPos aimed = BlockPos.containing(hit.getLocation());
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, aimed).below();

        Vec3 origin = Vec3.atCenterOf(ground).add(0, 1, 0);

        SandTornadoManager.startTornado(level, caster, ground, origin);
        // Sem setCurrAbility(null) aqui de propósito: fica canalizada -- ver onTick.
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !SandTornadoManager.hasActiveTornado(caster)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!player.isShiftKeyDown()) {
            SandTornadoManager.release(caster);
            bender.setCurrAbility(null);
        }
        // Enquanto agachado com o tornado ativo, o SandTornadoManager (via
        // tick do servidor) já cuida sozinho do giro/sucção/dano.
    }

    @Override
    public void onRemove(Bender bender) {
        if (bender.player instanceof ServerPlayer caster) {
            SandTornadoManager.release(caster);
        }
    }
}