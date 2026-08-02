package com.elementals.morebendings.bending.firesubbendings.combustion;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.ElementalConfig;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "combustionVent" — a habilidade MENOR de {@link CombustionElement}.
 * Contraste deliberado com {@link CombustionExplosionAbility}: aqui não
 * tem mira, não tem canalização e não tem nenhum risco de autodano -- o
 * bender só libera uma baforada de calor controlada ao redor do próprio
 * corpo. Empurra tudo que está perto pra longe (com um dano de área baixo)
 * e dá um recuo pro próprio caster na direção contrária à mira, uma
 * mobilidade de escape rápida.
 *
 * Instantânea, sem canalizar -- mesmo esquema de {@code GasCloudAbility}.
 */
public class CombustionVentAbility implements Ability {

    private static final double RADIUS = 3.5;
    private static final float DAMAGE = 2.5f;
    private static final double KNOCKBACK_STRENGTH = 0.55;
    private static final double SELF_RECOIL_STRENGTH = 0.5;

    private static final float CAST_CHI_COST = 8.0f;
    private static final int COOLDOWN_TICKS = 60; // 3s

    /** Cooldown por jogador, só em memória -- mesmo esquema de PlasmaCooldown. Único cooldown que sobrou em Combustion, já que o Blast não tem mais nenhum. */
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
        if (now - last < COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        Vec3 center = caster.position().add(0, caster.getBbHeight() * 0.5, 0);

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.7f);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
                24, RADIUS * 0.4, 0.5, RADIUS * 0.4, 0.08);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                12, RADIUS * 0.3, 0.3, RADIUS * 0.3, 0.05);

        AABB area = caster.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());
        for (LivingEntity entity : nearby) {
            float dmg = DAMAGE * ElementalConfig.get().BENDING_DAMAGE_MULTIPLIER;
            entity.hurt(level.damageSources().playerAttack(caster), dmg);

            Vec3 push = entity.position().subtract(caster.position());
            push = push.length() < 0.01 ? new Vec3(0, 1, 0) : push.normalize();
            entity.push(push.x * KNOCKBACK_STRENGTH, Math.max(push.y * KNOCKBACK_STRENGTH, 0.25), push.z * KNOCKBACK_STRENGTH);
            entity.hurtMarked = true;
        }

        // Recuo do próprio caster -- empurra na direção contrária à mira,
        // um pequeno "boost" de mobilidade além do dano/knockback em área.
        Vec3 look = caster.getLookAngle();
        caster.setDeltaMovement(caster.getDeltaMovement().subtract(look.x * SELF_RECOIL_STRENGTH, 0, look.z * SELF_RECOIL_STRENGTH)
                .add(0, 0.15, 0));
        caster.hurtMarked = true;
        caster.resetFallDistance();

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}