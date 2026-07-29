package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "discordantScream" — mira num único alvo na linha de visão e libera um
 * grito dissonante que aplica Fraqueza + Lentidão, simulando atordoamento
 * sonoro. Sem dano direto -- habilidade de controle/utilidade.
 *
 *  - discordantScreamRangeI    -> +4.0 de alcance
 *  - discordantScreamDurationI -> dobra a duração dos efeitos
 */
public class DiscordantScreamAbility implements Ability {

    private static final double BASE_RANGE = 10.0;
    private static final double CONE_COS = 0.97; // ~14° de abertura -- precisa mirar quase direto no alvo
    private static final int BASE_COOLDOWN_TICKS = 140; // 7s
    private static final float CAST_CHI_COST = 3.5f;

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

        double range = SoundElement.hasUpgrade(caster, SoundElement.DISCORDANT_SCREAM_RANGE_I)
                ? BASE_RANGE + 4.0 : BASE_RANGE;
        Vec3 origin = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();

        LivingEntity target = findTargetInLine(caster, level, origin, look, range);
        if (target == null) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        int duration = SoundElement.hasUpgrade(caster, SoundElement.DISCORDANT_SCREAM_DURATION_I) ? 120 : 60;
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1));

        level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.5f, 0.6f);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    /**
     * Acha o alvo vivo mais próximo dentro de um cone estreito à frente do
     * caster, exigindo linha de visão desobstruída (sem bloco no meio).
     * Substitui o placeholder que sempre retornava null.
     */
    private LivingEntity findTargetInLine(ServerPlayer caster, ServerLevel level, Vec3 origin, Vec3 look, double range) {
        AABB area = caster.getBoundingBox().inflate(range);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        LivingEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (LivingEntity candidate : nearby) {
            Vec3 toTarget = candidate.getEyePosition().subtract(origin);
            double distSq = toTarget.lengthSqr();
            if (distSq > range * range || distSq <= 0.0001) {
                continue;
            }
            if (toTarget.normalize().dot(look) < CONE_COS) {
                continue; // fora do cone estreito
            }

            HitResult obstruction = level.clip(new ClipContext(origin, candidate.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            if (obstruction.getType() != HitResult.Type.MISS) {
                continue; // tem parede no meio
            }

            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = candidate;
            }
        }
        return closest;
    }
}
