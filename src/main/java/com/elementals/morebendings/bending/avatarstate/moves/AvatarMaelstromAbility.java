package com.elementals.morebendings.bending.avatarstate.moves;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
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
 * <p>
 * Puxão/dano continuam instantâneos (como antes) -- o redemoinho visual
 * agora de fato GIRA e vai FECHANDO (raio encolhendo) ao longo de vários
 * ticks até sumir no centro, em vez de 3 braços estáticos desenhados de
 * uma vez -- ver {@link AvatarFxScheduler}.
 */
public class AvatarMaelstromAbility implements Ability {

    private static final double RANGE = 16.0;
    private static final double RADIUS = 7.0;
    private static final float DAMAGE = 5.0f;
    private static final float CHI_COST = 32.0f;

    private static final DustParticleOptions SWIRL_DUST =
            new DustParticleOptions(new Vector3f(0.10f, 0.35f, 0.85f), 2.2f);
    private static final DustParticleOptions CORE_DUST =
            new DustParticleOptions(new Vector3f(0.55f, 0.75f, 1.0f), 1.4f);

    private static final int ARMS = 3;
    private static final int SPIN_STEPS = 12; // ticks até o redemoinho fechar de vez

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

        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.4f, 0.4f);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.7f);

        for (int step = 0; step < SPIN_STEPS; step++) {
            final int s = step;
            AvatarFxScheduler.schedule(step, () -> {
                // raio encolhe conforme o passo avança -- o vórtice vai "engolindo" a si mesmo
                double currentRadius = RADIUS * (1.0 - (double) s / SPIN_STEPS);
                double spinOffset = s * 0.9; // gira mais rápido perto do centro que da borda
                for (int a = 0; a < ARMS; a++) {
                    for (double r = 1.0; r <= currentRadius; r += 1.0) {
                        double angle = (2 * Math.PI * a) / ARMS + r * 0.7 + spinOffset;
                        double x = center.x + r * Math.cos(angle);
                        double z = center.z + r * Math.sin(angle);
                        double depth = -0.15 * (1.0 - r / RADIUS); // afunda um pouco perto do centro
                        level.sendParticles(SWIRL_DUST, x, center.y + 0.2 + depth, z, 4, 0.15, 0.4, 0.15, 0.0);
                    }
                }
                if (s % 2 == 0) {
                    level.sendParticles(CORE_DUST, center.x, center.y + 0.1, center.z,
                            3, 0.3, 0.2, 0.3, 0.0);
                }
                if (s == SPIN_STEPS - 1) {
                    // engoliu tudo -- borbulhão final no centro
                    level.sendParticles(CORE_DUST, center.x, center.y + 0.2, center.z,
                            24, 0.4, 0.3, 0.4, 0.05);
                    level.playSound(null, center.x, center.y, center.z,
                            SoundEvents.GENERIC_SWIM, SoundSource.PLAYERS, 1.0f, 1.3f);
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}