package com.elementals.morebendings.bending.avatarstate.moves;

import dev.saperate.elementals.data.Bender;
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

import java.util.List;

/**
 * "avatarInfernoNova" — primeira ability de Fogo do {@code AvatarElement}.
 * O caster libera uma explosão de fogo em todas as direções ao redor de
 * si -- dano alto + ignição de tudo por perto, sem se machucar. Muito mais
 * forte que qualquer FireBlast normal.
 */
public class AvatarInfernoNovaAbility implements Ability {

    private static final double RADIUS = 8.0;
    private static final float DAMAGE = 10.0f;
    private static final int FIRE_TICKS = 160; // 8s -- usa setRemainingFireTicks, não setSecondsOnFire (1.21.1)
    private static final float CHI_COST = 34.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        bender.setCurrAbility(null);

        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            return;
        }

        AABB area = caster.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity target : nearby) {
            double distance = Math.max(0.5, target.position().distanceTo(caster.position()));
            double falloff = Math.max(0.2, 1.0 - (distance / RADIUS));
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE * (float) falloff);
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), (int) (FIRE_TICKS * falloff)));
            Vec3 push = target.position().subtract(caster.position()).normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(push.x * 0.5, 0.35, push.z * 0.5));
            target.hurtMarked = true;
        }

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, caster.getX(), caster.getY() + 1.0, caster.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.FLAME, caster.getX(), caster.getY() + 1.0, caster.getZ(),
                120, RADIUS * 0.4, 1.0, RADIUS * 0.4, 0.06);
        level.sendParticles(ParticleTypes.LAVA, caster.getX(), caster.getY() + 1.0, caster.getZ(),
                20, RADIUS * 0.3, 0.5, RADIUS * 0.3, 0.0);
        level.playSound(null, caster.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.6f, 1.1f);
        level.playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.4f, 0.8f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}