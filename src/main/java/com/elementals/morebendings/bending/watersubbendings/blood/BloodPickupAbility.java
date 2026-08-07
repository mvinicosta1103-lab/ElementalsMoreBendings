package com.elementals.morebendings.bending.watersubbendings.blood;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "bloodPickup" -- nó enxertado direto na leaf {@code bloodShield} (ramo
 * {@code bloodPush > bloodPushPowerI}), que o {@link BloodMasteryGraft}
 * tinha deixado propositalmente livre "reservada caso um addon futuro
 * libere mais slots" -- exatamente este caso: os 12 keybinds físicos
 * ({@code Ability.MAX_KEYBINDS}) já estavam esgotados (slots 0-3 =
 * habilidades base, 4-11 = as 8 do graft), então Pickup ocupa o slot 11,
 * que estava reservado (mas ainda não implementado) para
 * {@code bloodWideGrasp} -- essa fica pra quando decidirmos de onde tirar
 * um slot pra ela (ver nota em {@link BloodMasteryGraft}).
 * <br><br>
 * Diferente de {@code AbilityBloodControl} (base do mod): aquela é
 * disparada internamente por {@code AbilityBlood1} via
 * {@code shift+segurar 1500ms}, um caminho hardcoded pra instância
 * compilada que este addon não tem como interceptar sem Mixin (nunca
 * usado aqui). Pickup é uma ability nova e independente, com tecla
 * própria, mas com o MESMO espírito de "canal contínuo que puppeteia o
 * alvo" -- só que mais "trancado": o alvo é mantido rente ao ponto de mira
 * do bender (lerp bem mais agressivo que o 0.05 da base, então lift/puxar/
 * andar pra frente-trás-lado emergem naturalmente de como o CASTER se
 * move e olha, sem precisar ler input bruto de WASD do caster), e -- ao
 * contrário da base, que não aplica nenhum efeito no alvo -- Pickup empilha
 * Fraqueza/Lentidão/Fadiga de Mineração pesadas a cada tick enquanto
 * segura, pra aproximar de "imóvel e sem ação" (o motor de abilities só
 * expõe onLeftClick/onRightClick/onMiddleClick/onTick, sem hook pra
 * cancelar ataques/uso de item do lado do alvo -- essa é a aproximação
 * mais forte possível sem tocar em código compilado).
 * <br><br>
 * UPGRADE "bloodPickupSwarm" (filho direto de {@code bloodPickup}, ver
 * {@link BloodMasteryGraft}): passiva, sem ability/keybind própria -- só
 * eleva {@code MAX_TARGETS_BASE} (1) pra {@code MAX_TARGETS_SWARM} (5).
 * Sem o upgrade, Pickup continua exatamente como sempre foi (um único
 * alvo). Com ele comprado, apertar a tecla de Pickup DE NOVO enquanto já
 * está canalizando agarra um alvo ADICIONAL (custo de chi reduzido,
 * {@link #ADDITIONAL_TARGET_CHI_COST}) em vez de reiniciar o canal --
 * até o limite. Os alvos extras se distribuem num pequeno círculo em
 * volta do ponto de mira (plano perpendicular à direção do caster) em vez
 * de empilhar todo mundo no mesmo pixel; o custo de chi por tick agora
 * escala com {@code TICK_CHI_COST_PER_TARGET * alvos.size()}, e clique
 * esquerdo (puxão)/clique direito (soltar) passam a afetar TODOS os alvos
 * seguros de uma vez.
 * <br><br>
 * UPGRADE "bloodFreeGrip" (filho IRMÃO de {@code bloodPickupSwarm}, mesmo
 * pai {@code bloodPickup}): também passiva, sem ability/keybind própria.
 * Muda dois comportamentos de uma vez: (1) clique esquerdo deixa de puxar
 * os alvos pra perto e passa a ARREMESSÁ-los na direção em que o caster
 * está olhando, soltando-os no processo (diferente do puxão, que mantém o
 * canal ativo); (2) o caster deixa de ficar imóvel durante o canal
 * ({@link #shouldImmobilizePlayer} passa a checar o upgrade via
 * {@code Bender.getBender(player)}, já que esse método só recebe o
 * {@code Player}, não o {@code Bender}). Clique direito continua soltando
 * os alvos parados onde estão, com ou sem o upgrade -- a diferença do
 * bloodFreeGrip é só no clique esquerdo e na imobilização.
 * <br><br>
 * Controles (canal começa no onCall e dura até onRightClick, um arremesso
 * via onLeftClick+bloodFreeGrip, ou perda de chi/linha-de-visão de TODOS
 * os alvos):
 * <ul>
 *   <li>Tecla de Pickup de novo (canalizando + bloodPickupSwarm): agarra
 *       mais um alvo, até 5</li>
 *   <li>Clique esquerdo: puxão extra em todos os alvos seguros (ou, com
 *       bloodFreeGrip, arremessa e solta todos de uma vez)</li>
 *   <li>Clique do meio (+ shift pra diminuir): ajusta a distância
 *       compartilhada do ponto de mira, igual à UX da
 *       {@code AbilityBloodControl} base</li>
 *   <li>Clique direito: solta todos os alvos onde estão e remove os
 *       debuffs</li>
 * </ul>
 * O caster fica imóvel enquanto canaliza ({@link #shouldImmobilizePlayer})
 * -- exceto com bloodFreeGrip comprado, que libera o movimento -- mesmo
 * assim continua sendo uma ferramenta cara e que exige concentração, sem
 * o "canUseUpgrade(bloodControlPrecisionI) libera mover" da base.
 */
public class BloodPickupAbility implements Ability {

    private static final double GRAB_RANGE = 20.0;
    private static final float GRAB_CHI_COST = 30.0f;
    /** Custo pra agarrar cada alvo ALÉM do primeiro -- mais barato que o
     * grab inicial pra não punir dobradamente quem já pagou pra abrir o
     * canal. Só é alcançável com bloodPickupSwarm comprado. */
    private static final float ADDITIONAL_TARGET_CHI_COST = 18.0f;
    private static final float TICK_CHI_COST_PER_TARGET = 0.35f;

    private static final int MAX_TARGETS_BASE = 1;
    private static final int MAX_TARGETS_SWARM = 5;

    private static final int DEFAULT_DISTANCE = 4;
    private static final int MIN_DISTANCE = 3;
    private static final int MAX_DISTANCE = 15;
    private static final int DISTANCE_STEP = 2;

    /** Bem mais agressivo que o 0.05 da AbilityBloodControl base -- é o que
     * dá a sensação de "controle real" em vez de "puxão frouxo". */
    private static final float FOLLOW_LERP = 0.32f;
    private static final float YANK_STRENGTH = 1.6f;
    /** Impulso do arremesso de bloodFreeGrip -- bem mais forte que o puxão
     * (YANK_STRENGTH), já que aqui a intenção é jogar o alvo longe, não
     * só aproximá-lo. */
    private static final float THROW_STRENGTH = 2.6f;
    /** Pequeno componente vertical somado ao arremesso, só pra tirar o
     * alvo do chão e dar um arco em vez de um empurrão totalmente reto. */
    private static final float THROW_UPWARD_BOOST = 0.3f;
    /** Raio do círculo de formação em volta do ponto de mira quando há mais
     * de 1 alvo -- evita empilhar todos no mesmo ponto exato. */
    private static final float FORMATION_RADIUS = 1.4f;

    private static final int DEBUFF_REFRESH_TICKS = 20; // 1s, reaplicado a cada tick do canal

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_PICKUP)) {
            bender.setCurrAbility(null);
            return;
        }

        // Já canalizando -- tecla de novo tenta agarrar um alvo ADICIONAL
        // em vez de reiniciar o canal (só rende alguma coisa com
        // bloodPickupSwarm comprado, ver maxTargets()).
        if (bender.getCurrAbility() == this) {
            tryGrabAdditionalTarget(bender, caster, level);
            return;
        }

        LivingEntity target = raycastLivingTarget(caster, level, null);
        if (target == null) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(this, GRAB_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);

        bender.setCurrAbility(this);
        setAbilityData(bender, targets, DEFAULT_DISTANCE);
        target.fallDistance = 0.0f;
        applyControlDebuffs(target);

        playGrabFeedback(level, target);
    }

    private void tryGrabAdditionalTarget(Bender bender, ServerPlayer caster, ServerLevel level) {
        List<LivingEntity> targets = getTargets(bender);
        int max = maxTargets(bender);

        if (targets.size() >= max) {
            caster.displayClientMessage(Component.literal(
                            max >= MAX_TARGETS_SWARM
                                    ? "Você já está segurando o máximo de alvos (5)."
                                    : "Compre bloodPickupSwarm pra segurar mais de um alvo por vez."),
                    true);
            return;
        }

        LivingEntity newTarget = raycastLivingTarget(caster, level, targets);
        if (newTarget == null) {
            // Sem alvo novo à mira -- não gasta chi à toa.
            return;
        }

        if (!bender.reduceChi(this, ADDITIONAL_TARGET_CHI_COST)) {
            return;
        }

        targets.add(newTarget);
        newTarget.fallDistance = 0.0f;
        applyControlDebuffs(newTarget);

        playGrabFeedback(level, newTarget);
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            release(bender);
            return;
        }

        List<LivingEntity> targets = getTargets(bender);
        targets.removeIf(t -> t == null || t.isRemoved() || !t.isAlive());

        if (targets.isEmpty()) {
            release(bender);
            return;
        }

        if (!bender.reduceChi(this, TICK_CHI_COST_PER_TARGET * targets.size())) {
            release(bender);
            return;
        }

        // Qualquer alvo que escapou longe demais (ex: teleporte) solta só
        // ELE -- não derruba o canal inteiro por causa de um único fujão.
        double maxRangeSq = (GRAB_RANGE * 2.0) * (GRAB_RANGE * 2.0);
        Iterator<LivingEntity> escaped = targets.iterator();
        while (escaped.hasNext()) {
            LivingEntity t = escaped.next();
            if (caster.distanceToSqr(t) > maxRangeSq) {
                clearControlDebuffs(t);
                escaped.remove();
            }
        }
        if (targets.isEmpty()) {
            release(bender);
            return;
        }

        Vec3 eye = caster.getEyePosition();
        Vec3 center = eye.add(caster.getLookAngle().scale(getDistance(bender)));

        int count = targets.size();
        for (int i = 0; i < count; i++) {
            LivingEntity target = targets.get(i);
            Vec3 holdPoint = count == 1 ? center : formationOffset(center, caster, i, count);

            Vec3 toHold = holdPoint.subtract(target.position().add(0, target.getBbHeight() * 0.5, 0));
            Vec3 nudge = toHold.scale(FOLLOW_LERP);

            target.setDeltaMovement(nudge);
            target.hurtMarked = true;
            target.move(MoverType.PLAYER, target.getDeltaMovement());
            target.fallDistance = 0.0f;

            if (caster.tickCount % DEBUFF_REFRESH_TICKS == 0) {
                applyControlDebuffs(target);
            }

            level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(0x8B0000).toVector3f(), 0.7f),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    1, 0.15, 0.2, 0.15, 0.0);
        }
    }

    /** Distribui alvos extras num pequeno círculo ao redor do ponto central
     * de mira, num plano perpendicular à direção do caster -- pra não
     * empilhar todo mundo exatamente no mesmo ponto quando há mais de 1. */
    private Vec3 formationOffset(Vec3 center, ServerPlayer caster, int index, int count) {
        Vec3 look = caster.getLookAngle();
        Vec3 arbitraryUp = Math.abs(look.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(arbitraryUp).normalize();
        Vec3 planeUp = right.cross(look).normalize();

        double angle = (2 * Math.PI * index) / count;
        double dx = Math.cos(angle) * FORMATION_RADIUS;
        double dy = Math.sin(angle) * FORMATION_RADIUS;

        return center.add(right.scale(dx)).add(planeUp.scale(dy));
    }

    @Override
    public void onLeftClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        Player player = bender.player;
        List<LivingEntity> targets = getTargets(bender);
        if (targets.isEmpty()) {
            return;
        }

        if (bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_FREE_GRIP)) {
            throwTargets(bender, player, targets);
            return;
        }

        boolean pulledAny = false;
        for (LivingEntity target : targets) {
            if (target.isRemoved()) {
                continue;
            }
            Vec3 pull = player.getEyePosition().subtract(target.position())
                    .normalize().scale(YANK_STRENGTH);
            target.setDeltaMovement(target.getDeltaMovement().add(pull));
            target.hurtMarked = true;
            pulledAny = true;
        }

        if (pulledAny) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.5f, 0.4f);
        }
    }

    /** {@code bloodFreeGrip}: em vez de puxar, arremessa TODOS os alvos
     * segurados na direção em que o caster está olhando e encerra o canal
     * (solta todo mundo de uma vez -- diferente de onRightClick, que
     * também solta mas sem impulso). */
    private void throwTargets(Bender bender, Player player, List<LivingEntity> targets) {
        Vec3 lookDir = player.getLookAngle();
        Vec3 throwVelocity = lookDir.scale(THROW_STRENGTH).add(0, THROW_UPWARD_BOOST, 0);

        boolean threwAny = false;
        for (LivingEntity target : targets) {
            if (target.isRemoved()) {
                continue;
            }
            clearControlDebuffs(target);
            target.setDeltaMovement(throwVelocity);
            target.hurtMarked = true;
            target.fallDistance = 0.0f;
            threwAny = true;
        }

        if (threwAny) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6f, 0.7f);
        }

        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    @Override
    public void onMiddleClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        if (bender.player.isShiftKeyDown()) {
            decrementDistance(bender);
        } else {
            incrementDistance(bender);
        }
    }

    @Override
    public void onRightClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        release(bender);
    }

    @Override
    public boolean shouldImmobilizePlayer(Player player) {
        Bender bender = Bender.getBender(player);
        if (bender != null && bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_FREE_GRIP)) {
            // bloodFreeGrip -- caster livre pra andar enquanto canaliza.
            return false;
        }
        return true;
    }

    @Override
    public void onRemove(Bender bender) {
        release(bender);
    }

    private void release(Bender bender) {
        for (LivingEntity target : getTargets(bender)) {
            if (target != null) {
                clearControlDebuffs(target);
            }
        }
        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    private void playGrabFeedback(ServerLevel level, LivingEntity target) {
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.7f, 0.5f);
        level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(0x8B0000).toVector3f(), 1.3f),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                20, 0.3, 0.4, 0.3, 0.03);
    }

    private void applyControlDebuffs(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEBUFF_REFRESH_TICKS + 5, 3, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEBUFF_REFRESH_TICKS + 5, 4, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, DEBUFF_REFRESH_TICKS + 5, 2, false, true, true));
    }

    private void clearControlDebuffs(LivingEntity target) {
        target.removeEffect(MobEffects.WEAKNESS);
        target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        target.removeEffect(MobEffects.DIG_SLOWDOWN);
    }

    private LivingEntity raycastLivingTarget(ServerPlayer caster, ServerLevel level, List<LivingEntity> exclude) {
        Vec3 eye = caster.getEyePosition();
        Vec3 reach = eye.add(caster.getLookAngle().scale(GRAB_RANGE));

        HitResult blockHit = level.clip(new ClipContext(eye, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double maxDistSq = blockHit.getType() != HitResult.Type.MISS
                ? blockHit.getLocation().distanceToSqr(eye)
                : GRAB_RANGE * GRAB_RANGE;

        LivingEntity closest = null;
        double closestDistSq = maxDistSq;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(GRAB_RANGE),
                e -> e != caster && e.isAlive() && (exclude == null || !exclude.contains(e)))) {
            var clip = candidate.getBoundingBox().inflate(0.3).clip(eye, reach);
            if (clip.isEmpty()) {
                continue;
            }
            double distSq = clip.get().distanceToSqr(eye);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = candidate;
            }
        }
        return closest;
    }

    @SuppressWarnings("unchecked")
    private List<LivingEntity> getTargets(Bender bender) {
        if (bender.abilityData == null) {
            return new ArrayList<>();
        }
        return (List<LivingEntity>) ((Object[]) bender.abilityData)[0];
    }

    private int getDistance(Bender bender) {
        if (bender.abilityData == null) {
            return DEFAULT_DISTANCE;
        }
        return (Integer) ((Object[]) bender.abilityData)[1];
    }

    private int maxTargets(Bender bender) {
        return bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_PICKUP_SWARM)
                ? MAX_TARGETS_SWARM : MAX_TARGETS_BASE;
    }

    private void incrementDistance(Bender bender) {
        setAbilityData(bender, getTargets(bender), Math.min(getDistance(bender) + DISTANCE_STEP, MAX_DISTANCE));
    }

    private void decrementDistance(Bender bender) {
        setAbilityData(bender, getTargets(bender), Math.max(getDistance(bender) - DISTANCE_STEP, MIN_DISTANCE));
    }

    private void setAbilityData(Bender bender, List<LivingEntity> targets, int distance) {
        bender.abilityData = new Object[]{targets, distance};
    }
}