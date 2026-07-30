package com.elementals.morebendings.bending.earthsubbendings.sand;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estado de uma única cratera de areia movediça ativa (uma por caster).
 * Mesmo esquema geral de {@code PressureZoneState} (Atmosphere): zona
 * fixa num ponto do mundo, dirigida tick a tick por {@link
 * SandQuicksandManager#onServerTick}, que continua ativa por conta
 * própria enquanto durar -- não depende do caster ficar agachado nem por
 * perto (diferente de {@code SandTornadoState}/{@code MudTrapState}).
 *
 * Sem alterar nenhum bloco do mundo (diferente de {@code MudSpikesAbility}):
 * o "afundar" é só a própria entidade sendo puxada pra baixo aos poucos,
 * até no máximo {@link #MAX_SINK}, igual alguém atolando até a canela/
 * joelho -- nunca chega a sufocar. O efeito de verdade é a Lentidão/
 * Fraqueza pesada reaplicada tick a tick enquanto o alvo ficar dentro do
 * raio, simulando o esforço de tentar sair da areia solta.
 */
public class SandQuicksandState {

    private static final double HEIGHT = 2.0;
    private static final double MAX_SINK = 0.55; // nunca afunda mais que isso -- não sufoca
    private static final double SINK_STEP = 0.03;

    private static final int SLOWNESS_AMPLIFIER = 5;
    private static final int WEAKNESS_AMPLIFIER = 1;
    private static final int EFFECT_REFRESH_TICKS = 10;

    private final ServerLevel level;
    private final ServerPlayer caster;
    private final Vec3 center;
    private final double radius;
    private final int maxDurationTicks;

    /** Quanto cada alvo já afundou (em blocos), pra não passar de {@link #MAX_SINK}. */
    private final Map<UUID, Double> sunk = new HashMap<>();

    private int ticksElapsed = 0;

    public SandQuicksandState(ServerLevel level, ServerPlayer caster, Vec3 center, double radius, int maxDurationTicks) {
        this.level = level;
        this.caster = caster;
        this.center = center;
        this.radius = radius;
        this.maxDurationTicks = maxDurationTicks;
    }

    public void begin() {
        level.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z, 1, 0, 0, 0, 0);
    }

    /** @return true enquanto a cratera deve continuar ativa; false quando deve desmanchar. */
    public boolean tick() {
        ticksElapsed++;

        if (ticksElapsed > maxDurationTicks) {
            return false;
        }

        bogDownTargetsInside();
        spawnAmbientParticles();
        return true;
    }

    private void bogDownTargetsInside() {
        AABB area = new AABB(
                center.x - radius, center.y - 1.0, center.z - radius,
                center.x + radius, center.y + HEIGHT, center.z + radius);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            double distSq = target.position().distanceToSqr(center.x, target.getY(), center.z);
            if (distSq > radius * radius) {
                continue;
            }

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    EFFECT_REFRESH_TICKS, SLOWNESS_AMPLIFIER, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    EFFECT_REFRESH_TICKS, WEAKNESS_AMPLIFIER, false, false, true));

            double already = sunk.getOrDefault(target.getUUID(), 0.0);
            if (already < MAX_SINK && target.onGround()) {
                double step = Math.min(SINK_STEP, MAX_SINK - already);
                target.move(MoverType.SELF, new Vec3(0, -step, 0));
                sunk.put(target.getUUID(), already + step);
            }
        }
    }

    private void spawnAmbientParticles() {
        if (ticksElapsed % 5 != 0) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = level.random.nextDouble() * radius;
            double px = center.x + Math.cos(angle) * dist;
            double pz = center.z + Math.sin(angle) * dist;
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    px, center.y + 0.05, pz, 2, 0.1, 0.02, 0.1, 0.0);
        }
        if (ticksElapsed % 20 == 0) {
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.SAND_STEP, SoundSource.PLAYERS, 0.6f, 0.6f);
        }
    }

    /** Chamada uma vez, ao desmanchar a cratera (fim da duração). */
    public void release() {
        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                center.x, center.y, center.z, 16, radius * 0.5, 0.1, radius * 0.5, 0.02);
    }
}