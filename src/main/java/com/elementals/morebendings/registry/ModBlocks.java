package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Blocos "fantasma" que só existem pra dar um {@code BlockState} de
 * verdade (com blockstate/model json de verdade, ver
 * assets/elementalsmorebendings/{blockstates,models/block}) pra usar em
 * {@code Display.BlockDisplay} -- NUNCA são colocados no mundo de verdade
 * (sem BlockItem, sem loot table).
 * <p>
 * O motivo de existir: {@code Blocks.WATER.defaultBlockState()} direto
 * NÃO renderiza certo num BlockDisplay. Fluidos (água/lava) não têm um
 * baked model de verdade -- eles são desenhados por um pipeline à parte
 * do cliente (o "liquid renderer" que roda na hora de montar a malha do
 * chunk), pipeline esse que só existe pra blocos de verdade colocados no
 * mundo, não pra entidades de Display. Passar o BlockState da água pro
 * {@code BlockModelShaper} (que é o que o BlockDisplay usa) cai no
 * modelo "missing" (o cubo preto/roxo), só que com a hitbox pequena da
 * água -- daí os "cubinhos" estranhos que aparecem em vez de água de
 * verdade.
 * <p>
 * A saída é ter nosso próprio bloco com um model json de verdade que
 * usa a MESMA sprite animada que a água real usa
 * ({@code minecraft:block/water_still}) -- como é a mesma sprite, ela já
 * vem com a animação de fluxo de graça (a animação é uma propriedade do
 * atlas de texturas, não do bloco). O tint (cor azul) é aplicado à parte
 * via {@code RegisterColorHandlersEvent.Block}, ver
 * {@code ClientClass#onRegisterBlockColors}.
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);

    /** Ver javadoc da classe. Usado só pelo anel de Água do Avatar State (AvatarStateManager). */
    public static final Supplier<Block> WATER_RING_DISPLAY = BLOCKS.register("water_ring_display",
            () -> new Block(BlockBehaviour.Properties.of()
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.EMPTY)
                    .mapColor(MapColor.WATER)
                    .noLootTable()));

    private ModBlocks() {
    }
}