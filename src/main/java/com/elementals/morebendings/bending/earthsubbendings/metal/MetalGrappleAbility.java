package com.elementals.morebendings.bending.earthsubbendings.metal;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * "metalGrapple" — nó enxertado no fim do ramo {@code metalBind} da árvore
 * REAL de Metal Bending do mod base (ver {@link MetalMasteryGraft}), até
 * agora o único dos 4 ramos-raiz sem nenhuma habilidade deste addon
 * pendurada nele. Instantânea, mesmo esquema de {@code PetrifyingTouchAbility}:
 * OBRIGATÓRIO liberar {@code currAbility} de volta pra {@code null} em todo
 * caminho de saída de {@link #onCall} e em {@link #onRemove}.
 *
 * Diferente de {@link MetalSlamAbility} (que puxa TODO metal-wearer por
 * perto até o caster), esta é o oposto: um gancho magnético que puxa o
 * PRÓPRIO caster até o primeiro alvo válido na mira -- um bloco de metal
 * "de verdade" ({@link #GRAPPLE_BLOCKS}) ou uma criatura viva usando
 * armadura de metal (ver {@link MetalMasteryGraft#isWearingMetal}).
 * Ferramenta de mobilidade/traversal, não de dano -- fecha o tema "Bind"
 * (tether/corrente) que os outros 3 nós enxertados não cobrem.
 */
public class MetalGrappleAbility implements Ability {

    private static final double RANGE = 20.0;
    private static final float CHI_COST = 20.0f;
    private static final double PULL_STRENGTH = 1.6;
    private static final double MIN_PULL_STRENGTH = 0.6;
    /** Cone de detecção de entidade em torno do olhar do caster (produto escalar mínimo). */
    private static final double ENTITY_AIM_TOLERANCE = 0.97;

    private static final Set<Block> GRAPPLE_BLOCKS = Set.of(
            Blocks.IRON_BLOCK, Blocks.RAW_IRON_BLOCK, Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.IRON_BARS,
            Blocks.CHAIN, Blocks.LIGHTNING_ROD, Blocks.HEAVY_CORE
    );

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(MetalMasteryGraft.METAL_GRAPPLE)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 anchor = findAnchor(caster, level);
        if (anchor == null) {
            // Nada de metal na mira -- não gasta chi à toa (mesmo padrão de MetalSlamAbility).
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        pullCasterToward(caster, anchor);

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8f, 1.1f);
        level.sendParticles(ParticleTypes.END_ROD,
                anchor.x, anchor.y, anchor.z, 8, 0.2, 0.2, 0.2, 0.01);

        bender.setCurrAbility(null);
    }

    /**
     * @return o ponto de ancoragem mais próximo na mira do caster (dentro
     * de {@link #RANGE}) -- prioriza uma criatura de metal alinhada com o
     * olhar sobre um bloco de metal, já que criaturas se movem e blocos
     * não. Retorna {@code null} se não houver nenhum alvo válido.
     */
    private Vec3 findAnchor(ServerPlayer caster, ServerLevel level) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();

        LivingEntity entityTarget = findAimedMetalEntity(caster, level, eye, look);
        if (entityTarget != null) {
            return entityTarget.position().add(0, entityTarget.getBbHeight() * 0.5, 0);
        }

        Vec3 reach = eye.add(look.scale(RANGE));
        HitResult hit = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            if (GRAPPLE_BLOCKS.contains(level.getBlockState(blockHit.getBlockPos()).getBlock())) {
                return blockHit.getLocation();
            }
        }
        return null;
    }

    private LivingEntity findAimedMetalEntity(ServerPlayer caster, ServerLevel level, Vec3 eye, Vec3 look) {
        AABB area = caster.getBoundingBox().inflate(RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != caster && e.isAlive() && MetalMasteryGraft.isWearingMetal(e));

        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 toCandidate = candidate.position().subtract(eye);
            double distSq = toCandidate.lengthSqr();
            if (distSq < 0.01) {
                continue;
            }
            Vec3 direction = toCandidate.normalize();
            if (direction.dot(look) >= ENTITY_AIM_TOLERANCE && distSq < bestDistSq) {
                best = candidate;
                bestDistSq = distSq;
            }
        }
        return best;
    }

    private void pullCasterToward(ServerPlayer caster, Vec3 anchor) {
        Vec3 toAnchor = anchor.subtract(caster.position());
        double dist = Math.max(toAnchor.length(), 0.1);
        double strength = Math.max(MIN_PULL_STRENGTH, Math.min(PULL_STRENGTH, dist / 6.0));
        Vec3 pull = toAnchor.scale(strength / dist);

        caster.setDeltaMovement(pull);
        caster.hurtMarked = true; // força sincronizar a nova velocidade pro cliente
        caster.fallDistance = 0.0f; // evita dano de queda ao "aterrissar" depois do gancho
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}