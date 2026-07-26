package com.elementals.morebendings.bending.earthsubbendings.glass;

import com.elementals.morebendings.registry.ModEntities;
import dev.saperate.elementals.data.ElementalConfig;
import dev.saperate.elementals.entities.common.AbstractElementalsEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Um único estilhaço de vidro disparado pela {@link GlassShardsAbility}.
 * Mesmo esquema da {@code CrystalShardEntity}: entidade de projétil de
 * verdade em vez do hitscan antigo -- sai voando em linha reta na direção
 * que o jogador mirou, tem hitbox própria e pode errar o alvo.
 *
 * Reaproveita {@link AbstractElementalsEntity}, a mesma classe-base usada
 * por CrystalShardEntity/BoneSpikeEntity (e pelas entidades vanilla do mod
 * base, tipo AirBulletEntity/MetalBulletEntity) -- ela já cuida da detecção
 * de colisão com entidade/bloco a cada tick; só precisamos dizer o que
 * acontece quando acerta algo ou bate no chão.
 */
public class GlassShardEntity extends AbstractElementalsEntity<Player> {

    private static final float DAMAGE = 3.0f;

    public GlassShardEntity(EntityType<GlassShardEntity> type, Level level) {
        super(type, level, Player.class);
    }

    public GlassShardEntity(Level level, Player owner) {
        super(ModEntities.GLASS_SHARD.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setNoGravity(true);
        this.maxLifeTime = 40; // 2s a 20 ticks/s -- some sozinho se não acertar nada
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
                SoundSource.PLAYERS, 0.4f, 1.2f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
        for (int i = 0; i < 6; i++) {
            this.level().addParticle(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }
}