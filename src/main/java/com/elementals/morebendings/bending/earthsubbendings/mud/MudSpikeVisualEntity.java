package com.elementals.morebendings.bending.earthsubbendings.mud;

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
 * Entidade puramente visual de UM cluster de farpas de lama de {@code
 * mudSpikes} (ver {@link MudSpikesAbility}). Mesmo padrão de {@code
 * MagmaSpikeVisualEntity}: sem física, sem gravidade, sem colisão -- só
 * existência + timer pro {@link MudSpikeVisualEntityRenderer} ler e
 * desenhar o cluster de farpas em cima.
 * <p>
 * {@code seed} é sincronizado (não recalculado independentemente em cada
 * cliente) pra todo mundo ver o MESMO cluster -- mesma altura, mesma
 * inclinação, mesmo giro de cada farpa -- em vez de cada jogador tirando um
 * número aleatório diferente e ver formas diferentes pra a mesma entidade.
 * <p>
 * Ciclo de vida: nasce com {@code lifetimeTicks} = o mesmo {@link
 * MudSpikesAbility#RETRACT_AFTER_TICKS} que a ability já usa pra reverter o
 * bloco (ver {@link MudSpikeManager}) -- assim o modelo desaparece
 * exatamente quando o chão volta ao normal, sem precisar de uma segunda
 * fonte de verdade sobre duração. Conta ela mesma os ticks e se descarta
 * sozinha (só no servidor; o discard replica pro cliente pelo sistema de
 * tracking padrão).
 */
public class MudSpikeVisualEntity extends Entity {

    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(MudSpikeVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME_TICKS =
            SynchedEntityData.defineId(MudSpikeVisualEntity.class, EntityDataSerializers.INT);

    public MudSpikeVisualEntity(EntityType<MudSpikeVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** @param growPos posição do BLOCO em que o cluster está brotando -- a entidade nasce no topo dele, centralizada. */
    public MudSpikeVisualEntity(Level level, BlockPos growPos, int lifetimeTicks, int seed) {
        this(ModEntities.MUD_SPIKE_VISUAL.get(), level);
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

    /** Área de culling generosa -- o cluster pode ficar mais largo/alto que a hitbox nominal (0.5x0.5). */
    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 0.7, getY() - 0.1, getZ() - 0.7, getX() + 0.7, getY() + 1.2, getZ() + 0.7);
    }

    @Override
    public void tick() {
        super.baseTick();
        if (!this.level().isClientSide && this.tickCount >= getLifetimeTicks()) {
            this.discard(); // servidor decide quando some; ver javadoc da classe
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

    /** Puramente decorativo -- nunca leva dano (o dano de verdade de mudSpikes já foi aplicado na hora do impacto). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /**
     * Spawna um cluster visual sincronizado no servidor e retorna a instância
     * já adicionada ao nível -- helper usado por {@link MudSpikesAbility}, pra
     * não duplicar o "new + setPos + addFreshEntity" a cada bloco.
     */
    public static MudSpikeVisualEntity spawn(ServerLevel level, BlockPos growPos, int lifetimeTicks) {
        int seed = level.random.nextInt(Integer.MAX_VALUE);
        MudSpikeVisualEntity spike = new MudSpikeVisualEntity(level, growPos, lifetimeTicks, seed);
        level.addFreshEntity(spike);
        return spike;
    }
}