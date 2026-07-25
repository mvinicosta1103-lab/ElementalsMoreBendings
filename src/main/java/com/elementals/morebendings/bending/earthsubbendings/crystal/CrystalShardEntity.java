package com.elementals.morebendings.bending.earthsubbendings.crystal;

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
 * Um único estilhaço da barragem de cristal ("crystalShard"). Diferente da
 * versão antiga (hitscan instantâneo com raycast + partículas), essa é uma
 * entidade de verdade: sai voando em linha reta na direção que o jogador
 * mirou (com um pequeno desvio aleatório — ver {@code divergence} em
 * {@link CrystalShardAbility}), tem hitbox própria e pode simplesmente
 * errar o alvo, igual uma flecha.
 *
 * Reaproveita {@link AbstractElementalsEntity}, a mesma classe-base que o
 * mod original usa pra AirBulletEntity/MetalBulletEntity/etc — ela já
 * resolve detecção de colisão com entidade/bloco a cada tick (ver
 * decompilado de AbstractElementalsEntity#tick). A gente só precisa dizer
 * o que acontece quando acerta algo ou bate no chão.
 *
 * Não usamos o sistema de "controlled" (mira automática/homing) que
 * Air/Metal bullets usam pra ficar seguindo o alvo — aqui é só um tiro reto
 * com velocidade inicial, tipo flecha.
 */
public class CrystalShardEntity extends AbstractElementalsEntity<Player> {

    private static final float DAMAGE = 4.0f;

    public CrystalShardEntity(EntityType<CrystalShardEntity> type, Level level) {
        super(type, level, Player.class);
    }

    public CrystalShardEntity(Level level, Player owner) {
        super(ModEntities.CRYSTAL_SHARD.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setNoGravity(true);
        this.maxLifeTime = 40; // 2s a 20 ticks/s — se não acertar nada nesse tempo, some sozinho
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
        this.level().playSound((Entity) this, this.getOnPos(), SoundEvents.AMETHYST_BLOCK_BREAK,
                SoundSource.PLAYERS, 0.4f, 1.2f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
        for (int i = 0; i < 6; i++) {
            this.level().addParticle(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }
}