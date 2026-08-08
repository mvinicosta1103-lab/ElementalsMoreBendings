package com.elementals.morebendings.bending.earthsubbendings.lava;

import com.elementals.morebendings.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Entidade puramente visual do jato de {@code lavaGeyser} (ver {@link
 * LavaGeyserAbility}) -- mesmo esquema (sem física/gravidade/colisão,
 * nunca leva dano, {@code seed} sincronizado, conta os próprios ticks e
 * se auto-descarta) de {@link MagmaSpikeVisualEntity}, só que mais alta e
 * fina -- lê como um JATO vertical disparando pra cima, não uma
 * estalagmite brotando devagar. O giro contínuo por segmento (ver {@link
 * LavaGeyserVisualEntityRenderer}) simula a pressão do jato torcendo no
 * ar, em vez de uma inclinação estática por instância.
 * <p>
 * Duas fases, dois timers: {@code jetTicks} é quanto tempo o modelo 3D
 * fica de pé jorrando de verdade (não é mais uma erupção instantânea de
 * partícula -- ver {@link #tick()}), e {@code lifetimeTicks} (>= {@code
 * jetTicks}) é a vida TOTAL da entidade, incluindo uma cauda só de
 * fuligem depois que o jato já recolheu. A entidade continua viva (sem
 * modelo, só emitindo fumaça) até {@code lifetimeTicks} pra que a
 * fuligem assente no ar em vez de sumir de golpe junto com o jato.
 */
public class LavaGeyserVisualEntity extends Entity {

    private static final EntityDataAccessor<Integer> SEED =
            SynchedEntityData.defineId(LavaGeyserVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> JET_TICKS =
            SynchedEntityData.defineId(LavaGeyserVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME_TICKS =
            SynchedEntityData.defineId(LavaGeyserVisualEntity.class, EntityDataSerializers.INT);

    public LavaGeyserVisualEntity(EntityType<LavaGeyserVisualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * @param groundPos    posição do BLOCO de onde o jato brota -- a entidade nasce no topo dele, centralizada.
     * @param jetTicks     quanto tempo o jato de lava fica de pé jorrando (modelo 3D visível).
     * @param lifetimeTicks vida total (jato + cauda de fuligem sozinha); nunca menor que {@code jetTicks}.
     */
    public LavaGeyserVisualEntity(Level level, BlockPos groundPos, int jetTicks, int lifetimeTicks, int seed) {
        this(ModEntities.LAVA_GEYSER_VISUAL.get(), level);
        this.setPos(groundPos.getX() + 0.5, groundPos.getY() + 1.0, groundPos.getZ() + 0.5);
        this.entityData.set(SEED, seed);
        int clampedJet = Math.max(1, jetTicks);
        this.entityData.set(JET_TICKS, clampedJet);
        this.entityData.set(LIFETIME_TICKS, Math.max(clampedJet, lifetimeTicks));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SEED, 0);
        builder.define(JET_TICKS, 60);
        builder.define(LIFETIME_TICKS, 100);
    }

    public int getSeed() {
        return this.entityData.get(SEED);
    }

    /** Duração do jato em si (modelo 3D jorrando) -- ver {@link LavaGeyserVisualEntityRenderer}. */
    public int getJetTicks() {
        return this.entityData.get(JET_TICKS);
    }

    /** Vida total da entidade (jato + cauda de fuligem depois que o jato já recolheu). */
    public int getLifetimeTicks() {
        return this.entityData.get(LIFETIME_TICKS);
    }

    /** Área de culling generosa -- o jato sobe bem mais alto que a hitbox nominal (0.5x0.5). */
    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 0.8, getY() - 0.1, getZ() - 0.8, getX() + 0.8, getY() + 2.6, getZ() + 0.8);
    }

