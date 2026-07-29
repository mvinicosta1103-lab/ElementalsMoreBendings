package com.elementals.morebendings.bending.airsubbendings.sound;

import com.elementals.morebendings.bending.airsubbendings.common.SoundAbility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sonic Shockwave — libera um pulso de choque sonoro em área ao redor do bender.
 * Empurra e causa dano leve a todas as entidades hostis dentro do raio.
 *
 * NOTA: adapte os nomes/assinaturas de SoundAbility, cooldown() e canUse() para
 * bater exatamente com a classe base Ability do seu projeto — a estrutura aqui
 * segue o mesmo padrão usado por ObsidianPillarAbility no addon Obsidian Wake.
 */
public class SonicShockwaveAbility extends SoundAbility {

    private static final double BASE_RADIUS = 4.0D;
    private static final double BASE_DAMAGE = 3.0D;
    private static final double BASE_KNOCKBACK = 1.2D;
    private static final long BASE_COOLDOWN_MS = 6000L;

    public SonicShockwaveAbility(ServerPlayer bender) {
        super(bender, "sonicShockwave");
    }

    @Override
    public long getCooldown() {
        // Reduzido por sonicShockwaveCooldownI, se o bender tiver o upgrade
        return hasUpgrade("sonicShockwaveCooldownI") ? BASE_COOLDOWN_MS - 1500L : BASE_COOLDOWN_MS;
    }

    @Override
    public boolean execute() {
        ServerPlayer player = getBender();
        if (player == null) {
            return false;
        }

        double radius = hasUpgrade("sonicShockwaveRadiusI") ? BASE_RADIUS + 1.5D : BASE_RADIUS;
        Vec3 center = player.position();

        AABB area = new AABB(
                center.x - radius, center.y - 1.5D, center.z - radius,
                center.x + radius, center.y + 2.5D, center.z + radius
        );

        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 push = target.position().subtract(center).normalize().scale(BASE_KNOCKBACK);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.35D, push.z));
            target.hurtMarked = true;
            target.hurt(damageSource("sonicShockwave"), (float) BASE_DAMAGE);

            if (hasUpgrade("sonicShockwaveDisorientI")) {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
            }
        }

        spawnSoundParticles(center, radius);
        playSound("elementals:ability.sonic_shockwave", 1.4F, 0.8F);
        return true;
    }
}