package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * "lavaFlow" — filho de {@link LavaElement#LAVA_JET} (ao lado de {@code
 * lavaSurf}, ver {@link LavaElement}).
 *
 * Diferente de {@link LavaPoolAbility} (poça parada, criada de uma vez só)
 * e de {@link LavaJetAbility} (dano instantâneo em linha, sem bloco
 * nenhum), lavaFlow cria uma "correnteza" de lava de verdade que nasce nos
 * pés do jogador e vai crescendo/se espalhando sozinha ao longo de vários
 * ticks na direção que ele estava olhando no momento do cast -- quanto
 * mais longe da origem, mais larga a faixa fica (efeito de leque/rio se
 * espalhando), até atingir {@link #MAX_STEPS} "fileiras".
 *
 * onCall só calcula a origem/direção e registra o fluxo no {@link
 * LavaFlowManager}; quem efetivamente vai colocando os blocos, fileira por
 * fileira, tick a tick, é o manager (independente do onTick do bender --
 * mesmo esquema de LavaPoolManager/MagmaSpikeManager). Por isso aqui
 * também é instantânea e libera {@code currAbility} de volta pra null no
 * fim do onCall, igual LavaPoolAbility/LavaJetAbility.
 */
public class LavaFlowAbility implements Ability {

    private static final float CHI_COST = 45.0f;

    /** Quantas fileiras a faixa cresce até parar de se espalhar -- fluxo grande, cobre uma área grande. */
    static final int MAX_STEPS = 40;
    /** A cada quantos ticks de servidor uma nova fileira nasce (2 ticks = 10 fileiras/s). */
    static final int STEP_INTERVAL_TICKS = 2;
    /** Depois de totalmente crescida, quanto tempo (ticks) até toda a faixa esfriar de uma vez pra basalto. */
    static final int COOL_AFTER_TICKS = 300; // 15s -- fluxo grande fica mais tempo antes de esfriar

    /** Quantas fileiras de largura no máximo (radius 6 = 13 blocos de largura). */
    static final int MAX_RADIUS = 6;
    /** A cada quantas fileiras de distância a faixa ganha +1 de raio de largura. */
    static final int STEPS_PER_WIDEN = 4;

    /** Blocos de terreno "naturais" que a lava pode substituir -- mesma lista de {@code LavaPoolAbility}. */
    static final Set<Block> MOLTENABLE = Set.of(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.MUD,
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.CLAY,
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE
    );

    /**
     * Cobertura de neve/gelo que costuma ficar POR CIMA do terreno de
     * verdade (biomas de neve/tundra/montanha) -- sem isso, o heightmap
     * acha a neve como "topo" e para ali, já que neve não está em
     * {@link #MOLTENABLE}, e o fluxo não derrete nada. Esses blocos
     * evaporam/derretem (viram ar) enquanto o fluxo desce procurando o
     * primeiro bloco de terreno de verdade por baixo.
     */
    static final Set<Block> MELTABLE_OVERLAY = Set.of(
            Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW,
            Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.FROSTED_ICE
    );

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 flatDir = new Vec3(look.x, 0, look.z);
        if (flatDir.lengthSqr() < 1.0E-4) {
            // Olhando quase reto pra cima/baixo -- sem direção horizontal definida, cancela.
            bender.setCurrAbility(null);
            return;
        }
        flatDir = flatDir.normalize();

        BlockPos origin = BlockPos.containing(player.position());

        LavaFlowManager.startFlow(level, origin, flatDir);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.7f);

        bender.setCurrAbility(null); // instantânea -- libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}