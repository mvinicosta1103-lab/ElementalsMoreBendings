package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "crystalShard" — habilidade raiz da árvore de Crystal. Dispara espinhos
 * de cristal na direção que o jogador está olhando, causando dano direto
 * em quem for atingido.
 */
public class CrystalShardAbility implements Ability {

    private static final double RANGE = 8.0;
    private static final double WIDTH = 1.0;
    private static final float DAMAGE = 4.0f; // 2 corações

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

        DamageSource source = level.damageSources().indirectMagic(player, player);
        for (LivingEntity entity : hit) {
            entity.hurt(source, DAMAGE);
        }

        for (int i = 0; i <= 20; i++) {
            Vec3 point = origin.add(look.scale(RANGE * i / 20.0));
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        // Sem estado persistente pra limpar.
    }
}