package com.elementals.morebendings.bending.earthsubbendings.lava;

import com.elementals.morebendings.registry.ModEntities;
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
import net.minecraft.world.phys.Vec3;

/**
 * Entidade puramente visual da onda de lava de {@code lavaSurf} (ver
 * {@link LavaSurfAbility}) -- o "corpo" pro {@link
 * LavaSurfWaveVisualEntityRenderer} desenhar debaixo/atrás do jogador
 * enquanto ele corre em cima dela. Mesmo esquema sem-física/sem-colisão/
 * nunca-leva-dano de {@link MagmaSpikeVisualEntity}, mas REPOSICIONADA
 * todo tick pela própria ability (via {@link #followPlayer}, chamado em
 * {@link LavaSurfAbility#onTick}) em vez de nascer parada -- mesmo
 * princípio de {@code PlantVineGraspVisualEntity} pro cipó (ver seu
 * javadoc), só que aqui é a posição inteira que se move, não só o
 * comprimento.
 * <p>
 * Não tem timer de vida próprio: não conta ticks nem se auto-descarta --
 * dura exatamente enquanto {@link LavaSurfState} tiver uma entrada pra
 * esse jogador, e é {@link Entity#discard()}ada explicitamente por
 * {@link LavaSurfAbility#onRemove} assim que ele para de correr.
 */
public class LavaSurfWaveVisualEntity extends Entity {

    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(LavaSurfWaveVisualEntity.class, EntityDataSerializers.INT);

    public LavaSurfWaveVisualEntity(EntityType<LavaSurfWaveVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public LavaSurfWaveVisualEntity(Level level, Vec3 pos, float yaw, int seed) {
        this(ModEntities.LAVA_SURF_WAVE.get(), level);
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.entityData.set(SEED, seed);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    /**
     * Reposiciona a onda pros pés do jogador, orientada na direção em que
     * ele está indo. Chamado todo tick por {@link LavaSurfAbility#onTick}
     * enquanto a surfada durar -- {@code updateInterval} baixo (ver {@code
     * ModEntities#LAVA_SURF_WAVE}) garante que isso chegue ao cliente com
     * frequência suficiente pra parecer uma onda de verdade acompanhando o
     * jogador, não um efeito estático.
     */
    public void followPlayer(Vec3 feetPos, float yaw) {
        this.setPos(feetPos.x, feetPos.y, feetPos.z);
        // yRotO sincronizado ANTES de mudar o yaw -- mesmo motivo do comentário
        // no construtor de MudSurgeChunkEntity: sem isso, o renderer (que
        // interpola entre yRotO e getYRot() via entityYaw já pré-calculado
        // pelo EntityRenderDispatcher) ficaria lerpando entre o yaw de
        // ticks atrás e o atual, girando errado quando o jogador vira rápido.
        this.yRotO = this.getYRot();
        this.setYRot(yaw);
    }

    /** Área de culling generosa -- a onda se estende bem além da hitbox nominal (0.5x0.5). */
    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 1.3, getY() - 0.3, getZ() - 1.3, getX() + 1.3, getY() + 1.1, getZ() + 1.3);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Seed")) {
            this.entityData.set(SEED, tag.getInt("Seed"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Seed", getSeed());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Puramente decorativo -- nunca leva dano. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** Spawna a onda visual sincronizada no servidor e retorna a instância já adicionada ao nível. */
    public static LavaSurfWaveVisualEntity spawn(ServerLevel level, Vec3 pos, float yaw) {
        int seed = level.random.nextInt(Integer.MAX_VALUE);
        LavaSurfWaveVisualEntity wave = new LavaSurfWaveVisualEntity(level, pos, yaw, seed);
        level.addFreshEntity(wave);
        return wave;
    }
}