package com.elementals.morebendings.bending.earthsubbendings.sand;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * "sandQuicksand" — terceira habilidade raiz da árvore de Sand (ver {@link
 * SandElement}). Diferente de {@code sandTornado} (canalizada, atinge
 * quem estiver por perto do caster) e de {@code sandBlast} (hitscan
 * pontual), esta é uma ARMADILHA DE ÁREA que fica plantada no chão
 * mirado -- uma cratera de areia movediça de verdade -- e continua ativa
 * sozinha por um tempo, sem depender do caster ficar agachado ou por
 * perto (ver {@link SandQuicksandState}).
 *
 * Só pode ser conjurada em cima de terreno "arenoso" ({@link #SANDY}) --
 * senão não tem película de areia solta o bastante pra virar areia
 * movediça, e a habilidade falha com feedback (mesmo esquema de {@code
 * MudSpikesAbility} quando não acha bloco "muddable" por perto).
 *
 * Instantânea do ponto de vista do bender (libera {@code currAbility} na
 * hora, igual {@code MudSpikesAbility}) -- quem continua vivo tick a tick
 * depois disso é o {@link SandQuicksandManager}, dirigido pelo listener de
 * {@code ServerTickEvent.Post} registrado em
 * {@code ElementalsMoreBendingsMod}.
 */
public class SandQuicksandAbility implements Ability {

    private static final double RANGE = 12.0;
    private static final float CHI_COST = 30.0f;

    /** Chão que tem "areia solta" o bastante pra virar areia movediça. */
    private static final Set<Block> SANDY = Set.of(Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL);

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (SandQuicksandManager.hasActivePit(caster)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        BlockPos aimed = BlockPos.containing(hit.getLocation());
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, aimed).below();

        if (!SANDY.contains(level.getBlockState(ground).getBlock())) {
            caster.displayClientMessage(
                    Component.literal("Não há areia suficiente ali para virar areia movediça."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 center = Vec3.atCenterOf(ground).add(0, 1, 0);
        SandQuicksandManager.startPit(level, caster, center);

        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                center.x, center.y, center.z, 30, 1.4, 0.15, 1.4, 0.02);
        level.playSound(null, ground, SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.9f, 0.7f);

        bender.setCurrAbility(null); // não canaliza -- quem continua é o SandQuicksandManager
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}