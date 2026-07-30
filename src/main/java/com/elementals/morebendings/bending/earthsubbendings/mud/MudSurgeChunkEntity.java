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
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * UM pedaço de lama endurecida da leva de {@code mudSurge} (ver {@link
 * MudSurgeAbility}). Antes "mudSurge" era só um raycast AABB instantâneo:
 * checava quem estava na caixa na hora do cast e aplicava Lentidão -- sem
 * nada voando de verdade, só um leque de partículas cosméticas. Agora cada
 * "fatia" da onda é uma entidade real: um pedaço anguloso de lama seca que
 * sai disparado rente ao chão na direção mirada e continua avançando até
 * bater em algo sólido ou esgotar o alcance.
 * <p>
 * PERFURANTE DE PROPÓSITO (isso que faz virar AoE de verdade): diferente de
 * {@code MudBallEntity} ou {@code CrystalShardEntity} (que se desmancham no
 * primeiro alvo acertado), um pedaço de {@code mudSurge} atravessa
 * quem acerta e continua reto -- vários pedaços lado a lado (ver {@link
 * MudSurgeAbility#onCall}) formam uma frente de impacto que varre uma
 * fileira inteira de inimigos de uma vez, não um projétil único mirado em
 * uma vítima. {@link #hitEntityIds} evita que o MESMO pedaço acerte a MESMA
 * vítima mais de uma vez enquanto as hitboxes ainda estão sobrepostas (a
 * vítima pode ficar dentro da caixa do pedaço por 2-3 ticks seguidos).
 * <p>
 * Sem gravidade -- diferente de {@link MudBallEntity} (que arqueia como uma
 * bola de verdade), aqui o pedaço precisa se manter rente ao chão pra
 * "varrer" a área, então voa reto na altura em que nasceu até parar.
 */
public class MudSurgeChunkEntity extends AbstractElementalsEntity<Player> {

    private static final float DAMAGE = 3.0f;
    private static final float KNOCKBACK_FORWARD = 0.5f;
    private static final float KNOCKBACK_UP = 0.25f;
    private static final int SLOWNESS_DURATION_TICKS = 60; // 3s
    private static final int SLOWNESS_AMPLIFIER = 2; // Lentidão III

    /** IDs de entidades já atingidas por ESTE pedaço específico -- evita hit múltiplo enquanto atravessa a vítima. */
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public MudSurgeChunkEntity(EntityType<MudSurgeChunkEntity> type, Level level) {
        super(type, level, Player.class);
    }

    /**
     * Spawna um pedaço já em movimento.
     *
     * @param x, y, z       posição inicial (uma "fatia" da leva, deslocada lateralmente das outras -- ver {@link MudSurgeAbility})
     * @param direction     vetor de direção NORMALIZADO (mesmo pra todos os pedaços da mesma leva)
     * @param speed         velocidade em blocos/tick
     * @param lifetimeTicks depois de quantos ticks o pedaço some sozinho, mesmo sem bater em nada (ver {@link #maxLifeTime})
     */
    public MudSurgeChunkEntity(Level level, Player owner, double x, double y, double z, Vec3 direction, double speed, int lifetimeTicks) {
        super(ModEntities.MUD_SURGE_CHUNK.get(), level, Player.class);
        this.setOwner(owner);
        this.setPos(x, y, z);
        this.setNoGravity(true);
        this.setDeltaMovement(direction.scale(speed));
        this.maxLifeTime = lifetimeTicks;
        // Yaw alinhado com a direção do avanço só pro renderer poder orientar o
        // pedaço tombando "pra frente" em vez de girar num eixo arbitrário.
        // yRotO sincronizado na hora pra não ter uma guinada visual de 0° ->
        // yaw real no primeiro frame interpolado (Entity nasce com yRotO = 0).
        this.setYRot((float) (Math.toDegrees(Math.atan2(-direction.x, direction.z))));
        this.yRotO = this.getYRot();
    }

    @Override
    public void tick() {
        super.tick();
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void collidesWithGround() {
        // Bateu em bloco sólido de verdade (parede, tronco, etc) -- a onda para aí, não atravessa terreno.
        splat();
        this.discard();
    }

    @Override
    public void onHitEntity(Entity entity) {
        applyHit(entity);
    }

    @Override
    public void onTouchEntity(Entity entity) {
        applyHit(entity);
    }

    @Override
    public boolean damagesOnTouch() {
        return true;
    }

    /** Dano + arremesso + Lentidão, uma vez só por vítima -- ver javadoc da classe sobre {@link #hitEntityIds}. */
    private void applyHit(Entity entity) {
        if (this.level().isClientSide || !this.hitEntityIds.add(entity.getId())) {
            return;
        }

        Player owner = this.getOwner();
        if (owner == null) {
            return;
        }

        entity.hurt(this.damageSources().playerAttack(owner), DAMAGE * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER);
        entity.hurtMarked = true;

        Vec3 push = this.getDeltaMovement().normalize().scale(KNOCKBACK_FORWARD).add(0, KNOCKBACK_UP, 0);
        entity.push(push.x, push.y, push.z);

        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER));
        }
    }

    /** Some sem dono depois de um tempo, pra não ficar avançando pra sempre se o dono desconectar. */
    @Override
    public boolean discardsOnNullOwner() {
        return true;
    }

    private void splat() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                this.getX(), this.getY(), this.getZ(), 8, 0.2, 0.15, 0.2, 0.05);
    }

    @Override
    public void onClientRemoval() {
        if (!this.level().isClientSide) {
            return;
        }
        this.level().playSound((Entity) this, this.getOnPos(), SoundEvents.MUD_BREAK,
                SoundSource.NEUTRAL, 0.4f, 0.8f + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2f);
    }
}