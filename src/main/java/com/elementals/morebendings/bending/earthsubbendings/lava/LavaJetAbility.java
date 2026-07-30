package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "lavaJet" — segunda habilidade raiz da árvore de Lava (ver {@link
 * LavaElement}), ao lado de {@link LavaPoolAbility}. Enquanto lavaPool é
 * uma habilidade de controle de área (poça que fica no chão), lavaJet é a
 * opção ofensiva direta: dispara um jato de lava em linha reta na direção
 * que o jogador está olhando.
 *
 * Mesmo esquema de hitbox em linha que {@code MudSurgeAbility} usa (AABB
 * alongada entre a origem e o alvo, ao invés de uma entidade de projétil
 * de verdade) — só que aqui, em vez de Lentidão, o efeito é dano de lava
 * de verdade + ignição, igual encostar em lava de verdade encostaria.
 *
 * Instantânea, sem estado próprio: por isso é OBRIGATÓRIO liberar {@code
 * currAbility} de volta pra {@code null} no final de {@link #onCall} e em
 * {@link #onRemove} (mesmo motivo documentado em {@code MudSurgeAbility}).
 */
public class LavaJetAbility implements Ability {

    private static final double RANGE = 7.0;
    private static final double WIDTH = 1.1;
    private static final float CHI_COST = 25.0f;
    private static final float DAMAGE = 5.0f;
    private static final int FIRE_SECONDS = 4;
    /** Empurrão pra longe do caster, na direção do jato. */
    private static final double KNOCKBACK = 0.6;

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

        Vec3 look = player.getLookAngle();
        Vec3 origin = player.position().add(0, player.getBoundingBox().getYsize() * 0.5, 0);
        Vec3 target = origin.add(look.scale(RANGE));

        AABB area = new AABB(origin, target).inflate(WIDTH);
        List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive());

        for (LivingEntity entity : hit) {
            entity.hurt(level.damageSources().lava(), DAMAGE);
            entity.igniteForSeconds(FIRE_SECONDS);
            entity.push(look.x * KNOCKBACK, 0.1, look.z * KNOCKBACK);
        }

        // Trilha de partículas de lava/fogo ao longo do jato, igual a
        // trilha de splash do MudSurge -- só que com visual de lava.
        for (int i = 0; i <= 20; i++) {
            Vec3 point = origin.add(look.scale(RANGE * i / 20.0));
            level.sendParticles(ParticleTypes.LAVA, point.x, point.y, point.z, 2, 0.15, 0.1, 0.15, 0.0);
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 3, 0.15, 0.1, 0.15, 0.01);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}