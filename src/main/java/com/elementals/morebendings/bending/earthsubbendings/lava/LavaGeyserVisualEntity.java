package com.elementals.morebendings.bending.earthsubbendings.lava;

import com.elementals.morebendings.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Entidade puramente visual do jato de {@code lavaGeyser} (ver {@link
 * LavaGeyserAbility}) -- mesmo esquema (sem física/gravidade/colisão,
 * nunca leva dano, {@code seed} sincronizado, conta os próprios ticks e
 * se auto-descarta) de {@link MagmaSpikeVisualEntity}, só que mais alta e
 * fina -- lê como um JATO vertical disparando pra cima, não uma
 * estalagmite brotando devagar. O giro contínuo por segmento (ver {@link
 * LavaGeyserVisualEntityRenderer}) simula a pressão do jato torcendo no
 * ar, em vez de uma inclinação estática por instância.
 */
public class LavaGeyserVisualEntity extends Entity {

    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(LavaGeyserVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME_TICKS =
            SynchedEntityData.defineId(LavaGeyserVisualEntity.class, EntityDataSerializers.INT);

    public LavaGeyserVisualEntity(EntityType<LavaGeyserVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** @param groundPos posição do BLOCO de onde o jato brota -- a entidade nasce no topo dele, centralizada. */
    public LavaGeyserVisualEntity(Level level, BlockPos groundPos, int lifetimeTicks, int seed) {
        this(ModEntities.LAVA_GEYSER_VISUAL.get(), level);
        this.setPos(groundPos.getX() + 0.5, groundPos.getY() + 1.0, groundPos.getZ() + 0.5);
        this.entityData.set(SEED, seed);
        this.entityData.set(LIFETIME_TICKS, Math.max(1, lifetimeTicks));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0);
        builder.define(LIFETIME_TICKS, 18);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public int getLifetimeTicks() {
        return this.entityData.get(LIFETIME_TICKS);
    }

    /** Área de culling generosa -- o jato sobe bem mais alto que a hitbox nominal (0.5x0.5). */
    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 0.8, getY() - 0.1, getZ() - 0.8, getX() + 0.8, getY() + 2.6, getZ() + 0.8);
    }

    @Override
    public void tick() {
        super.baseTick();
        if (!this.level().isClientSide && this.tickCount >= getLifetimeTicks()) {
            this.discard(); // servidor decide quando some, ver javadoc da classe
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Seed")) {
            this.entityData.set(SEED, tag.getInt("Seed"));
        }
        if (tag.contains("LifetimeTicks")) {
            this.entityData.set(LIFETIME_TICKS, tag.getInt("LifetimeTicks"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Seed", getSeed());
        tag.putInt("LifetimeTicks", getLifetimeTicks());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Puramente decorativo -- nunca leva dano (o dano de verdade já foi aplicado na hora do impacto). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** Spawna um jato visual sincronizado no servidor e retorna a instância já adicionada ao nível. */
    public static LavaGeyserVisualEntity spawn(ServerLevel level, BlockPos groundPos, int lifetimeTicks) {
        int seed = level.random.nextInt(Integer.MAX_VALUE);
        LavaGeyserVisualEntity geyser = new LavaGeyserVisualEntity(level, groundPos, lifetimeTicks, seed);
        level.addFreshEntity(geyser);
        return geyser;
    }
}