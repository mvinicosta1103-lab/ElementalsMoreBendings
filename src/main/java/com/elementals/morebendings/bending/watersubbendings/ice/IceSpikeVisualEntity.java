package com.elementals.morebendings.bending.watersubbendings.ice;

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
 * Entidade puramente visual de UM espinho de gelo brotando do chão --
 * mesmo esquema exato de {@code CrystalSpikeVisualEntity}: sem física, sem
 * gravidade, sem colisão, só existência + timer pro {@link
 * IceSpikeVisualEntityRenderer} desenhar a farpa e se descartar sozinha
 * quando {@link IceSpikeManager} reverte o bloco embaixo dela.
 * <p>
 * {@code seed} é sincronizado pra todo mundo ver a MESMA farpa (mesma
 * altura, inclinação e giro) em vez de cada cliente sortear a sua.
 */
public class IceSpikeVisualEntity extends Entity {

    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(IceSpikeVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME_TICKS =
            SynchedEntityData.defineId(IceSpikeVisualEntity.class, EntityDataSerializers.INT);

    public IceSpikeVisualEntity(EntityType<IceSpikeVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** @param growPos posição do BLOCO em que o espinho está brotando -- a entidade nasce no topo dele, centralizada. */
    public IceSpikeVisualEntity(Level level, BlockPos growPos, int lifetimeTicks, int seed) {
        this(ModEntities.ICE_SPIKE_VISUAL.get(), level);
        this.setPos(growPos.getX() + 0.5, growPos.getY() + 1.0, growPos.getZ() + 0.5);
        this.entityData.set(SEED, seed);
        this.entityData.set(LIFETIME_TICKS, Math.max(1, lifetimeTicks));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0);
        builder.define(LIFETIME_TICKS, 30);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    public int getLifetimeTicks() {
        return this.entityData.get(LIFETIME_TICKS);
    }

    /** Área de culling generosa -- o espinho pode ficar bem mais alto que a hitbox nominal (0.5x0.5). */
    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 0.7, getY() - 0.1, getZ() - 0.7, getX() + 0.7, getY() + 1.7, getZ() + 0.7);
    }

    @Override
    public void tick() {
        super.baseTick();
        if (!this.level().isClientSide && this.tickCount >= getLifetimeTicks()) {
            this.discard();
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

    /** Puramente decorativo -- nunca leva dano (o dano de verdade da erupção já foi aplicado na hora do impacto). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /**
     * Spawna um espinho visual sincronizado no servidor e retorna a instância
     * já adicionada ao nível -- helper usado por {@link IceSpikeAbility}.
     */
    public static IceSpikeVisualEntity spawn(ServerLevel level, BlockPos growPos, int lifetimeTicks) {
        int seed = level.random.nextInt(Integer.MAX_VALUE);
        IceSpikeVisualEntity spike = new IceSpikeVisualEntity(level, growPos, lifetimeTicks, seed);
        level.addFreshEntity(spike);
        return spike;
    }
}