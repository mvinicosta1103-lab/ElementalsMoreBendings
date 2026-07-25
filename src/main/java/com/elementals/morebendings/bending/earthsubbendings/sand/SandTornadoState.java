package com.elementals.morebendings.bending.earthsubbendings.sand;

import dev.saperate.elementals.data.ElementalConfig;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Estado de um único tornado de areia ativo (um caster mantém um por vez).
 * Dirigido tick a tick por {@link SandTornadoManager#onServerTick}, mesmo
 * esquema que {@code MudTrapState} usa pra armadilha de lama.
 *
 * O que acontece:
 *
 *  1. {@link #begin} -- suga (vira {@link Blocks#AIR}) os blocos de
 *     {@code SAND}/{@code RED_SAND} num disco ao redor do centro mirado,
 *     guardando o estado original de cada um pra restaurar depois.
 *
 *  2. {@link #tick} -- gira uma "coluna" visual de partículas de areia
 *     (funil: mais larga embaixo, estreita em cima -- ver {@link
 *     #spawnSpiralParticles}) e, ao mesmo tempo, puxa qualquer
 *     {@link LivingEntity} próxima pro eixo central com um pequeno impulso
 *     pra cima (efeito de sucção), cega (Cegueira curta, renovada a cada
 *     tick enquanto estiver dentro) e aplica um dano leve periódico --
 *     literalmente ser jogado dentro de uma tempestade de areia giratória.
 *
 *  3. {@link #release} -- devolve os blocos sugados ao lugar original.
 *
 * Libera quando: o caster para de agachar, morre/desconecta, ou depois de
 * {@link #MAX_DURATION_TICKS} por segurança (failsafe).
 */
public class SandTornadoState {

    private static final int MAX_DURATION_TICKS = 20 * 20; // failsafe: 20s

    /** Raio (em blocos) do disco de areia sugado no chão, ao redor do centro mirado. */
    private static final int SUCK_RADIUS = 3;
    /** Altura visual do funil de partículas. */
    private static final double COLUMN_HEIGHT = 6.0;
    /** Raio de alcance da sucção/efeitos sobre entidades vivas. */
    private static final double PULL_RADIUS = 5.0;

    private static final double ANGULAR_SPEED_DEG = 22.0; // graus por tick, por "braço" do funil
    private static final int ARM_COUNT = 3;

    private static final double PULL_STRENGTH = 0.12;
    private static final double LIFT_STRENGTH = 0.06;

    private static final int BLIND_DURATION_TICKS = 15; // renovada tick a tick enquanto dentro
    private static final int DAMAGE_INTERVAL_TICKS = 10; // 1 tique de dano a cada meio segundo
    private static final float DAMAGE_AMOUNT = 1.0f;

    private static final Set<Block> SUCKABLE = Set.of(Blocks.SAND, Blocks.RED_SAND);

    private final ServerLevel level;
    private final ServerPlayer caster;
    private final BlockPos ground;
    private final Vec3 origin; // base da coluna, um bloco acima do chão mirado

    private final Map<BlockPos, BlockState> savedStates = new LinkedHashMap<>();

    private int ticksElapsed = 0;

    public SandTornadoState(ServerLevel level, ServerPlayer caster, BlockPos ground, Vec3 origin) {
        this.level = level;
        this.caster = caster;
        this.ground = ground;
        this.origin = origin;
    }

    /** Chamada uma vez, ao criar o tornado -- suga a areia próxima do chão. */
    public void begin() {
        boolean suckedAny = false;
        for (int dx = -SUCK_RADIUS; dx <= SUCK_RADIUS; dx++) {
            for (int dz = -SUCK_RADIUS; dz <= SUCK_RADIUS; dz++) {
                if (dx * dx + dz * dz > SUCK_RADIUS * SUCK_RADIUS) {
                    continue; // mantém o disco redondo, não quadrado
                }
                BlockPos pos = ground.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                if (!SUCKABLE.contains(state.getBlock())) {
                    continue;
                }
                savedStates.put(pos, state);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                suckedAny = true;
            }
        }

        if (suckedAny) {
            level.playSound(null, ground, SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
        level.playSound(null, ground, SoundEvents.SAND_FALL, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    /** @return true enquanto o tornado deve continuar ativo; false quando deve ser liberado. */
    public boolean tick() {
        ticksElapsed++;

        if (!caster.isAlive() || caster.isRemoved()) {
            return false;
        }
        if (!caster.isShiftKeyDown()) {
            return false;
        }
        if (ticksElapsed > MAX_DURATION_TICKS) {
            return false;
        }

        spawnSpiralParticles();
        pullAndDamageEntities();

        if (ticksElapsed % 8 == 0) {
            level.playSound(null, ground, SoundEvents.SAND_FALL, SoundSource.PLAYERS, 0.7f, 1.4f);
        }

        return true;
    }

    /** Desenha o funil giratório de partículas -- puramente visual, não mexe em blocos a cada tick. */
    private void spawnSpiralParticles() {
        BlockState particleState = savedStates.values().stream()
                .filter(s -> s.getBlock() == Blocks.RED_SAND)
                .findAny()
                .orElse(Blocks.SAND.defaultBlockState());

        for (int arm = 0; arm < ARM_COUNT; arm++) {
            double armOffsetDeg = arm * (360.0 / ARM_COUNT);
            // Duas alturas por braço, pra deixar a coluna mais "cheia" sem gastar partícula demais.
            for (int step = 0; step < 2; step++) {
                double heightProgress = ((ticksElapsed * 2 + step * 7) % 40) / 40.0; // 0..1, sobe e recomeça
                double height = heightProgress * COLUMN_HEIGHT;
                double radius = SUCK_RADIUS * (1.0 - heightProgress * 0.7); // funil: estreita subindo
                double angleRad = Math.toRadians(ticksElapsed * ANGULAR_SPEED_DEG + armOffsetDeg);

                double x = origin.x + Math.cos(angleRad) * radius;
                double z = origin.z + Math.sin(angleRad) * radius;
                double y = origin.y + height;

                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, particleState),
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    /** Puxa, cega e machuca levemente quem estiver perto o suficiente do tornado. */
    private void pullAndDamageEntities() {
        List<LivingEntity> caught = SapsUtils.getEntitiesInRadius(origin, (float) PULL_RADIUS, level, caster);

        boolean applyDamageThisTick = ticksElapsed % DAMAGE_INTERVAL_TICKS == 0;

        for (LivingEntity entity : caught) {
            if (!entity.isAlive()) {
                continue;
            }

            Vec3 toCenter = new Vec3(origin.x - entity.getX(), 0, origin.z - entity.getZ());
            double horizontalDist = toCenter.length();
            if (horizontalDist > 0.05) {
                Vec3 pull = toCenter.normalize().scale(PULL_STRENGTH);
                entity.setDeltaMovement(entity.getDeltaMovement().add(pull.x, LIFT_STRENGTH, pull.z));
                entity.hurtMarked = true;
            }

            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_DURATION_TICKS, 0, false, false));

            if (applyDamageThisTick) {
                entity.hurt(level.damageSources().playerAttack(caster),
                        DAMAGE_AMOUNT * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER);
            }
        }
    }

    /** Restaura os blocos sugados ao lugar original. Chamada uma vez, ao final. */
    public void release() {
        for (Map.Entry<BlockPos, BlockState> entry : savedStates.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        if (!savedStates.isEmpty()) {
            level.playSound(null, ground, SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                origin.x, origin.y + 0.5, origin.z, 20, 0.6, 0.6, 0.6, 0.03);
    }
}