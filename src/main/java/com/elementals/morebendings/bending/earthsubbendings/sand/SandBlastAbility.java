package com.elementals.morebendings.bending.earthsubbendings.sand;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "sandBlast" — segunda habilidade raiz da árvore de Sand (ver {@link
 * SandElement}), a primeira puramente ofensiva/instantânea da sub-bending
 * (diferente de {@code sandTornado}, que é canalizada).
 *
 * Mesmo esquema de {@code PetrifyingTouchAbility}: raycast de entidade
 * (via {@link SapsUtils#raycastEntity}) na direção mirada; se acertar,
 * causa dano leve e joga um jato de areia/grão na cara do alvo -- {@link
 * MobEffects#BLINDNESS} por alguns segundos, simulando areia nos olhos --
 * além de um pequeno empurrão pra trás. Sem cooldown próprio: o custo de
 * chi já limita o spam, igual {@code crystalShard}/{@code glassShards}.
 *
 * Instantânea: por isso é OBRIGATÓRIO liberar {@code currAbility} de volta
 * pra {@code null} no final de {@link #onCall} e em {@link #onRemove},
 * senão o bender trava nesta ability pra sempre.
 */
public class SandBlastAbility implements Ability {

    private static final double RANGE = 14.0;
    private static final float CHI_COST = 15.0f;
    private static final float DAMAGE = 2.5f;
    private static final int BLINDNESS_DURATION_TICKS = 60; // 3s
    private static final double KNOCKBACK = 0.5;

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
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0));

            Vec3 push = target.position().subtract(player.position()).normalize().scale(KNOCKBACK);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.1, push.z));
            target.hurtMarked = true; // sincroniza o empurrão com o cliente

            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    24, 0.3, 0.35, 0.3, 0.05);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SAND_HIT, SoundSource.PLAYERS, 0.9f, 1.1f);
        } else {
            // Errou o alvo -- feedback mais fraco, mas ainda mostra que a
            // habilidade foi de fato conjurada (mesmo esquema de PetrifyingTouchAbility).
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SAND_HIT, SoundSource.PLAYERS, 0.5f, 0.9f);
        }

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}