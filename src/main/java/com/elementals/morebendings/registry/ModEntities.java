package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.bending.earthsubbendings.bone.BoneSpikeEntity;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardEntity;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassShardEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registro das entidades do addon. Segue o mesmo padrão que o mod base usa
 * pros próprios projéteis (ver NeoForgeRegistryHelper#registerEntity
 * decompilado: EntityType.Builder.of(...).noSummon().sized(w, h).build(name)),
 * só que direto, sem passar pela camada de abstração multi-loader deles —
 * a gente só roda em NeoForge mesmo.
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Constants.MOD_ID);

    public static final Supplier<EntityType<CrystalShardEntity>> CRYSTAL_SHARD =
            ENTITY_TYPES.register("crystal_shard",
                    () -> EntityType.Builder.<CrystalShardEntity>of(CrystalShardEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("crystal_shard"));

    public static final Supplier<EntityType<BoneSpikeEntity>> BONE_SPIKE =
            ENTITY_TYPES.register("bone_spike",
                    () -> EntityType.Builder.<BoneSpikeEntity>of(BoneSpikeEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("bone_spike"));

    public static final Supplier<EntityType<GlassShardEntity>> GLASS_SHARD =
            ENTITY_TYPES.register("glass_shard",
                    () -> EntityType.Builder.<GlassShardEntity>of(GlassShardEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("glass_shard"));

    private ModEntities() {
    }
}