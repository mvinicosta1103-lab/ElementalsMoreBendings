package com.elementals.morebendings.bending.earthsubbendings.bone;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/**
 * "bonePuppeteer" — segunda habilidade raiz da árvore de Bone (ver
 * {@link BoneElement}). Instantânea, igual {@code CurseMinionAbility}:
 * OBRIGATÓRIO liberar {@code currAbility} no final.
 *
 * Mira (raycast via {@link SapsUtils#raycastEntity}) uma criatura viva na
 * direção olhada, até {@link #RANGE} blocos:
 *
 *  - Se for um {@link Mob} morto-vivo de verdade ({@link
 *    EntityTypeTags#UNDEAD} -- Skeleton, Stray, Wither Skeleton, Zombie,
 *    Husk, Drowned, Zombie Villager, etc.), o controle é literal: a
 *    criatura vira um fantoche por {@link BonePuppeteerManager#DURATION_TICKS},
 *    andando na direção que o CASTER estiver olhando (mesma ideia de
 *    {@code BoneControlAbility} com a própria farpa, só que numa entidade
 *    de verdade) e sem IA própria enquanto durar. Ver {@link
 *    BonePuppeteerManager}.
 *  - Se for um {@link Player} (ou qualquer outra criatura viva que não seja
 *    morto-vivo), controle de movimento de verdade não é possível --
 *    {@link Player} não tem IA de alvo pra sequestrar (mesma limitação que
 *    {@code CurseMinionAbility} já documenta pra {@code Mob#setTarget}), e
 *    não faz sentido "puppeteering" em algo que não é feito de ossos soltos.
 *    Em vez disso, os ossos são "travados" por um instante: Lentidão e
 *    Fraqueza pesadas, simulando perder o controle do próprio esqueleto sem
 *    de fato sequestrar o jogador.
 */
public class BonePuppeteerAbility implements Ability {

    private static final double RANGE = 10.0;
    private static final float CAST_CHI_COST = 15.0f;

    private static final int BONE_LOCK_DURATION_TICKS = 60; // 3s
    private static final int BONE_LOCK_SLOW_AMPLIFIER = 3;
    private static final int BONE_LOCK_WEAKNESS_AMPLIFIER = 2;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        EntityHitResult hit = SapsUtils.raycastEntity(player, RANGE,
                entity -> entity instanceof LivingEntity living && living != player && living.isAlive());

        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            caster.displayClientMessage(Component.literal("Nenhum alvo encontrado."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        if (target instanceof Mob mob && mob.getType().is(EntityTypeTags.UNDEAD)) {
            BonePuppeteerManager.possess(level, caster, mob);
        } else {
            boneLock(level, target);
        }

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    /** Trava os ossos de quem não é morto-vivo (players e mobs vivos) -- não
     * é controle de movimento de verdade, só um debuff pesado e temporário. */
    private void boneLock(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                BONE_LOCK_DURATION_TICKS, BONE_LOCK_SLOW_AMPLIFIER));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                BONE_LOCK_DURATION_TICKS, BONE_LOCK_WEAKNESS_AMPLIFIER));

        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                20, 0.3, 0.4, 0.3, 0.1);
        level.playSound(null, target.blockPosition(), SoundEvents.BONE_BLOCK_BREAK,
                SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}