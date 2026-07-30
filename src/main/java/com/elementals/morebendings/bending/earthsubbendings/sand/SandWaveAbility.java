package com.elementals.morebendings.bending.earthsubbendings.sand;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "sandWave" — quarta habilidade raiz da árvore de Sand (ver {@link
 * SandElement}), completando o teto de 4 filhos diretos que a
 * {@code UpgradeTreeScreen} do mod base desenha (mesmo limite documentado
 * em {@code MudElement}/{@code LavaElement}).
 *
 * De mobilidade pura, não ofensiva -- o clássico "surfar" em cima da
 * areia que sandbenders fazem no Avatar: só pode ser usada com o jogador
 * em pé sobre um bloco "arenoso" ({@link #SURFABLE}), e desliza ele pra
 * frente na direção em que está olhando (só o componente horizontal --
 * ver {@link #onCall}), com um breve bônus de velocidade e Queda Lenta
 * pra não se machucar caindo de algum desnível no meio do caminho.
 *
 * Cooldown-based (mesmo esquema de {@code UpdraftAbility}/{@code
 * ObsidianCrustAbility}) em vez de só chi, senão dava pra encadear
 * impulsos sem parar e atravessar o mapa inteiro instantaneamente.
 */
public class SandWaveAbility implements Ability {

    private static final double DASH_SPEED = 1.6;
    private static final float CHI_COST = 12.0f;
    private static final int COOLDOWN_TICKS = 30; // 1.5s
    private static final int SPEED_DURATION_TICKS = 30; // 1.5s
    private static final int SPEED_AMPLIFIER = 2;
    private static final int SLOW_FALLING_DURATION_TICKS = 60; // 3s

    /** Chão liso o bastante pra "surfar" em cima. */
    private static final Set<Block> SURFABLE = Set.of(Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL);

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -1_000_000L);
        if (now - last < COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        BlockPos below = caster.blockPosition().below();
        if (!SURFABLE.contains(level.getBlockState(below).getBlock())) {
            caster.displayClientMessage(
                    Component.literal("Precisa estar sobre areia para surfar."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        // Só o componente horizontal da mira -- "surfar" fica rente ao chão,
        // não é um dash pra cima/baixo como o updraft.
        Vec3 look = caster.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize().scale(DASH_SPEED);

        caster.setDeltaMovement(horizontal.x, Math.max(caster.getDeltaMovement().y, 0.1), horizontal.z);
        caster.hasImpulse = true;
        caster.fallDistance = 0;

        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SPEED_DURATION_TICKS, SPEED_AMPLIFIER));
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_DURATION_TICKS, 0));

        level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                caster.getX(), caster.getY() + 0.1, caster.getZ(), 20, 0.3, 0.05, 0.3, 0.1);
        level.playSound(null, caster.blockPosition(), SoundEvents.SAND_STEP, SoundSource.PLAYERS, 1.0f, 0.7f);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}