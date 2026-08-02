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
 * Igual ao tiro de P'Li/Combustion Man: o projétil em si não tem modelo
 * renderizado (ver {@link CombustionBoltEntityRenderer}) e não deixa um
 * rastro óbvio de fogo/fumaça durante o voo -- só um leve rastro de ar
 * deslocado (ver {@link #spawnAirTrail}), sutil o bastante pra não
 * entregar de cara que é uma explosão vindo. A única coisa clara que o
 * alvo vê é o brilho no "terceiro olho" do bender no instante do disparo
 * e, um instante depois, a explosão no ponto de impacto.
 *
 * Sempre nasce um projétil de verdade (nunca mais um raycast instantâneo
 * upgrade {@code combustionGuidance} é só o campo {@link #guided}: sem
 * ele o bolt vai reto na direção do disparo, com ele pode "puxar"
 * levemente a rota na direção do que o dono está olhando durante os
 * primeiros {@link #HOMING_TICKS} ticks de voo -- não é uma virada
 * instantânea (não teleporta, não gruda no alvo), é um ajuste de rota
 * gradual a cada tick. Depois da janela de homing, o bolt trava a rota e
 * vai reto até acertar algo ou expirar.
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

    /** Só true com o upgrade combustionGuidance -- ver CombustionExplosionAbility#fire. */
    public boolean guided = false;

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

        if (!this.level().isClientSide && guided && flightTicks <= HOMING_TICKS) {
            steerTowardsAim();
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        // Não é mais 100% invisível em voo: em vez de um rastro de fogo/fumaça
        // óbvio (o que entregaria que é Combustion de cara), deixa só uma
        // leve perturbação no ar -- poucas partículas de Cloud, sem
        // velocidade própria, soltando devagar. Dá pra notar de perto se
        // estiver prestando atenção, mas não é um rastro chamativo tipo
        // bala de canhão. Só client-side, senão duplica (servidor não
        // desenha partícula).
        if (this.level().isClientSide) {
            spawnAirTrail();
        }
    }

    /** Rastro sutil de "ar deslocado" -- ver comentário em {@link #tick()}. */
    private void spawnAirTrail() {
        this.level().addParticle(ParticleTypes.CLOUD,
                this.getX(), this.getY(), this.getZ(),
                0.0, 0.0, 0.0);
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