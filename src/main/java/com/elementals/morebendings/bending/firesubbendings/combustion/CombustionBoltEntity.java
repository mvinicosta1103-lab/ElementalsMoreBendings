package com.elementals.morebendings.bending.firesubbendings.combustion;

import com.elementals.morebendings.registry.ModEntities;
import dev.saperate.elementals.entities.common.AbstractElementalsEntity;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Diferente do tiro instantâneo padrão (raycast na hora, sem entidade),
 * esta é um projétil de verdade que sai devagar e pode ser levemente
 * "puxado" na direção do que o dono está olhando durante os primeiros
 * {@link #HOMING_TICKS} ticks de voo -- não é uma virada instantânea (não
 * teleporta, não gruda no alvo), é um ajuste de rota gradual a cada tick,
 * igual a como P'Li/Combustion Man conseguem corrigir a mira em cima de um
 * alvo que se move enquanto o tiro ainda está no ar. Depois da janela de
 * homing, o bolt trava a rota e vai reto até acertar algo ou expirar.
 *
 * Detona (ver {@link CombustionExplosionUtils#explode}) tanto em bloco
 * quanto em entidade -- diferente de CrystalShardEntity/BoneSpikeEntity
 * (que só têm dano direto de contato), aqui o impacto sempre gera uma
 * explosão em área.
 */
public class CombustionBoltEntity extends AbstractElementalsEntity<Player> {

    private static final int HOMING_TICKS = 30; // 1.5s de correção de rota após o lançamento
    private static final float TURN_RATE = 0.12f; // 0=nunca vira, 1=vira instantâneo (por tick)
    private static final double HOMING_RAYCAST_RANGE = 40.0;

    /** Definidos pela ability logo após a construção -- ver CombustionExplosionAbility#fire. */
    public float damage = 8.0f;
    public double explosionRadius = 2.5;

    private int flightTicks = 0;

    public CombustionBoltEntity(EntityType<CombustionBoltEntity> type, Level level) {
        super(type, level, Player.class);
    }

    public CombustionBoltEntity(Level level, Player owner) {
        super(ModEntities.COMBUSTION_BOLT.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setNoGravity(true);
        this.maxLifeTime = 80; // 4s -- some sozinho se não acertar nada nesse tempo
    }

    @Override
    public void tick() {
        super.tick();
        flightTicks++;

        if (!this.level().isClientSide && flightTicks <= HOMING_TICKS) {
            steerTowardsAim();
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                    this.getX(), this.getY(), this.getZ(), 2, 0.03, 0.03, 0.03, 0.005);
        }
    }

    /** Ajuste gradual de rota na direção do que o dono está mirando agora. */
    private void steerTowardsAim() {
        Player owner = this.getOwner();
        if (owner == null) {
            return;
        }

        HitResult aim = SapsUtils.raycastFull(owner, HOMING_RAYCAST_RANGE, false);
        Vec3 toAim = aim.getLocation().subtract(this.position());
        if (toAim.lengthSqr() < 0.0001) {
            return;
        }

        Vec3 desired = toAim.normalize();
        Vec3 current = this.getDeltaMovement();
        double speed = current.length();
        if (speed < 0.0001) {
            return;
        }

        Vec3 blended = current.normalize().scale(1.0 - TURN_RATE).add(desired.scale(TURN_RATE));
        if (blended.lengthSqr() < 0.0001) {
            return;
        }
        this.setDeltaMovement(blended.normalize().scale(speed));
    }

    @Override
    public void collidesWithGround() {
        detonate();
    }

    @Override
    public void onHitEntity(Entity entity) {
        detonate();
    }

    private void detonate() {
        if (this.level() instanceof ServerLevel level) {
            Player ownerPlayer = this.getOwner();
            net.minecraft.server.level.ServerPlayer owner =
                    ownerPlayer instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null;
            CombustionExplosionUtils.explode(level, this.position(), owner, damage, explosionRadius, 1.5);
        }
        this.discard();
    }

    /** Some sem dono depois de um tempo, pra não ficar voando pra sempre se o dono desconectar. */
    @Override
    public boolean discardsOnNullOwner() {
        return true;
    }

    @Override
    public void onClientRemoval() {
        if (!this.level().isClientSide) {
            return;
        }
        this.level().playSound((Entity) this, this.getOnPos(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.5f, 1.0f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }
}