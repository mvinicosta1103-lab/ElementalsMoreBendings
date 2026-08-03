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
import net.minecraft.world.entity.LivingEntity;
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

    // --- modo "auto-gerenciado" (usado por PlantRootSnareAbility) -----------
    // Todos server-only, NUNCA sincronizados -- só servem pro tick() do
    // servidor decidir como recalcular endpoints/quando se descartar
    // sozinho. vineGrasp nunca toca nesses campos (fica tudo no default
    // abaixo, selfManagedTargetId == -1), então o comportamento "cipó
    // reposicionado de fora, todo tick, pela ability" -- usado por {@link
    // PlantVineGraspAbility} -- continua idêntico a antes.
    private int selfManagedTargetId = -1;
    private Vec3 selfManagedGroundAnchor = Vec3.ZERO;
    private float selfManagedAttachHeightFraction = 0.6f;
    private int selfManagedGrowTicks;
    private int selfManagedHoldTicks;
    private int selfManagedShrinkTicks;
    private int selfManagedElapsed = 0;

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
        // Modo auto-gerenciado (rootSnare): a própria entidade decide
        // endpoints/quando sumir, sem ninguém de fora chamando
        // updateEndpoints a cada tick (diferente de vineGrasp, que É
        // canalizada e tem a ability chamando isso o tempo todo). Só roda
        // no servidor -- o cliente só precisa dos valores já sincronizados
        // via entityData (LENGTH/posição/rotação), igual já acontece pro
        // cipó de vineGrasp normal.
        if (selfManagedTargetId != -1 && !this.level().isClientSide) {
            tickSelfManaged();
            return;
        }
        super.baseTick(); // sem super.tick() de propósito -- posição/rotação são só o que a ability manda via updateEndpoints, nada de física própria
    }

    private void tickSelfManaged() {
        if (!(this.level().getEntity(selfManagedTargetId) instanceof LivingEntity target) || !target.isAlive()) {
            discard();
            return;
        }

        selfManagedElapsed++;
        int totalTicks = selfManagedGrowTicks + selfManagedHoldTicks + selfManagedShrinkTicks;
        Vec3 attachPoint = target.position().add(0, target.getBbHeight() * selfManagedAttachHeightFraction, 0);

        Vec3 end;
        if (selfManagedElapsed <= selfManagedGrowTicks) {
            // Fase 1: brota do chão -- endpoint sobe da âncora até o ponto de "agarrar".
            end = selfManagedGroundAnchor.lerp(attachPoint, selfManagedElapsed / (double) selfManagedGrowTicks);
        } else if (selfManagedElapsed <= selfManagedGrowTicks + selfManagedHoldTicks) {
            // Fase 2: segura -- acompanha a vítima (que pode ser empurrada um pouco mesmo lenta).
            end = attachPoint;
        } else if (selfManagedElapsed <= totalTicks) {
            // Fase 3: solta -- encolhe de volta pro chão antes de se descartar.
            double t = (selfManagedElapsed - selfManagedGrowTicks - selfManagedHoldTicks) / (double) selfManagedShrinkTicks;
            end = attachPoint.lerp(selfManagedGroundAnchor, t);
        } else {
            discard();
            return;
        }

        updateEndpoints(selfManagedGroundAnchor, end);
        super.baseTick();
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

    /**
     * Spawna uma vinha "fire and forget" que brota do chão do lado da
     * vítima e sobe sozinha até prender ela, sem precisar de ninguém
     * chamando {@link #updateEndpoints} de fora -- usado por {@code
     * PlantRootSnareAbility}, que é instantânea (não canaliza {@code
     * currAbility}, não tem {@code onTick} pra ficar reposicionando a
     * vinha manualmente). Ver {@link #tickSelfManaged()} pro ciclo de vida
     * completo (cresce / segura acompanhando o alvo / encolhe / descarta).
     *
     * @param angleRadians ângulo (em radianos) ao redor da vítima onde a
     *                      âncora no chão nasce -- chame várias vezes com
     *                      ângulos diferentes pra várias vinhas ao redor do
     *                      mesmo alvo (efeito de "cercado por raízes").
     * @param radius        distância horizontal da âncora até o centro da vítima.
     */
    public static PlantVineGraspVisualEntity spawnRootSnareVine(ServerLevel level, LivingEntity target,
                                                                double angleRadians, double radius, int growTicks, int holdTicks, int shrinkTicks) {
        PlantVineGraspVisualEntity vine = new PlantVineGraspVisualEntity(ModEntities.PLANT_ROOT_SNARE_VINE.get(), level);

        double ox = Math.cos(angleRadians) * radius;
        double oz = Math.sin(angleRadians) * radius;
        vine.selfManagedGroundAnchor = new Vec3(target.getX() + ox, target.getY(), target.getZ() + oz);
        vine.selfManagedTargetId = target.getId();
        vine.selfManagedGrowTicks = Math.max(1, growTicks);
        vine.selfManagedHoldTicks = Math.max(0, holdTicks);
        vine.selfManagedShrinkTicks = Math.max(1, shrinkTicks);

        // Nasce com comprimento 0 (âncora == ponta) -- o primeiro tick de
        // tickSelfManaged() já cuida de esticar pra cima na fase de crescimento.
        vine.setPos(vine.selfManagedGroundAnchor.x, vine.selfManagedGroundAnchor.y, vine.selfManagedGroundAnchor.z);
        vine.updateEndpoints(vine.selfManagedGroundAnchor, vine.selfManagedGroundAnchor);

        level.addFreshEntity(vine);
        return vine;
    }
}