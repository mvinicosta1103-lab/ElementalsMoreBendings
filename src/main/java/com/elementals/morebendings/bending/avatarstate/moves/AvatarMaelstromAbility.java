package com.elementals.morebendings.bending.avatarstate.moves;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * "avatarMaelstrom" — segunda ability de Água do {@code AvatarElement}.
 * Cria um redemoinho gigante no ponto mirado que puxa tudo por perto pro
 * centro (e pra baixo) enquanto causa dano contínuo de afogamento --
 * muito mais amplo que o WaterVortex normal.
 */
public class AvatarMaelstromAbility implements Ability {

    private static final double RANGE = 16.0;
    private static final double RADIUS = 7.0;
    private static final float DAMAGE = 5.0f;
    private static final float CHI_COST = 32.0f;

    private static final DustParticleOptions SWIRL_DUST =
            new DustParticleOptions(new Vector3f(0.10f, 0.35f, 0.85f), 2.2f);

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

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 center = hit.getLocation();

        AABB area = new AABB(center, center).inflate(RADIUS, 4.0, RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);

        for (LivingEntity target : nearby) {
            Vec3 pull = center.subtract(target.position());
            double dist = Math.max(0.5, pull.length());
            Vec3 pullDir = pull.normalize();
            target.setDeltaMovement(target.getDeltaMovement()
                    .add(pullDir.x * 0.5, -0.15, pullDir.z * 0.5));
            target.hurt(level.damageSources().playerAttack(caster), DAMAGE * (float) Math.max(0.3, 1.0 - dist / RADIUS));
            target.hurtMarked = true;
        }

        int arms = 3;
        for (int a = 0; a < arms; a++) {
            for (int r = 1; r <= RADIUS; r++) {
                double angle = (2 * Math.PI * a) / arms + r * 0.7;
                double x = center.x + r * Math.cos(angle);
                double z = center.z + r * Math.sin(angle);
                level.sendParticles(SWIRL_DUST, x, center.y + 0.2, z, 6, 0.15, 0.6, 0.15, 0.0);
            }
        }
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.4f, 0.4f);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}