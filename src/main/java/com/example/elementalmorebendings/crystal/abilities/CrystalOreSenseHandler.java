package com.example.elementalmorebendings.crystal.abilities;

import com.example.elementalmorebendings.common.AddonTags;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.effects.ElementalsStatusEffects;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

/**
 * "Minérios brilham pro jogador quando ele usa Seismic Sense" — feature
 * pedida junto com a expansão de Crystal pra dobrar/controlar minérios (ver
 * {@link AbilitySupport#isOreBendable}).
 * <p>
 * Não depende de nenhum mixin/shader novo: o Seismic Sense do jar base já é
 * só um {@code MobEffect} (ver {@code SeismicSenseStatusEffect} — cega o
 * jogador e dá visão noturna em loop, e o "sonar" visual em si é resolvido
 * por um post-processing shader do próprio Elementals). Aqui a gente só
 * ouve o tick do jogador; enquanto ele tiver Crystal desbloqueado E o
 * efeito Seismic Sense ativo, cada bloco de minério (mesma tag usada pra
 * liberar a dobra, {@link AddonTags#ORE_BLOCKS}) num raio ao redor dele
 * recebe partículas de brilho — mandadas com o overload de
 * {@code ServerLevel#sendParticles} que envia só pro ServerPlayer
 * informado, então NINGUÉM MAIS vê esse brilho, só quem tá com Seismic
 * Sense ligado (igual um radar de minério pessoal).
 */
public final class CrystalOreSenseHandler {
    private CrystalOreSenseHandler() {}

    /** Cor dourada/brilhante pras partículas de "minério revelado". */
    private static final DustParticleOptions ORE_GLOW =
            new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.35f), 1.5f);

    /** Raio (em blocos) em volta do jogador varrido em busca de minério. */
    private static final int RADIUS = 10;

    /** Intervalo entre varreduras — não roda todo tick pra não pesar no servidor. */
    private static final int INTERVAL_TICKS = 10;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player rawPlayer = event.getEntity();
        if (!(rawPlayer instanceof ServerPlayer player)) {
            return; // só server-side; do lado cliente isso é puramente visual/replicado
        }
        if (player.level().getGameTime() % INTERVAL_TICKS != 0) {
            return;
        }

        Element crystal = CrystalElement.get();
        if (crystal == null) {
            return;
        }

        Bender bender = Bender.getBender(player);
        if (bender == null || !bender.hasElement(crystal)) {
            return;
        }

        if (!SapsUtils.safeHasStatusEffect(ElementalsStatusEffects.SEISMIC_SENSE.get(), player)) {
            return;
        }

        ServerLevel world = (ServerLevel) player.level();
        BlockPos center = player.blockPosition();
        BlockPos from = center.offset(-RADIUS, -RADIUS, -RADIUS);
        BlockPos to = center.offset(RADIUS, RADIUS, RADIUS);
        double radiusSqr = (double) RADIUS * RADIUS;

        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (pos.distSqr(center) > radiusSqr) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!state.is(AddonTags.ORE_BLOCKS)) {
                continue;
            }
            world.sendParticles(player, ORE_GLOW, false,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.15, 0.15, 0.15, 0.0);
        }
    }
}