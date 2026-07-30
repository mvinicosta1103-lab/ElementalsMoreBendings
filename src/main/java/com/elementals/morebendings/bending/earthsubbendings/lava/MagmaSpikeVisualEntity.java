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
 * Entidade puramente visual de UM espinho de magma. Antes, {@link
 * MagmaSpikeAbility}/{@link VolcanicEruptionAbility} só trocavam a
 * textura do chão pra {@code magma_block} -- lia como "piso diferente",
 * não como espinho de verdade brotando do chão. Essa entidade dá o
 * "corpo" pro {@link MagmaSpikeVisualEntityRenderer} desenhar um espeto
 * pontudo de verdade em cima de cada posição, igual {@code MistFogEntity}
 * dá corpo pra névoa de Heavy Fog (mesmo padrão: sem física, sem
 * gravidade, sem colisão -- só existência + timer pro renderer ler).
 * <p>
 * {@code seed} é sincronizado (não recalculado independentemente em cada
 * cliente) pra todo mundo ver o MESMO espinho -- mesma altura, mesma
 * inclinação, mesmo giro -- em vez de cada jogador tirando um número
 * aleatório diferente e ver formas diferentes pra a mesma entidade.
 * <p>
 * Ciclo de vida: nasce com {@code lifetimeTicks} = o mesmo
 * {@code RETRACT_AFTER_TICKS}/{@code SPIKE_RETRACT_AFTER_TICKS} que a
 * ability correspondente já usa pra reverter o bloco (ver {@link
 * MagmaSpikeManager}/{@link VolcanicEruptionManager}) -- assim o modelo
 * desaparece exatamente quando o chão volta ao normal, sem precisar de
 * uma segunda fonte de verdade sobre duração. Conta ela mesma os ticks e
 * se descarta sozinha (só no servidor; o discard replica pro cliente
 * pelo sistema de tracking padrão).
 */
public class MagmaSpikeVisualEntity extends Entity {

    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(MagmaSpikeVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME_TICKS =
            SynchedEntityData.defineId(MagmaSpikeVisualEntity.class, EntityDataSerializers.INT);

    public MagmaSpikeVisualEntity(EntityType<MagmaSpikeVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** @param growPos posição do BLOCO em que o espinho está brotando -- a entidade nasce no topo dele, centralizada. */
    public MagmaSpikeVisualEntity(Level level, BlockPos growPos, int lifetimeTicks, int seed) {
        this(ModEntities.MAGMA_SPIKE_VISUAL.get(), level);
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
        return new AABB(getX() - 0.7, getY() - 0.1, getZ() - 0.7, getX() + 0.7, getY() + 1.6, getZ() + 0.7);
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

    /** Puramente decorativo -- nunca leva dano (o dano de verdade da erupção já foi aplicado na hora do impacto). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /**
     * Spawna um espinho visual sincronizado no servidor e retorna a instância
     * já adicionada ao nível -- helper comum usado tanto por {@link
     * MagmaSpikeAbility} quanto por {@link VolcanicEruptionAbility}, pra não
     * duplicar o "new + setPos + addFreshEntity" nos dois lugares.
     */
    public static MagmaSpikeVisualEntity spawn(ServerLevel level, BlockPos growPos, int lifetimeTicks) {
        int seed = level.random.nextInt(Integer.MAX_VALUE);
        MagmaSpikeVisualEntity spike = new MagmaSpikeVisualEntity(level, growPos, lifetimeTicks, seed);
        level.addFreshEntity(spike);
        return spike;
    }
}