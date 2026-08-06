package com.elementals.morebendings.bending.earthsubbendings.glass;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Trata o golpe que estilhaça a {@code glassArmor} ativa (ver {@link
 * GlassArmorAbility}). Mesmo esquema (registro em {@code
 * LivingIncomingDamageEvent}) de {@code LavaArmorCombatHandler}/{@code
 * PlasmaBoostCombatHandler}, mas mexendo no dano recebido em vez de só
 * retaliar: aqui o jogador com a couraça ativa é a VÍTIMA, o golpe que
 * chega é reduzido em {@link GlassArmorAbility#ABSORB_FRACTION} e a
 * couraça se desliga logo em seguida -- é descartável, um único golpe por
 * cast.
 *
 * Se o bender já tiver comprado {@code glassArmorShatterI} (ver {@link
 * GlassElement#GLASS_ARMOR_SHATTER_I}), os cacos voam de volta: quem
 * desferiu o golpe leva um empurrão de dano extra + Lentidão curta,
 * simulando estilhaços cravados.
 */
public final class GlassArmorCombatHandler {

    private static final float SHATTER_RETALIATION_DAMAGE = 2.0f;
    private static final int SHATTER_SLOWNESS_DURATION_TICKS = 60; // 3s
    private static final int SHATTER_SLOWNESS_AMPLIFIER = 1; // Lentidão II

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!GlassArmorAbility.isActive(victim.getUUID())) return;
        if (!(victim.level() instanceof ServerLevel level)) return;

        // Estilhaça imediatamente -- é um único uso por cast, não um escudo contínuo.
        GlassArmorAbility.shatter(victim.getUUID());

        event.setAmount(event.getAmount() * (1.0f - GlassArmorAbility.ABSORB_FRACTION));

        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.1f);
        level.sendParticles(ParticleTypes.CRIT,
                victim.getX(), victim.getY() + 1.0, victim.getZ(), 18, 0.35, 0.5, 0.35, 0.05);

        DamageSource source = event.getSource();
        if (GlassElement.hasUpgrade(victim, GlassElement.GLASS_ARMOR_SHATTER_I)
                && source.getEntity() instanceof LivingEntity attacker
                && attacker != victim) {
            attacker.hurt(victim.damageSources().playerAttack(victim), SHATTER_RETALIATION_DAMAGE);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SHATTER_SLOWNESS_DURATION_TICKS, SHATTER_SLOWNESS_AMPLIFIER));
            attacker.hurtMarked = true;
        }
    }

    private GlassArmorCombatHandler() {
    }
}