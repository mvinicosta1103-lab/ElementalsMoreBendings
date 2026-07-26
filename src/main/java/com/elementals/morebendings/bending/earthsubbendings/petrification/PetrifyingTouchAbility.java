package com.elementals.morebendings.bending.earthsubbendings.petrification;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.effects.ElementalsStatusEffects;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;

/**
 * "petrifyingTouch" — habilidade ofensiva raiz da árvore de Petrification
 * (ver {@link PetrificationElement}). É um golpe de curto alcance ("toque"):
 * se acertar uma entidade viva na mira, causa um dano leve e a deixa
 * {@code stunned} — efeito de verdade do mod base ({@link
 * ElementalsStatusEffects#STUNNED}) que trava o movimento da vítima (via
 * {@code makeStuckInBlock}) sem impedir que ela ainda ataque/reaja, igual a
 * uma pessoa que teve as pernas/tronco petrificados mas ainda pode se
 * debater.
 *
 * Instantânea, no mesmo esquema de {@code MudSurgeAbility}/{@code
 * CrystalShardAbility}: por isso é OBRIGATÓRIO liberar {@code
 * currAbility} de volta pra {@code null} no final de {@link #onCall} e em
 * {@link #onRemove}, senão o bender trava nesta ability pra sempre.
 */
public class PetrifyingTouchAbility implements Ability {

    private static final double RANGE = 4.5;
    private static final float CHI_COST = 20.0f;
    private static final float DAMAGE = 2.0f;
    private static final int STUN_DURATION_TICKS = 40; // 2s
    private static final int STUN_AMPLIFIER = 0;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        EntityHitResult hit = SapsUtils.raycastEntity(player, RANGE,
                entity -> entity instanceof LivingEntity && entity != player);

        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);
            target.addEffect(new MobEffectInstance(ElementalsStatusEffects.STUNNED.get(),
                    STUN_DURATION_TICKS, STUN_AMPLIFIER));

            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.STONE_HIT, SoundSource.PLAYERS, 0.8f, 0.6f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    16, 0.3, 0.4, 0.3, 0.0);
        } else {
            // Errou o alvo -- som mais fraco, só pra dar feedback de que a
            // habilidade foi usada mesmo sem acertar nada.
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.STONE_HIT, SoundSource.PLAYERS, 0.4f, 0.8f);
        }

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}