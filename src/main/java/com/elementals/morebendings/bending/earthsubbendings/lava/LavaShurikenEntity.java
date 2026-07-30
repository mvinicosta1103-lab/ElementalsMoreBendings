package com.elementals.morebendings.bending.earthsubbendings.lava;

import com.elementals.morebendings.registry.ModEntities;
import dev.saperate.elementals.data.ElementalConfig;
import dev.saperate.elementals.entities.common.AbstractElementalsEntity;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Entidade do "lavaShuriken" (ver {@link LavaShurikenAbility}). Decompilei
 * {@code WaterBladeEntity} (dev.saperate.elementals.entities.water) do jar
 * base pra entender o sistema de "controlled" que {@link
 * AbstractElementalsEntity} já expõe (usado por Water Blade, Air/Metal
 * Bullet etc no mod original) e reaproveitei o mesmo esquema aqui:
 *
 *  - Enquanto {@code getIsControlled()} for true, {@link #controlEntity}
 *    roda todo tick e faz a entidade "flutuar" na direção que o dono está
 *    olhando (com um pequeno alcance extra se estiver agachado, igual o
 *    Water Blade original), acelerando suavemente até o ponto mirado em
 *    vez de teleportar -- comportamento herdado de
 *    {@code SapsUtils.getEntityLookVector} + aproximação por delta.
 *  - {@link LavaShurikenAbility#onLeftClick} desliga o controle e lança a
 *    farpa reto na mira, igual o Water Blade faz.
 *
 * DIFERENÇAS DELIBERADAS em relação ao Water Blade original (pedido do
 * addon):
 *  1. MAIS OFENSIVA: dano base bem maior ({@link #DEFAULT_DAMAGE} = 12 vs
 *     7.5 do Water Blade) e sem o modo de "furar bloco devagar" (mining) --
 *     em vez disso, ao encostar numa entidade ela sempre atravessa
 *     (perfura) causando dano de toque contínuo enquanto controlada, e
 *     dano cheio + discard no impacto direto quando arremessada solta.
 *  2. SECA ÁGUA EM OBSIDIANA: toda vez que a farpa está dentro/tocando um
 *     bloco de água (fonte ou corrente), a água vira {@link
 *     Blocks#OBSIDIAN} na hora -- o oposto exato da própria mecânica de
 *     lava+água do jogo.
 *  3. Imune a fogo (óbvio, é lava) e emite luz fraca.
 *
 * CORREÇÕES (ver conversa de debug):
 *  - {@link #onHitEntity} e {@link #onTouchEntity} agora checam
 *    {@code owner == null} antes de chamar {@code damageSources().playerAttack(owner)}.
 *    Sem essa checagem, se o dono desconectar (ou, no client, se o owner
 *    ainda não tiver sido resolvido via rede no primeiro tick pós-spawn)
 *    o vanilla lança NullPointerException dentro de playerAttack(null) e
 *    derruba o client -- esse é o crash reportado ao usar a habilidade.
 *  - {@link #dryNearbyWater} agora só roda a cada 5 ticks em vez de todo
 *    tick, pra evitar dezenas de setBlock (+ recalculo de luz) por tick
 *    perto de água, que pode gerar lag severo ou disparar o watchdog do
 *    servidor integrado.
 */
public class LavaShurikenEntity extends AbstractElementalsEntity<Player> {

    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(LavaShurikenEntity.class, EntityDataSerializers.FLOAT);

    private static final float DEFAULT_DAMAGE = 12.0f;
    /** Dano de "toque" (roçar) enquanto controlada, sem discard -- fração do dano de impacto cheio. */
    private static final float TOUCH_DAMAGE_FRACTION = 0.35f;
    /** Raio (em blocos) checado ao redor da farpa toda vez que ela está dentro de água. */
    private static final int DRY_RADIUS = 1;
    /** A cada quantos ticks a checagem de água roda (throttle de performance). */
    private static final int DRY_CHECK_INTERVAL = 5;

    public LavaShurikenEntity(EntityType<LavaShurikenEntity> type, Level level) {
        super(type, level, Player.class);
    }

    /** Spawna parada na posição do dono, ainda sem controle -- mesmo construtor "cru" do Water Blade. */
    public LavaShurikenEntity(Level level, Player owner) {
        super(ModEntities.LAVA_SHURIKEN.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    /** Spawna já numa posição específica e já controlada -- usado por {@link LavaShurikenAbility#onCall}. */
    public LavaShurikenEntity(Level level, Player owner, double x, double y, double z) {
        super(ModEntities.LAVA_SHURIKEN.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(x, y, z);
        this.setControlled(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DAMAGE, DEFAULT_DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount % DRY_CHECK_INTERVAL == 0) {
            dryNearbyWater();
        }

        Player owner = this.getOwner();
        if (owner == null || this.isRemoved()) {
            return;
        }
        moveEntity(owner);
    }

    /** Mesma lógica de {@code WaterBladeEntity#moveEntity}: leve gravidade, controle se ativo, depois aplica o movimento. */
    private void moveEntity(Entity owner) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.02, 0.0));
        if (this.getIsControlled()) {
            controlEntity(owner);
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    /** Mesma lógica de {@code WaterBladeEntity#controlEntity}: persegue um ponto na frente do olhar do dono. */
    private void controlEntity(Entity owner) {
        float reach = owner.isCrouching() ? 6.0f : 3.0f;
        Vector3f toGoal = SapsUtils.getEntityLookVector(owner, reach).subtract(this.position()).toVector3f();
        toGoal.mul(0.25f);
        if (toGoal.length() < 0.6f) {
            this.setDeltaMovement(0, 0, 0);
        }
        this.addDeltaMovement(new Vec3(toGoal.x, toGoal.y, toGoal.z));
    }

    /**
     * Converte água num raio pequeno ao redor pra obsidiana, se a farpa
     * estiver dentro (ou tocando) um bloco de água. Agora chamada só a
     * cada {@link #DRY_CHECK_INTERVAL} ticks (ver javadoc da classe).
     */
    private void dryNearbyWater() {
        if (this.level().isClientSide) {
            return;
        }

        BlockPos center = this.blockPosition();
        boolean converted = false;

        for (int dx = -DRY_RADIUS; dx <= DRY_RADIUS; dx++) {
            for (int dy = -DRY_RADIUS; dy <= DRY_RADIUS; dy++) {
                for (int dz = -DRY_RADIUS; dz <= DRY_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (this.level().getBlockState(pos).is(Blocks.WATER)) {
                        this.level().setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.0);
                        }
                        converted = true;
                    }
                }
            }
        }

        if (converted) {
            this.level().playSound(null, center, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.1f);
        }
    }

    @Override
    public void collidesWithGround() {
        // Só quebra ao bater no chão se NÃO estiver mais sob controle --
        // igual o Water Blade: enquanto controlada, ela pode encostar no
        // chão sem se desmanchar (deixa o jogador raspar ela rente ao
        // terreno de propósito).
        if (!this.getIsControlled()) {
            this.discard();
        }
    }

    @Override
    public void onHitEntity(Entity entity) {
        // Impacto direto (farpa arremessada, sem controle) -- dano cheio + discard, igual o Water Blade.
        Player owner = this.getOwner();
        if (owner == null) {
            // Dono sumiu (desconectou / ainda não resolvido) -- não dá pra
            // atribuir o dano a ninguém, então só descarta a entidade em
            // vez de estourar NPE em playerAttack(null).
            this.discard();
            return;
        }
        entity.hurt(this.damageSources().playerAttack(owner), this.getDamage() * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER);
        entity.igniteForSeconds(3);
        entity.addDeltaMovement(this.getDeltaMovement().scale(0.8));
        entity.hurtMarked = true;
        this.discard();
    }

    @Override
    public void onTouchEntity(Entity entity) {
        // Roçar nela enquanto controlada -- dano parcial periódico, sem discard.
        if (this.tickCount % 10 != 0) {
            return;
        }
        Player owner = this.getOwner();
        if (owner == null) {
            return;
        }
        entity.hurt(this.damageSources().playerAttack(owner),
                this.getDamage() * TOUCH_DAMAGE_FRACTION * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER);
    }

    @Override
    public boolean damagesOnTouch() {
        return true;
    }

    @Override
    public boolean discardsOnNullOwner() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean emitsLight() {
        return true;
    }

    public void setDamage(float damage) {
        this.getEntityData().set(DAMAGE, damage);
    }

    public float getDamage() {
        return this.getEntityData().get(DAMAGE);
    }

    @Override
    public void onClientRemoval() {
        if (!this.level().isClientSide) {
            return;
        }
        this.level().playSound(this, this.getOnPos(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS,
                0.5f, 1.0f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(ParticleTypes.LAVA,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }
}