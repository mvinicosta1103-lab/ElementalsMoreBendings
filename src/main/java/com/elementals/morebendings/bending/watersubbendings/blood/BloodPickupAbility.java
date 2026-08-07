package com.elementals.morebendings.bending.watersubbendings.blood;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
 * A IA do alvo (pathfinding/goal selector) é desligada enquanto ele está
 * agarrado ({@code Mob#setNoAi}) -- sem isso, mobs recalculam a própria
 * movimentação todo tick e brigam com o controle do caster, fazendo o
 * puxão parecer travado em vez de seguir a mira. A IA é restaurada em
 * todo caminho que solta o alvo (onRightClick, arremesso, perda de
 * chi/linha-de-visão, onRemove). O reposicionamento em si usa
 * {@code setPos} direto (em vez de {@code setDeltaMovement}+{@code move})
 * pelo mesmo motivo -- é imune a qualquer física/IA residual.
 * <br><br>
 * UPGRADE "bloodFreeGrip" (filho de {@code bloodPickup}, ver
 * {@link BloodMasteryGraft}): passivo, sem ability/keybind própria. Muda
 * dois comportamentos de uma vez: (1) clique esquerdo deixa de puxar o
 * alvo pra perto e passa a ARREMESSÁ-lo na direção em que o caster está
 * olhando, soltando-o no processo (diferente do puxão, que mantém o canal
 * ativo); (2) o caster deixa de ficar imóvel durante o canal
 * ({@link #shouldImmobilizePlayer} passa a checar o upgrade via
 * {@code Bender.getBender(player)}, já que esse método só recebe o
 * {@code Player}, não o {@code Bender}). Clique direito continua soltando
 * o alvo parado onde está, com ou sem o upgrade -- a diferença do
 * bloodFreeGrip é só no clique esquerdo e na imobilização.
 * <br><br>
 * Controles (canal começa no onCall e dura até onRightClick, um arremesso
 * via onLeftClick+bloodFreeGrip, ou perda de chi/linha-de-visão):
 * <ul>
 *   <li>Clique esquerdo: puxão extra (impulso forte na direção do
 *       caster, além do lerp contínuo) -- ou, com bloodFreeGrip,
 *       arremessa o alvo e encerra o canal</li>
 *   <li>Clique do meio (+ shift pra diminuir): ajusta a distância de
 *       controle, igual à UX da {@code AbilityBloodControl} base</li>
 *   <li>Clique direito: solta o alvo e remove os debuffs</li>
 * </ul>
 * O caster fica imóvel enquanto canaliza ({@link #shouldImmobilizePlayer})
 * -- exceto com bloodFreeGrip comprado, que libera o movimento -- mesmo
 * assim continua sendo uma ferramenta cara e que exige concentração, sem
 * o "canUseUpgrade(bloodControlPrecisionI) libera mover" da base.
 * <br><br>
 * NOTA: suporte a múltiplos alvos simultâneos (bloodPickupSwarm) foi
 * removido por ora -- ver histórico do arquivo -- pra manter o pickup de
 * alvo único 100% estável primeiro. Fica pra uma iteração futura.
 */
public class BloodPickupAbility implements Ability {

    private static final double GRAB_RANGE = 20.0;
    private static final float GRAB_CHI_COST = 30.0f;
    private static final float TICK_CHI_COST = 0.35f;

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

        LivingEntity target = raycastLivingTarget(caster, level);
        if (target == null) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(this, GRAB_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        bender.setCurrAbility(this);
        setAbilityData(bender, target, DEFAULT_DISTANCE);
        target.fallDistance = 0.0f;
        applyControlDebuffs(target);
        disableAi(target);

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.7f, 0.5f);
        level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(0x8B0000).toVector3f(), 1.3f),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                20, 0.3, 0.4, 0.3, 0.03);
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            release(bender, getVictim(bender));
            return;
        }

        LivingEntity target = getVictim(bender);
        if (target == null || target.isRemoved() || !target.isAlive()) {
            release(bender, null);
            return;
        }

        if (!bender.reduceChi(this, TICK_CHI_COST)) {
            release(bender, target);
            return;
        }

        if (caster.distanceToSqr(target) > (GRAB_RANGE * 2.0) * (GRAB_RANGE * 2.0)) {
            // alvo escapou longe demais (ex: teleporte) -- solta em vez de arrastar do nada.
            release(bender, target);
            return;
        }

        Vec3 eye = caster.getEyePosition();
        Vec3 holdPoint = eye.add(caster.getLookAngle().scale(getDistance(bender)));

        Vec3 currentCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 nudge = holdPoint.subtract(currentCenter).scale(FOLLOW_LERP);
        Vec3 newCenter = currentCenter.add(nudge);

        // setPos em vez de setDeltaMovement()+move(): mobs com IA
        // recalculam a própria movimentação em aiStep()/travel() a cada
        // tick e sobrescrevem qualquer velocidade forçada de fora -- é
        // por isso que o alvo parava de seguir a mira. A IA já vem
        // desligada desde o grab (disableAi), e mesmo assim reposicionamos
        // direto por garantia: o alvo sempre anda pro holdPoint.
        target.setPos(newCenter.x, newCenter.y - target.getBbHeight() * 0.5, newCenter.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.fallDistance = 0.0f;

        if (caster.tickCount % DEBUFF_REFRESH_TICKS == 0) {
            applyControlDebuffs(target);
        }

        level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(0x8B0000).toVector3f(), 0.7f),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                1, 0.15, 0.2, 0.15, 0.0);
    }

    @Override
    public void onLeftClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        Player player = bender.player;
        LivingEntity target = getVictim(bender);
        if (target == null || target.isRemoved()) {
            return;
        }

        if (bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_FREE_GRIP)) {
            throwTarget(bender, player, target);
            return;
        }

        Vec3 pull = player.getEyePosition().subtract(target.position())
                .normalize().scale(YANK_STRENGTH);
        target.setDeltaMovement(target.getDeltaMovement().add(pull));
        target.hurtMarked = true;

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.5f, 0.4f);
    }

    /** {@code bloodFreeGrip}: em vez de puxar, arremessa o alvo na direção
     * em que o caster está olhando e encerra o canal (solta -- diferente
     * de onRightClick, que também solta mas sem impulso). */
    private void throwTarget(Bender bender, Player player, LivingEntity target) {
        Vec3 lookDir = player.getLookAngle();
        Vec3 throwVelocity = lookDir.scale(THROW_STRENGTH).add(0, THROW_UPWARD_BOOST, 0);

        clearControlDebuffs(target);
        restoreAi(target);
        target.setDeltaMovement(throwVelocity);
        target.hurtMarked = true;
        target.fallDistance = 0.0f;

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6f, 0.7f);

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
        release(bender, getVictim(bender));
    }

    @Override
    public boolean shouldImmobilizePlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        Bender bender = Bender.getBender(serverPlayer);
        if (bender != null && bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_FREE_GRIP)) {
            // bloodFreeGrip -- caster livre pra andar enquanto canaliza.
            return false;
        }
        return true;
    }

    @Override
    public void onRemove(Bender bender) {
        release(bender, getVictim(bender));
    }

    private void release(Bender bender, LivingEntity target) {
        if (target != null) {
            clearControlDebuffs(target);
            restoreAi(target);
        }
        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    /** Desliga a IA (pathfinding/goal selector) do alvo enquanto ele está
     * agarrado -- sem isso, mobs com IA recalculam a própria movimentação
     * todo tick e brigam com o controle do caster. Não afeta
     * {@code Player}, que não tem {@code setNoAi}. */
    private void disableAi(LivingEntity target) {
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
        }
    }

    private void restoreAi(LivingEntity target) {
        if (target instanceof Mob mob) {
            mob.setNoAi(false);
        }
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

    private LivingEntity raycastLivingTarget(ServerPlayer caster, ServerLevel level) {
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
                caster.getBoundingBox().inflate(GRAB_RANGE), e -> e != caster && e.isAlive())) {
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

    private LivingEntity getVictim(Bender bender) {
        if (bender.abilityData == null) {
            return null;
        }
        return (LivingEntity) ((Object[]) bender.abilityData)[0];
    }

    private int getDistance(Bender bender) {
        if (bender.abilityData == null) {
            return DEFAULT_DISTANCE;
        }
        return (Integer) ((Object[]) bender.abilityData)[1];
    }

    private void incrementDistance(Bender bender) {
        setAbilityData(bender, getVictim(bender), Math.min(getDistance(bender) + DISTANCE_STEP, MAX_DISTANCE));
    }

    private void decrementDistance(Bender bender) {
        setAbilityData(bender, getVictim(bender), Math.max(getDistance(bender) - DISTANCE_STEP, MIN_DISTANCE));
    }

    private void setAbilityData(Bender bender, LivingEntity victim, int distance) {
        bender.abilityData = new Object[]{victim, distance};
    }
}