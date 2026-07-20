package com.example.obsidianwake.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * MoltenGripAbility ("moltenGrip")
 * Grabs the nearest entity in front of the player with a tendril of magma
 * and yanks it toward the caster, dealing minor damage and a brief burn.
 * <p>
 * Visualmente, um chicote de partículas de lava é desenhado entre o jogador
 * e o alvo no instante do agarrão, pra deixar claro o que está acontecendo
 * (a ação inteira acontece num único tick, então o efeito é um "flash" da
 * trilha inteira de uma vez, em vez de uma entidade animada ao longo do
 * tempo).
 */
public class MoltenGripAbility implements Ability {

    private static final float BASE_COST = 12.0f;

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "moltenGrip", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        double range = bender.plrData.canUseUpgrade("moltenGripRangeII") ? 12.0
                : (bender.plrData.canUseUpgrade("moltenGripRangeI") ? 9.0 : 6.0);
        float damage = bender.plrData.canUseUpgrade("moltenGripPowerI") ? 4.0f : 2.0f;

        List<LivingEntity> candidates = AbilitySupport.entitiesInFront(player, range, 1.5);
        LivingEntity target = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (target != null) {
            Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.6));
            Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);
            spawnMagmaTendril(player.serverLevel(), start, end);

            AbilitySupport.pullToward(player.position(), target, 1.6, 0.3);
            target.hurt(player.serverLevel().damageSources().magic(), damage);
            target.igniteForSeconds(2.0f);
        }

        bender.setCurrAbility(null);
    }

    /**
     * Desenha uma trilha de partículas em forma de tendril (com uma leve
     * ondulação, tipo chicote, em vez de uma linha reta) do ponto start até
     * end, com um estouro de impacto maior na ponta.
     */
    private static void spawnMagmaTendril(ServerLevel world, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        if (length < 0.01) return;
        Vec3 dir = diff.scale(1.0 / length);

        // vetor perpendicular (aproximadamente horizontal) pra dar a ondulação do chicote
        Vec3 perp = new Vec3(-dir.z, 0, dir.x);
        if (perp.lengthSqr() < 1.0E-4) perp = new Vec3(1, 0, 0);
        perp = perp.normalize();

        int segments = Math.max(12, (int) (length * 3));
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            // ondulação senoidal que diminui perto das pontas, como uma chicotada
            double wobble = Math.sin(t * Math.PI * 3.0) * (1.0 - t) * 0.35;
            Vec3 point = start.add(dir.scale(length * t)).add(perp.scale(wobble));

            world.sendParticles(ParticleTypes.LAVA, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
            world.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 2, 0.06, 0.06, 0.06, 0.01);
            if (i % 3 == 0) {
                world.sendParticles(ParticleTypes.DRIPPING_LAVA, point.x, point.y, point.z, 1, 0.04, 0.04, 0.04, 0.0);
            }
        }

        // estouro de impacto no alvo
        world.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 12, 0.3, 0.3, 0.3, 0.06);
        world.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 16, 0.25, 0.25, 0.25, 0.03);

        world.playSound(null, BlockPos.containing(start), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 0.7f);
        world.playSound(null, BlockPos.containing(end), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0f, 1.1f);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}