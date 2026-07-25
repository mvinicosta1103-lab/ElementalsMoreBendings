package com.elementals.morebendings.bending.earthsubbendings.bone;

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
 * A farpa/espinho de osso que a "boneControl" (ver {@link BoneControlAbility})
 * conjura e deixa o jogador guiar antes de arremessar.
 *
 * Reaproveita {@link AbstractElementalsEntity} igual a {@code CrystalShardEntity}
 * deste addon e a {@code EarthBlockEntity}/{@code MetalBulletEntity} do mod base:
 *
 *  - Enquanto {@code getIsControlled() == true} (recém-conjurada, sendo mirada),
 *    NADA de detecção de colisão/dano roda sobre ela -- isso já é assim por
 *    padrão em {@code AbstractElementalsEntity#tick} (os checks de bloco/entidade
 *    só rodam quando {@code !getIsControlled()}). A gente só precisa mover ela: é
 *    a {@link BoneControlAbility#onTick} que chama
 *    {@link #moveEntityTowardsGoal(org.joml.Vector3f, float)} a cada tick, igual
 *    {@code AbilityBloodControl} faz com a vítima -- aqui é só com uma entidade
 *    nossa em vez de um LivingEntity de verdade.
 *  - Ao ser arremessada (ver {@code onLeftClick} da ability), a ability chama
 *    {@code setControlled(false)} + {@code setDeltaMovement(shooter, ...)} (igual
 *    {@code CrystalShardEntity}/{@code AbilityEarthBoulder} fazem) -- a partir
 *    daí o próprio {@code AbstractElementalsEntity#tick} liga o raycast de
 *    projétil sozinho e chama {@link #onHitEntity} quando acertar alguém.
 */
public class BoneSpikeEntity extends AbstractElementalsEntity<Player> {

    private static final float DAMAGE = 6.0f;

    public BoneSpikeEntity(EntityType<BoneSpikeEntity> type, Level level) {
        super(type, level, Player.class);
    }

    public BoneSpikeEntity(Level level, Player owner) {
        super(ModEntities.BONE_SPIKE.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        this.setNoGravity(true); // não afeta nem enquanto controlada nem depois de arremessada
        this.setControlled(true);
    }

    @Override
    public void tick() {
        super.tick();
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    /** Só é chamado depois de arremessada (ver comentário da classe) -- se bateu
     * num bloco sem acertar ninguém, quebra e some, igual a CrystalShardEntity. */
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

    /** Some sem dono depois de um tempo, pra não ficar flutuando pra sempre se o
     * jogador que a estava controlando desconectar no meio da mira. */
    @Override
    public boolean discardsOnNullOwner() {
        return true;
    }

    @Override
    public void onClientRemoval() {
        if (!this.level().isClientSide) {
            return;
        }
        this.level().playSound((Entity) this, this.getOnPos(), SoundEvents.BONE_BLOCK_BREAK,
                SoundSource.PLAYERS, 0.5f, 1.0f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
        for (int i = 0; i < 6; i++) {
            this.level().addParticle(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }
}