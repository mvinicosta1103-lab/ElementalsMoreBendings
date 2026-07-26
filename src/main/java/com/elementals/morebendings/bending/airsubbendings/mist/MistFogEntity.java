package com.elementals.morebendings.bending.airsubbendings.mist;

import com.elementals.morebendings.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Entidade puramente visual da névoa de Heavy Fog — não tem física de
 * projétil (ao contrário de {@code CrystalShardEntity}/{@code
 * BoneSpikeEntity}, que reaproveitam {@code AbstractElementalsEntity}):
 * ela nasce parada no centro da névoa e fica só ali, sem gravidade, sem
 * colisão, sem alvo. Toda a jogabilidade (Cegueira/Escuridão/dano/
 * lentidão) já é resolvida à parte por {@link MistCloudState} — esta
 * classe só existe pra dar ao {@link MistCloudManager} um "corpo" no
 * mundo pro {@link MistFogEntityRenderer} desenhar em cima.
 * <p>
 * Ciclo de vida: criada e adicionada ao nível por
 * {@link MistCloudState#MistCloudState}, descartada por
 * {@link MistCloudState#tick()} no mesmo instante em que a zona de
 * efeito termina — não tem lógica própria de expiração por tempo
 * (de propósito, pra não ter duas fontes de verdade sobre a duração).
 */
public class MistFogEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(MistFogEntity.class, EntityDataSerializers.FLOAT);

    public MistFogEntity(EntityType<MistFogEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public MistFogEntity(Level level, double x, double y, double z, double radius) {
        this(ModEntities.MIST_FOG.get(), level);
        this.setPos(x, y, z);
        this.entityData.set(DATA_RADIUS, (float) radius);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 4.0f);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /**
     * Área de renderização precisa cobrir o raio inteiro da névoa (até
     * ~5.5 blocos), senão o motor de culling pode esconder a névoa quando
     * a câmera olha pra borda dela em vez do centro. Bounding box "de
     * verdade" (colisão) continua pequena — só a de culling é ampliada.
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        float r = getRadius() + 1.0f;
        return new AABB(getX() - r, getY() - 1.0, getZ() - r, getX() + r, getY() + 3.0 + 1.0, getZ() + r);
    }

    @Override
    public void tick() {
        super.baseTick();
        // Sem movimento, sem colisão, sem lógica de jogo -- só existe pro
        // renderer ler getRadius()/tickCount. Ver MistCloudState pra tudo
        // que efetivamente acontece com jogadores/mobs.
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        if (tag.contains("Radius")) {
            this.entityData.set(DATA_RADIUS, tag.getFloat("Radius"));
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putFloat("Radius", getRadius());
    }

    /** Nunca é interagível/atacável -- é decoração pura. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