    @Override
    public void tick() {
        super.baseTick();
        if (this.level().isClientSide) {
            return;
        }

        int jetTicks = getJetTicks();
        int lifetimeTicks = getLifetimeTicks();

        // Fase 1: enquanto o modelo 3D está de pé, o jato jorra lava de verdade --
        // partícula contínua a cada tick, não só a explosão pontual do instante do impacto.
        if (this.tickCount < jetTicks) {
            spawnJetParticles((ServerLevel) this.level(), jetTicks);
        }

        // Fuligem acompanha o jato do início ao fim e continua sozinha depois que ele
        // recolhe, rareando aos poucos -- é isso que faz a fumaça "durar até depois do
        // Geyser acabar" em vez de sumir junto com o modelo.
        spawnSootParticles((ServerLevel) this.level(), jetTicks, lifetimeTicks);

        if (this.tickCount >= lifetimeTicks) {
            this.discard(); // servidor decide quando some, ver javadoc da classe
        }
    }

    /** Partículas de lava subindo pelo corpo do jato, com um "cuspe" ocasional de gotas no topo. */
    private void spawnJetParticles(ServerLevel level, int jetTicks) {
        float progress = Mth.clamp(this.tickCount / (float) jetTicks, 0f, 1f);
        // acompanha a curva de erupção/recolhimento do renderer: sobe rápido, some rápido no fim.
        double jetHeight = 1.8 * Mth.sin(progress * (float) Math.PI * 0.9f + 0.05f);
        jetHeight = Math.max(0.3, jetHeight);

        double px = getX() + (level.random.nextDouble() - 0.5) * 0.25;
        double pz = getZ() + (level.random.nextDouble() - 0.5) * 0.25;
        double py = getY() + level.random.nextDouble() * jetHeight;
        level.sendParticles(ParticleTypes.LAVA, px, py, pz, 2, 0.06, 0.12, 0.06, 0.05);

        if (this.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.DRIPPING_LAVA,
                    getX(), getY() + jetHeight, getZ(), 3, 0.22, 0.08, 0.22, 0.0);
        }
        // borbulhar ocasional pra dar corpo sonoro ao jato durante a fase em que ele jorra.
        if (this.tickCount % 10 == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS,
                    0.5f, 0.8f + level.random.nextFloat() * 0.4f);
        }
    }

    /** Fuligem: acompanha o jato inteiro e some aos poucos (fade) depois que ele já recolheu. */
    private void spawnSootParticles(ServerLevel level, int jetTicks, int lifetimeTicks) {
        boolean stillJetting = this.tickCount < jetTicks;
        int tailTicks = Math.max(1, lifetimeTicks - jetTicks);
        int ticksSinceJet = this.tickCount - jetTicks;
        float fade = stillJetting ? 1.0f : Mth.clamp(1.0f - (ticksSinceJet / (float) tailTicks), 0f, 1f);
        if (fade <= 0f || level.random.nextFloat() > fade) {
            return; // rareia até sumir de vez -- não é um corte abrupto
        }

        double px = getX() + (level.random.nextDouble() - 0.5) * 1.0;
        double pz = getZ() + (level.random.nextDouble() - 0.5) * 1.0;
        double py = getY() + 0.4 + level.random.nextDouble() * 1.6;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0.05, 0.09, 0.05, 0.01);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Seed")) {
            this.entityData.set(SEED, tag.getInt("Seed"));
        }
        if (tag.contains("JetTicks")) {
            this.entityData.set(JET_TICKS, tag.getInt("JetTicks"));
        }
        if (tag.contains("LifetimeTicks")) {
            this.entityData.set(LIFETIME_TICKS, tag.getInt("LifetimeTicks"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Seed", getSeed());
        tag.putInt("JetTicks", getJetTicks());
        tag.putInt("LifetimeTicks", getLifetimeTicks());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Puramente decorativo -- nunca leva dano (o dano de verdade já foi aplicado na hora do impacto). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /**
     * Spawna um jato visual sincronizado no servidor e retorna a instância já adicionada ao
     * nível. {@code jetTicks} é quanto tempo o jato jorra de pé; {@code lifetimeTicks} é a vida
     * total, incluindo a cauda de fuligem depois que o jato recolhe (deve ser >= jetTicks).
     */
    public static LavaGeyserVisualEntity spawn(ServerLevel level, BlockPos groundPos, int jetTicks, int lifetimeTicks) {
        int seed = level.random.nextInt(Integer.MAX_VALUE);
        LavaGeyserVisualEntity geyser = new LavaGeyserVisualEntity(level, groundPos, jetTicks, lifetimeTicks, seed);
        level.addFreshEntity(geyser);
        return geyser;
    }
}