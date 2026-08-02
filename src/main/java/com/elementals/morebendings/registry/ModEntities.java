package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.bending.earthsubbendings.bone.BoneSpikeEntity;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardEntity;
import com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalSpikeVisualEntity;
import com.elementals.morebendings.bending.earthsubbendings.glass.GlassShardEntity;
import com.elementals.morebendings.bending.earthsubbendings.lava.LavaShurikenEntity;
import com.elementals.morebendings.bending.earthsubbendings.lava.MagmaSpikeVisualEntity;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudBallEntity;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudSpikeVisualEntity;
import com.elementals.morebendings.bending.earthsubbendings.mud.MudSurgeChunkEntity;
import com.elementals.morebendings.bending.earthsubbendings.sand.SandTornadoVisualEntity;
import com.elementals.morebendings.bending.airsubbendings.mist.MistFogEntity;
import com.elementals.morebendings.bending.firesubbendings.combustion.CombustionBoltEntity;
import com.elementals.morebendings.bending.watersubbendings.plant.PlantThornVolleyEntity;
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

    public static final Supplier<EntityType<CombustionBoltEntity>> COMBUSTION_BOLT =
            ENTITY_TYPES.register("combustion_bolt",
                    () -> EntityType.Builder.<CombustionBoltEntity>of(CombustionBoltEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.35f, 0.35f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("combustion_bolt"));

    /**
     * A farpa controlável de "lavaShuriken" (ver {@link LavaShurikenEntity}
     * / {@code LavaShurikenAbility}). Hitbox um pouco maior que os
     * estilhaços simples (crystal/glass/bone) porque ela costuma ficar
     * "flutuando" perto de entidades por vários segundos sob controle, em
     * vez de ser um tiro instantâneo -- {@code updateInterval(1)} continua
     * baixo pra manter a posição sincronizada suave enquanto o jogador guia
     * ela pelo ar.
     */
    public static final Supplier<EntityType<LavaShurikenEntity>> LAVA_SHURIKEN =
            ENTITY_TYPES.register("lava_shuriken",
                    () -> EntityType.Builder.<LavaShurikenEntity>of(LavaShurikenEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.3f, 0.3f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("lava_shuriken"));

    /**
     * Entidade puramente visual da névoa de Heavy Fog (ver {@code
     * MistFogEntity}/{@code MistCloudState}) -- não voa, não colide, só
     * fica parada servindo de "corpo" pro {@code MistFogEntityRenderer}.
     * Tamanho de hitbox aqui é só nominal (a área de culling de verdade é
     * recalculada em {@code MistFogEntity#getBoundingBoxForCulling} com
     * base no raio de verdade da névoa, que varia com os upgrades).
     */
    public static final Supplier<EntityType<MistFogEntity>> MIST_FOG =
            ENTITY_TYPES.register("mist_fog",
                    () -> EntityType.Builder.<MistFogEntity>of(MistFogEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(1.0f, 2.0f)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build("mist_fog"));

    /**
     * Espinho de magma puramente visual (ver {@link MagmaSpikeVisualEntity}) --
     * um por bloco erguido em {@code magmaSpike}/{@code volcanicEruption}.
     * Mesmo esquema de {@link #MIST_FOG}: sem física, sem colisão,
     * {@code updateInterval} alto porque não se move (só nasce parada e
     * conta os próprios ticks pra sumir -- não precisa reconciliar posição
     * com o cliente com frequência).
     */
    public static final Supplier<EntityType<MagmaSpikeVisualEntity>> MAGMA_SPIKE_VISUAL =
            ENTITY_TYPES.register("magma_spike_visual",
                    () -> EntityType.Builder.<MagmaSpikeVisualEntity>of(MagmaSpikeVisualEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build("magma_spike_visual"));

    /**
     * Espinho de cristal puramente visual (ver {@link CrystalSpikeVisualEntity}) --
     * um por bloco erguido em {@code crystalSpike}. Mesmo esquema de
     * {@link #MAGMA_SPIKE_VISUAL}: sem física, sem colisão,
     * {@code updateInterval} alto porque não se move (só nasce parada e
     * conta os próprios ticks pra sumir -- não precisa reconciliar posição
     * com o cliente com frequência).
     */
    public static final Supplier<EntityType<CrystalSpikeVisualEntity>> CRYSTAL_SPIKE_VISUAL =
            ENTITY_TYPES.register("crystal_spike_visual",
                    () -> EntityType.Builder.<CrystalSpikeVisualEntity>of(CrystalSpikeVisualEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build("crystal_spike_visual"));

    /**
     * Funil giratório puramente visual de "sandTornado" (ver
     * {@link SandTornadoVisualEntity}/{@code SandTornadoState}) -- mesmo
     * esquema de {@link #MAGMA_SPIKE_VISUAL}/{@link #MUD_SPIKE_VISUAL}: sem
     * física, sem colisão, {@code updateInterval} alto porque a entidade não
     * se move de verdade (só existe parada na base, contando com um
     * {@code seed} sincronizado -- quem gira é o renderer no cliente, não a
     * posição da entidade). Hitbox nominal pequena; a área de culling real é
     * bem maior, definida em {@code SandTornadoVisualEntity#getBoundingBoxForCulling}.
     */
    public static final Supplier<EntityType<SandTornadoVisualEntity>> SAND_TORNADO_VISUAL =
            ENTITY_TYPES.register("sand_tornado_visual",
                    () -> EntityType.Builder.<SandTornadoVisualEntity>of(SandTornadoVisualEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build("sand_tornado_visual"));

    /**
     * Projétil de "mudBall" (ver {@link MudBallEntity}) -- hitbox pequena
     * igual aos outros estilhaços simples (crystal/glass/bone), já que é
     * um tiro de contato/impacto, não algo que fica flutuando por aí.
     */
    public static final Supplier<EntityType<MudBallEntity>> MUD_BALL =
            ENTITY_TYPES.register("mud_ball",
                    () -> EntityType.Builder.<MudBallEntity>of(MudBallEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.3f, 0.3f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("mud_ball"));

    /**
     * Entidade puramente visual de UM cluster de farpas de "mudSpikes" (ver
     * {@link MudSpikeVisualEntity}) -- mesmo esquema de {@link
     * #MAGMA_SPIKE_VISUAL}: sem física, sem colisão, {@code updateInterval}
     * alto porque não se move.
     */
    public static final Supplier<EntityType<MudSpikeVisualEntity>> MUD_SPIKE_VISUAL =
            ENTITY_TYPES.register("mud_spike_visual",
                    () -> EntityType.Builder.<MudSpikeVisualEntity>of(MudSpikeVisualEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build("mud_spike_visual"));

    /**
     * Pedaço de lama endurecida de "mudSurge" (ver {@link MudSurgeChunkEntity}
     * / {@code MudSurgeAbility}) -- vários spawnados lado a lado por cast,
     * formando a frente da onda. Sem gravidade e {@code updateInterval} baixo
     * (se move rápido, rente ao chão, e precisa continuar sincronizado
     * enquanto atravessa vários alvos na fileira).
     */
    public static final Supplier<EntityType<MudSurgeChunkEntity>> MUD_SURGE_CHUNK =
            ENTITY_TYPES.register("mud_surge_chunk",
                    () -> EntityType.Builder.<MudSurgeChunkEntity>of(MudSurgeChunkEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.4f, 0.35f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("mud_surge_chunk"));

    public static final Supplier<EntityType<PlantThornVolleyEntity>> PLANT_THORN =
            ENTITY_TYPES.register("plant_thorn",
                    () -> EntityType.Builder.<PlantThornVolleyEntity>of(PlantThornVolleyEntity::new, MobCategory.MISC)
                            .noSummon()
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("plant_thorn"));

    private ModEntities() {
    }
}