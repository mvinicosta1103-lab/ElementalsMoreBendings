package com.elementals.morebendings.bending.earthsubbendings.mud;

import com.elementals.morebendings.registry.ModEntities;
import dev.saperate.elementals.data.ElementalConfig;
import dev.saperate.elementals.entities.common.AbstractElementalsEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

/**
 * Projétil de "mudBall" (ver {@link MudBallAbility}). Mesmo esquema de
 * {@code CrystalShardEntity}: sai reto na direção mirada, tem hitbox
 * própria e pode errar o alvo -- sem homing/controle nenhum, diferente de
 * {@code LavaShurikenEntity}. Hitbox pequena, igual aos outros estilhaços
 * simples (crystal/glass/bone), já que é um tiro de contato/impacto.
 * <p>
 * Diferente dos estilhaços cortantes (crystal/glass/bone), uma bola de lama
 * não fere feito lâmina -- o "dano" dela é atordoar: impacto direto aplica
 * Lentidão por um tempo curto (mesma mecânica de {@code MudSurgeAbility}),
 * junto de um dano baixo de impacto.
 */
public class MudBallEntity extends AbstractElementalsEntity<Player> {

    private static final float DAMAGE = 2.5f;
    private static final int SLOWNESS_DURATION_TICKS = 40; // 2s
    private static final int SLOWNESS_AMPLIFIER = 1; // Lentidão II

    public MudBallEntity(EntityType<MudBallEntity> type, Level level) {
        super(type, level, Player.class);
    }

    public MudBallEntity(Level level, Player owner) {
        super(ModEntities.MUD_BALL.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setNoGravity(false); // uma bola de lama de verdade cai um pouco em voo, diferente do cristal
        this.maxLifeTime = 40; // 2s a 20 ticks/s -- se não acertar nada nesse tempo, some sozinho
    }

    @Override
    public void tick() {
        super.tick();
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void collidesWithGround() {
        splat();
        this.discard();
    }

    @Override
    public void onHitEntity(Entity entity) {
        Player owner = this.getOwner();
        entity.hurt(this.damageSources().playerAttack(owner), DAMAGE * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER);
        entity.hurtMarked = true;
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER));
        }
        splat();
        this.discard();
    }

    /** Some sem dono depois de um tempo, pra não ficar voando pra sempre se o dono desconectar. */
    @Override
    public boolean discardsOnNullOwner() {
        return true;
    }

    /** Espalha partículas de lama no ponto de impacto, tanto batendo no chão quanto numa entidade. */
    private void splat() {
        // sendParticles(BlockParticleOption, x, y, z, count, dx, dy, dz, speed) só existe
        // em ServerLevel (broadcast pros clientes que estão rastreando) -- Level (o tipo
        // genérico que this.level() retorna) só tem addParticle, cliente-only.
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                this.getX(), this.getY(), this.getZ(), 10, 0.25, 0.2, 0.25, 0.05);
    }

    @Override
    public void onClientRemoval() {
        if (!this.level().isClientSide) {
            return;
        }
        this.level().playSound((Entity) this, this.getOnPos(), SoundEvents.MUD_BREAK,
                SoundSource.PLAYERS, 0.5f, 1.0f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
        for (int i = 0; i < 6; i++) {
            this.level().addParticle(ParticleTypes.CRIT,
                    this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }
}