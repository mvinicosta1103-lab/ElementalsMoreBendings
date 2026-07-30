package com.elementals.morebendings.bending.earthsubbendings.sand;

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
 * Entidade puramente visual do funil giratório de {@code sandTornado} (ver
 * {@link SandTornadoState}/{@link SandTornadoAbility}) -- mesmo padrão de
 * {@code MudSpikeVisualEntity}/{@code MagmaSpikeVisualEntity}: sem física,
 * sem gravidade, sem colisão, só existência + um {@code seed} sincronizado
 * pra o {@link SandTornadoVisualEntityRenderer} desenhar exatamente o
 * MESMO funil em todo cliente (mesmo giro/textura por segmento), em vez de
 * cada jogador sortear uma variação diferente.
 *
 * Substitui a antiga versão só-de-partículas por um modelo de verdade: um
 * funil de blocos de areia empilhados em espiral, girando continuamente.
 *
 * Diferente das visuais de espinho (que têm {@code lifetimeTicks} fixo),
 * esta fica viva enquanto o tornado durar -- a duração varia com o
 * jogador segurando agachado, então quem decide quando ela morre é o
 * próprio {@link SandTornadoState#release()}, chamando {@link #discard()}
 * diretamente, em vez de contar os próprios ticks.
 */
public class SandTornadoVisualEntity extends Entity {

    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(SandTornadoVisualEntity.class, EntityDataSerializers.INT);

    public SandTornadoVisualEntity(EntityType<SandTornadoVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** @param origin base do funil -- mesmo ponto que {@link SandTornadoState} já usa pros efeitos. */
    public SandTornadoVisualEntity(Level level, Vec3 origin, int seed) {
        this(ModEntities.SAND_TORNADO_VISUAL.get(), level);
        this.setPos(origin.x, origin.y, origin.z);
        this.entityData.set(SEED, seed);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    /** Área de culling generosa -- o funil é bem mais alto/largo que a hitbox nominal (0.5x0.5). */
    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 3.5, getY() - 0.2, getZ() - 3.5, getX() + 3.5, getY() + 7.0, getZ() + 3.5);
    }

    @Override
    public void tick() {
        super.baseTick();
        // Sem auto-descarte por tempo de vida: a duração do tornado varia
        // com o jogador segurando agachado, não é fixa -- quem chama
        // discard() é SandTornadoState#release().
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

    /** Puramente decorativo -- o dano de verdade já é aplicado por {@link SandTornadoState}. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /**
     * Spawna a visual sincronizada no servidor e retorna a instância já
     * adicionada ao nível -- helper usado por {@link SandTornadoState#begin()}.
     */
    public static SandTornadoVisualEntity spawn(ServerLevel level, Vec3 origin) {
        int seed = level.random.nextInt(Integer.MAX_VALUE);
        SandTornadoVisualEntity visual = new SandTornadoVisualEntity(level, origin, seed);
        level.addFreshEntity(visual);
        return visual;
    }
}