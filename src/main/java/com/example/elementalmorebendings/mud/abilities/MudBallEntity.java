package com.example.elementalmorebendings.mud.abilities;

import com.example.elementalmorebendings.registry.ModEntities;
import dev.saperate.elementals.data.ElementalConfig;
import dev.saperate.elementals.data.PlayerData;
import dev.saperate.elementals.entities.common.AbstractElementalsEntity;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;

import static dev.saperate.elementals.utils.SapsUtils.getEntityLookVector;

/**
 * MudBallEntity ("Mud Ball")
 * <p>
 * Praticamente uma cópia estrutural da AirBallEntity do jar base (mesmo
 * padrão de "bola controlada -> arremessada" que estende
 * AbstractElementalsEntity), só que sem explosão — em vez disso causa dano
 * direto e, se "mudBallBlindnessI" estiver desbloqueado, Cegueira, igual ao
 * que o nome do upgrade já sugeria antes de a Ability existir de verdade.
 * O visual (dois cubos girando, renderizados via block atlas) é feito por
 * {@link MudBallEntityRenderer}, copiando o truque da AirBallEntityRenderer
 * mas usando a textura vanilla de Mud (elementals:block/mud), então não
 * precisa de nenhum bloco/model novo pra ficar "costurada" no atlas.
 */
public class MudBallEntity extends AbstractElementalsEntity<Player> {

    public MudBallEntity(EntityType<MudBallEntity> type, Level world) {
        super(type, world, Player.class);
    }

    public MudBallEntity(Level world, Player owner) {
        super(ModEntities.MUD_BALL.get(), world, Player.class);
        setOwner(owner);
        setPos(owner.getX(), owner.getY(), owner.getZ());
    }

    public MudBallEntity(Level world, Player owner, double x, double y, double z) {
        super(ModEntities.MUD_BALL.get(), world, Player.class);
        setOwner(owner);
        setPos(x, y, z);
        setControlled(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (random.nextInt(0, 40) == 6) {
            playSound(SoundEvents.MUD_STEP, 1.0f,
                    (0.9f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f) * 0.7f);
        }

        Entity owner = getOwner();
        if (owner == null || isRemoved()) {
            return;
        }

        if (!owner.isCrouching()) {
            moveEntity();
        }
    }

    private void moveEntity() {
        if (getIsControlled()) {
            moveEntityTowardsGoal(getEntityLookVector(getOwner(), 2.5)
                    .subtract(0, 0.3, 0)
                    .toVector3f());
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public boolean damagesOnTouch() {
        return false;
    }

    @Override
    public void onHitEntity(Entity entity) {
        if (!(entity instanceof LivingEntity target) || getOwner() == null) {
            onCollision();
            return;
        }

        PlayerData plrData = PlayerData.get(getOwner());

        float damage = 3.0f;
        if (plrData.canUseUpgrade("mudBallPowerII")) {
            damage = 7.0f;
        } else if (plrData.canUseUpgrade("mudBallPowerI")) {
            damage = 5.0f;
        }

        target.hurt(this.damageSources().playerAttack(getOwner()),
                damage * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER);
        target.invulnerableTime = 10;

        if (plrData.canUseUpgrade("mudBallBlindnessI")) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }

        onCollision();
    }

    @Override
    public void collidesWithGround() {
        onCollision();
    }

    private void onCollision() {
        if (!level().isClientSide) {
            level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                    getX(), getY() + 0.3, getZ(), 25, 0.3, 0.3, 0.3, 0.05);
        }
        discard();
    }

    @Override
    public float getMovementSpeed() {
        return 0.1f;
    }

    @Override
    public void onClientRemoval() {
        level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.MUD_BREAK, SoundSource.BLOCKS,
                1.0F, (1.0F + (level().random.nextFloat() - level().random.nextFloat()) * 0.2F) * 0.7F, false);
    }

    @Override
    public float touchGroundFrictionMultiplier() {
        return -1;
    }
}