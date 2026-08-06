package com.elementals.morebendings.bending.firesubbendings.lightning;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "thunderStep" — nó enxertado no fim do leaf {@code lightningEMPSizeI},
 * sub-ramo {@code lightningEMP} DENTRO de {@code lightningVoltArc} da
 * árvore REAL de Lightning Bending do mod base (ver {@link
 * LightningMasteryGraft}). Instantânea, mesmo esquema de {@code
 * PetrifyingTouchAbility}: OBRIGATÓRIO liberar {@code currAbility} de volta
 * pra {@code null} em todo caminho de saída de {@link #onCall} e em {@link
 * #onRemove}.
 * <br><br>
 * Mobilidade pura: o caster se teleporta {@link #MAX_DISTANCE} blocos na
 * direção que está olhando (parando antes de qualquer bloco sólido no
 * caminho), deixando um rastro de faíscas -- qualquer criatura viva no
 * trajeto leva um pequeno dano de choque. Cobre o tema "mobilidade
 * instantânea" que nenhuma habilidade base de Lightning cobre.
 */
public class ThunderStepAbility implements Ability {

    private static final double MAX_DISTANCE = 14.0;
    private static final double TRAIL_HIT_RADIUS = 1.5;
    private static final float CHI_COST = 22.0f;
    private static final float TRAIL_DAMAGE = 2.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(LightningMasteryGraft.THUNDER_STEP)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 start = caster.position();
        Vec3 destination = findDestination(caster, level);

        strikeTrail(level, caster, start, destination);
        teleport(caster, destination);

        bender.setCurrAbility(null);
    }

    /**
     * @return o ponto de destino do passo -- {@link #MAX_DISTANCE} blocos
     * na direção do olhar, encurtado se houver um bloco sólido no caminho
     * (raycast, mesmo padrão de {@code MetalGrappleAbility#findAnchor}).
     */
    private Vec3 findDestination(ServerPlayer caster, ServerLevel level) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 reach = eye.add(look.scale(MAX_DISTANCE));

        HitResult hit = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 target = hit.getType() != HitResult.Type.MISS
                ? hit.getLocation().subtract(look.scale(0.5)) // não crava dentro do bloco
                : reach;

        return new Vec3(target.x, caster.position().y + (target.y - eye.y), target.z);
    }

    private void strikeTrail(ServerLevel level, ServerPlayer caster, Vec3 start, Vec3 end) {
        AABB trailArea = new AABB(start, end).inflate(TRAIL_HIT_RADIUS);
        List<LivingEntity> hitAlongTrail = level.getEntitiesOfClass(LivingEntity.class, trailArea,
                e -> e != caster && e.isAlive());

        for (LivingEntity victim : hitAlongTrail) {
            victim.hurt(level.damageSources().playerAttack(caster), TRAIL_DAMAGE);
        }

        double steps = Math.max(4, start.distanceTo(end) / 1.5);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.lerp(end, i / steps);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    point.x, point.y + 1.0, point.z, 3, 0.15, 0.15, 0.15, 0.01);
        }
    }

    private void teleport(ServerPlayer caster, Vec3 destination) {
        caster.teleportTo(destination.x, destination.y, destination.z);
        caster.fallDistance = 0.0f;
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}