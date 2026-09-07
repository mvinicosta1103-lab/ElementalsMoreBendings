package com.elementals.morebendings.bending.watersubbendings.spirit;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Goal injetada em qualquer {@link Mob} amaldiçoado por {@code curseMinion}
 * (ver {@link CurseMinionManager}) enquanto a maldição durar.
 *
 * Existe porque a maioria dos mobs pacíficos (vaca, ovelha, porco, aldeão
 * etc.) e vários neutros NÃO têm goal de ataque nenhuma registrada por
 * padrão -- só dar {@code setTarget(...)} nesses casos não faz eles
 * realmente perseguirem/baterem em ninguém, o que é insuficiente pro que
 * {@code curseMinion} promete (qualquer criatura amaldiçoada vira hostil
 * de verdade, mesmo pacífica/neutra).
 *
 * Por isso essa goal é adicionada com prioridade máxima (0) em TODO mob
 * amaldiçoado, hostil ou não -- ela sempre vence qualquer goal vanilla que
 * compartilhe as flags MOVE/LOOK (ex: {@code MeleeAttackGoal} de um
 * zumbi), então o comportamento de combate fica uniforme e previsível
 * pra qualquer tipo de mob, sem depender do que a IA original dele já
 * tinha. É removida do {@code goalSelector} assim que a maldição acaba
 * (ver {@link CurseMinionManager}), restaurando a IA original.
 *
 * Não usa {@code LivingEntity#doHurtTarget}/{@code Attributes.ATTACK_DAMAGE}
 * diretamente sem checar antes -- vários mobs pacíficos não têm esse
 * atributo registrado (só os que a Mojang define em
 * {@code createLivingAttributes} + o que cada mob adiciona por conta
 * própria), e pedir o valor de um atributo não registrado lança
 * {@code IllegalArgumentException}. Se o atributo não existir, usa
 * {@link #FALLBACK_DAMAGE} como dano fixo.
 */
final class CursedAttackGoal extends Goal {

    private static final double ATTACK_RANGE_SQ = 2.5 * 2.5;
    private static final int ATTACK_COOLDOWN_TICKS = 20; // 1s entre golpes
    private static final float FALLBACK_DAMAGE = 3.0f; // usado quando o mob não tem Attributes.ATTACK_DAMAGE

    private final Mob mob;
    private final double speedModifier;
    private int attackCooldown;

    CursedAttackGoal(Mob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        attackCooldown = 0;
        mob.setAggressive(true);
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }

        mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
        mob.getNavigation().moveTo(target, speedModifier);

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (attackCooldown <= 0 && mob.distanceToSqr(target) <= ATTACK_RANGE_SQ) {
            attackCooldown = ATTACK_COOLDOWN_TICKS;
            performAttack(target);
        }
    }

    private void performAttack(LivingEntity target) {
        mob.swing(InteractionHand.MAIN_HAND);

        float damage = mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                ? (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE)
                : FALLBACK_DAMAGE;

        DamageSource source = mob.damageSources().mobAttack(mob);
        if (target.hurt(source, damage)) {
            double dx = target.getX() - mob.getX();
            double dz = target.getZ() - mob.getZ();
            target.knockback(0.4, -dx, -dz);
        }
    }
}