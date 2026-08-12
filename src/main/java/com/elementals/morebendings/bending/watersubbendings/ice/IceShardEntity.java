package com.elementals.morebendings.bending.watersubbendings.ice;

import com.elementals.morebendings.registry.ModEntities;
import dev.saperate.elementals.data.ElementalConfig;
import dev.saperate.elementals.entities.common.AbstractElementalsEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Um único estilhaço da barragem de gelo ("iceShard") -- mesmo esquema
 * exato de {@code CrystalShardEntity}: sai voando em linha reta na direção
 * mirada (com pequeno desvio aleatório, ver {@code divergence} em {@link
 * IceShardAbility}), tem hitbox própria e reaproveita {@link
 * AbstractElementalsEntity} do mod base pra detecção de colisão.
 *
 * Diferente do estilhaço de cristal, ao acertar um alvo vivo também aplica
 * lentidão curta -- o "gelo" esfria o alvo além de cortar.
 */
public class IceShardEntity extends AbstractElementalsEntity<Player> {

    private static final float DAMAGE = 3.5f;
    private static final int SLOW_DURATION_TICKS = 40; // 2s

    public IceShardEntity(EntityType<IceShardEntity> type, Level level) {
        super(type, level, Player.class);
    }

    public IceShardEntity(Level level, Player owner) {
        super(ModEntities.ICE_SHARD.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setNoGravity(true);
        this.maxLifeTime = 40; // 2s a 20 ticks/s -- se não acertar nada nesse tempo, some sozinho
    }

    @Override
    public void tick() {
        super.tick();
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void collidesWithGround() {
        this.discard();
    }

    @Override
    public void onHitEntity(Entity entity) {
        Player owner = this.getOwner();
        entity.hurt(this.damageSources().playerAttack(owner), DAMAGE * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER);
        entity.hurtMarked = true;
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION_TICKS, 1));
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
        this.level().playSound((Entity) this, this.getOnPos(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.35f, 1.5f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
        for (int i = 0; i < 6; i++) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }
}