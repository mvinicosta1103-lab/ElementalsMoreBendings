package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    /**
     * Blocos "de terreno" que a lava pode substituir -- bem mais amplo que
     * a lista original de LavaPoolAbility. Cobre terra/pedra crua, TODAS
     * as variantes lisas/talhadas/tijolos de pedra e deepslate, arenito,
     * terracota (comuns em chão de vila/estrutura), blackstone e tuff.
     * Sem isso, qualquer área "trabalhada" (praças, caminhos de vila,
     * badlands) simplesmente não tinha nenhum bloco reconhecido e o fluxo
     * inteiro saía vazio, em silêncio.
     */
    static final Set<Block> MOLTENABLE = Set.of(
            // terra crua
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.MUD,
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.CLAY,
            // pedra crua e talhada
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE,
            Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.POLISHED_ANDESITE, Blocks.POLISHED_DIORITE, Blocks.POLISHED_GRANITE,
            Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
            Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS,
            Blocks.TUFF, Blocks.POLISHED_TUFF, Blocks.TUFF_BRICKS, Blocks.CALCITE,
            // deepslate
            Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE,
            Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.CHISELED_DEEPSLATE,
            Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_TILES,
            // arenito
            Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE,
            Blocks.RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE,
            // badlands / terracota
            Blocks.TERRACOTTA,
            Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA,
            Blocks.RED_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
            // blackstone / basalt
            Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS,
            Blocks.BASALT, Blocks.SMOOTH_BASALT
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
            // Falha silenciosa original -- agora avisa o jogador em vez de
            // simplesmente não fazer nada (parecia "quebrado" sem isso).
            player.sendSystemMessage(Component.literal("Chi insuficiente para usar Lava Flow.")
                    .withStyle(ChatFormatting.RED));
            bender.setCurrAbility(null);
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 flatDir = new Vec3(look.x, 0, look.z);
        if (flatDir.lengthSqr() < 1.0E-4) {
            // Olhando quase reto pra cima/baixo -- sem direção horizontal definida, cancela.
            player.sendSystemMessage(Component.literal("Olhe mais pra frente (não reto pra cima/baixo) para usar Lava Flow.")
                    .withStyle(ChatFormatting.RED));
            bender.setCurrAbility(null);
            return;
        }
        flatDir = flatDir.normalize();

        BlockPos origin = BlockPos.containing(player.position());

        LavaFlowManager.startFlow(level, origin, flatDir, player);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.7f);

        bender.setCurrAbility(null); // instantânea -- libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}