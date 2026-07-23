package com.example.elementalmorebendings.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class AddonTags {
    private AddonTags() {}

    public static final TagKey<Block> ORE_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("elementalmorebendings", "ore_blocks")
    );
}