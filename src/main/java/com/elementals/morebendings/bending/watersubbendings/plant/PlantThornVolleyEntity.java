package com.elementals.morebendings.bending.watersubbendings.plant;

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
 * Um único espinho da barragem de "thornVolley" — mesmo esquema de
 * {@code CrystalShardEntity}: entidade de verdade (via
 * {@link AbstractElementalsEntity}), sai voando em linha reta na direção
 * da mira do caster (com espalhamento aleatório, ver {@code divergence} em
 * {@link PlantThornVolleyAbility}), tem hitbox própria e pode errar o alvo.
 *
 * Diferencial temático em relação a crystalShard/boneSpike: além do dano de
 * impacto, aplica um tique curto de Veneno em quem for atingido — o
 * "espinho" continua incomodando depois da picada.
 */
public class PlantThornVolleyEntity extends AbstractElementalsEntity<Player> {

    private static final float DAMAGE = 3.0f;
    private static final int POISON_DURATION_TICKS = 60; // 3s
    private static final int POISON_AMPLIFIER = 0; // Veneno I

    public PlantThornVolleyEntity(EntityType<PlantThornVolleyEntity> type, Level level) {
        super(type, level, Player.class);
    }

    public PlantThornVolleyEntity(Level level, Player owner) {
        super(ModEntities.PLANT_THORN.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setNoGravity(true);
        this.maxLifeTime = 40; // 2s a 20 ticks/s — some sozinho se não acertar nada
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
            living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER));
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
        this.level().playSound((Entity) this, this.getOnPos(), SoundEvents.SWEET_BERRY_BUSH_BREAK,
                SoundSource.PLAYERS, 0.4f, 1.1f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
        for (int i = 0; i < 6; i++) {
            this.level().addParticle(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }
}