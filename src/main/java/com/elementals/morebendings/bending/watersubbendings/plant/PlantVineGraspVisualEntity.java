package com.elementals.morebendings.bending.watersubbendings.plant;

import com.elementals.morebendings.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * "Modelo" do cipó de {@code vineGrasp} (ver {@link PlantVineGraspAbility}) --
 * entidade puramente visual, mesmo esquema de {@code CrystalSpikeVisualEntity}/
 * {@code MagmaSpikeVisualEntity} (sem física, sem gravidade, sem colisão, nunca
 * leva dano): não representa a vinha inteira como uma corda de verdade
 * (o mod base não expõe nenhum sistema de "corda" pronto), e sim um único
 * "caixote" bem fino e comprido -- a mesma técnica que {@code
 * PlantThornVolleyEntityRenderer}/{@code CrystalShardEntityRenderer} usam pra
 * desenhar farpas alongadas via {@code RenderUtils.drawCube} -- só que aqui o
 * comprimento MUDA a cada tick (sincronizado via {@link #LENGTH}), porque a
 * vinha precisa esticar/encolher continuamente entre a mão do caster e a
 * vítima agarrada enquanto os dois se movem.
 * <p>
 * Quem chama {@link #updateEndpoints} todo tick é {@link PlantVineGraspAbility#onTick},
 * direto (sem Manager/ServerTickEvent -- a própria ability já é canalizada e
 * já roda todo tick enquanto a tecla estiver segurada). A entidade nasce e
 * morre junto com a captura: {@link PlantVineGraspAbility} descarta ela em
 * {@code onRemove}/quando a vítima escapa.
 * <p>
 * A posição da entidade É a ponta de origem (mão do caster); a ponta de
 * destino (vítima) não é guardada como posição de verdade -- só como
 * yaw/pitch + comprimento sincronizados, igual {@link
 * com.elementals.morebendings.bending.watersubbendings.plant.PlantThornVolleyEntity}
 * usa yRot/xRot pra orientação de voo.
 */
public class PlantVineGraspVisualEntity extends Entity {

    private static final EntityDataAccessor<Float> LENGTH =
            SynchedEntityData.defineId(PlantVineGraspVisualEntity.class, EntityDataSerializers.FLOAT);

    public PlantVineGraspVisualEntity(EntityType<PlantVineGraspVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    private PlantVineGraspVisualEntity(Level level, Vec3 start, Vec3 end) {
        this(ModEntities.PLANT_VINE_GRASP.get(), level);
        updateEndpoints(start, end);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LENGTH, 0.0f);
    }

    /**
     * Reposiciona a vinha inteira: a entidade "vive" no ponto {@code start}
     * (mão do caster) e se estica em direção a {@code end} (vítima). Chamado
     * todo tick pela ability enquanto a captura durar -- como
     * {@code updateInterval} da entidade é baixo (ver {@code ModEntities}),
     * isso é reenviado ao cliente com frequência suficiente pra parecer uma
     * corda de verdade acompanhando os dois pontos, não um efeito estático.
     */
    public void updateEndpoints(Vec3 start, Vec3 end) {
        this.setPos(start.x, start.y, start.z);

        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        this.entityData.set(LENGTH, (float) dist);

        if (dist > 1.0E-4) {
            float yaw = (float) (Mth.atan2(diff.x, diff.z) * (180.0 / Math.PI));
            float horizontal = (float) Math.sqrt(diff.x * diff.x + diff.z * diff.z);
            float pitch = (float) (-(Mth.atan2(diff.y, horizontal)) * (180.0 / Math.PI));
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yRotO = yaw;
            this.xRotO = pitch;
        }
    }

    public float getLength() {
        return this.entityData.get(LENGTH);
    }

    /** Culling generoso -- o comprimento real varia bastante (2 a ~12 blocos) e não é refletido na hitbox nominal. */
    @Override
    public AABB getBoundingBoxForCulling() {
        double reach = Math.max(1.0, getLength()) + 1.0;
        return new AABB(getX() - reach, getY() - reach, getZ() - reach,
                getX() + reach, getY() + reach, getZ() + reach);
    }

    @Override
    public void tick() {
        super.baseTick(); // sem super.tick() de propósito -- posição/rotação são só o que a ability manda via updateEndpoints, nada de física própria
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Efeito puramente transitório de uma captura em andamento -- nada pra persistir entre save/load.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Idem.
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** Spawna a vinha já esticada entre os dois pontos e adiciona ao mundo -- helper usado por {@link PlantVineGraspAbility}. */
    public static PlantVineGraspVisualEntity spawn(ServerLevel level, Vec3 start, Vec3 end) {
        PlantVineGraspVisualEntity vine = new PlantVineGraspVisualEntity(level, start, end);
        level.addFreshEntity(vine);
        return vine;
    }
}