package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "mudSurge" — habilidade raiz (e única, por enquanto) da árvore de Mud.
 * Implementa {@link Ability} de verdade, então pode ser vinculada a uma
 * tecla via {@code Element.addAbility} / {@code Bender.bindAbility}, igual
 * qualquer outra habilidade do mod base.
 *
 * Efeito: dispara uma onda de lama na direção que o jogador está olhando;
 * qualquer entidade viva atingida recebe Lentidão por um tempo curto.
 */
public class MudSurgeAbility implements Ability {

    private static final double RANGE = 6.0;
    private static final double WIDTH = 1.5;
    private static final int SLOWNESS_DURATION_TICKS = 60; // 3s
    private static final int SLOWNESS_AMPLIFIER = 2; // Lentidão III

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 origin = player.position().add(0, player.getBoundingBox().getYsize() * 0.5, 0);
        Vec3 target = origin.add(look.scale(RANGE));

        AABB area = new AABB(origin, target).inflate(WIDTH);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive());

        for (LivingEntity entity : hit) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER));
        }

        for (int i = 0; i <= 20; i++) {
            Vec3 point = origin.add(look.scale(RANGE * i / 20.0));
            level.sendParticles(ParticleTypes.SPLASH, point.x, point.y, point.z, 3, 0.2, 0.1, 0.2, 0.01);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        // Sem estado persistente pra limpar.
    }
}