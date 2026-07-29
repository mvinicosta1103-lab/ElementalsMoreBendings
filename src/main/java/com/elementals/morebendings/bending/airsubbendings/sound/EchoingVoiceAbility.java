package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "echoingVoice" — primeira habilidade raiz da árvore de Sound (ver
 * {@link SoundElement}). Instantânea, igual {@code CurseMinionAbility}:
 * OBRIGATÓRIO liberar {@code currAbility} no final.
 *
 * O jogador solta um grito amplificado na direção que está olhando: um
 * cone de {@link #RANGE} blocos de alcance atinge quem estiver na frente
 * (usando produto escalar entre a direção olhada e a direção até o
 * alvo, não um raycast único, pra pegar vários inimigos de uma vez).
 * Quem for atingido:
 *  - recebe dano de "sound" (dano mágico, ignora armadura, igual outras
 *    abilities baseadas em energia/dessa árvore);
 *  - é empurrado pra longe do caster;
 *  - fica atordoado (Náusea + Lentidão) por um tempo curto, simulando
 *    ficar "surdo" pelo estouro sonoro;
 *  - tem o fogo apagado, se estiver pegando fogo (efeito colateral da
 *    onda de ar/som se propagando).
 */
public class EchoingVoiceAbility implements Ability {

    private static final double RANGE = 8.0;
    private static final double HALF_ANGLE_COS = 0.5; // ~60° de abertura pra cada lado
    private static final float DAMAGE = 5.0f;
    private static final double KNOCKBACK_STRENGTH = 1.1;
    private static final int STUN_DURATION_TICKS = 40; // 2s
    private static final int BASE_COOLDOWN_TICKS = 100; // 5s
    private static final float CAST_CHI_COST = 4.0f;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -100000L);
        if (now - last < BASE_COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 origin = caster.position().add(0, caster.getEyeHeight() * 0.5, 0);

        // Cone de partículas na direção olhada, só pra dar feedback visual.
        for (double d = 1.0; d <= RANGE; d += 1.0) {
            Vec3 point = origin.add(look.scale(d));
            level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.2, 0.2, 0.2, 0.0);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 0.6f, 1.4f);

        if (caster.isOnFire()) {
            caster.clearFire();
        }

        AABB area = caster.getBoundingBox().inflate(RANGE);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        DamageSource soundDamage = level.damageSources().indirectMagic(caster, caster);

        for (LivingEntity target : nearby) {
            Vec3 toTarget = target.position().subtract(origin);
            double distance = toTarget.length();
            if (distance > RANGE || distance <= 0.0001) {
                continue;
            }
            double dot = toTarget.normalize().dot(look);
            if (dot < HALF_ANGLE_COS) {
                continue; // fora do cone
            }

            target.hurt(soundDamage, DAMAGE);
            Vec3 push = toTarget.normalize().scale(KNOCKBACK_STRENGTH);
            target.push(push.x, Math.max(0.15, push.y * 0.3), push.z);
            target.hurtMarked = true;

            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, STUN_DURATION_TICKS, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUN_DURATION_TICKS, 2));

            if (target.isOnFire()) {
                target.clearFire();
            }
        }

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}