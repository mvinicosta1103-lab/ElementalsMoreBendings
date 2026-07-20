package com.example.elementalmorebendings.plant.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * ThornBarrageAbility ("thornBarrage")
 * <p>
 * Ataque em leque: dispara vários "espinhos" instantâneos (hitscan) num
 * cone à frente do jogador, cada um desenhado como uma pequena linha reta
 * de partículas divergindo do centro. Atinge todos os inimigos pegos pelo
 * leque, causa dano e aplica Veneno (Poison) por um tempo curto.
 */
public class ThornBarrageAbility implements Ability {

    private static final float BASE_COST = 20.0f;
    private static final int THORN_COUNT = 7;
    private static final double SPREAD_DEGREES = 26.0;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "thornBarrage", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double range = bender.plrData.canUseUpgrade("thornBarrageRangeI") ? 12.0 : 9.0;
        float damage = bender.plrData.canUseUpgrade("thornBarragePowerI") ? 3.0f : 2.0f;
        int poisonDuration = bender.plrData.canUseUpgrade("thornBarragePowerI") ? 70 : 40;

        ServerLevel world = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).normalize();
        Vec3 realUp = right.cross(look).normalize();

        for (int i = 0; i < THORN_COUNT; i++) {
            // distribui os espinhos num leque: metade pros lados, metade
            // com leve variação vertical, todos ainda apontando pro alvo geral.
            double h = Math.toRadians((i - (THORN_COUNT - 1) / 2.0) * (SPREAD_DEGREES / THORN_COUNT));
            double v = Math.toRadians(((i % 3) - 1) * 4.0);

            Vec3 dir = look
                    .add(right.scale(Math.tan(h)))
                    .add(realUp.scale(Math.tan(v)))
                    .normalize();
            Vec3 end = eye.add(dir.scale(range));

            for (int p = 0; p <= 14; p++) {
                double t = p / 14.0;
                Vec3 point = eye.add(end.subtract(eye).scale(t));
                world.sendParticles(AbilitySupport.VINE_DUST, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
            }

            for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(eye, end).inflate(0.6),
                    e -> e.isAlive() && e != player && e.distanceToSqr(eye) <= range * range)) {
                target.hurt(world.damageSources().playerAttack(player), damage);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, 0));
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY(0.6), target.getZ(), 6, 0.25, 0.25, 0.25, 0.01);
            }
        }

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}
