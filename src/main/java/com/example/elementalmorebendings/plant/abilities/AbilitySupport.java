package com.example.elementalmorebendings.plant.abilities;

import dev.saperate.elementals.Elementals;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Utilitários compartilhados pelas habilidades deste addon. Só usa a API
 * pública do Elementals (Bender/Element/Ability/Upgrade) — não depende de
 * nenhuma classe interna do addon Elementals Subbending, já que elas não
 * são acessíveis daqui (mesma abordagem do ObsidianWake).
 */
final class AbilitySupport {
    private AbilitySupport() {}

    /** Cor "verde vinha" usada nas partículas de poeira (dust) das habilidades novas. */
    static final DustParticleOptions VINE_DUST =
            new DustParticleOptions(new Vector3f(0.20f, 0.55f, 0.16f), 1.6f);
    static final DustParticleOptions LEAF_DUST =
            new DustParticleOptions(new Vector3f(0.35f, 0.70f, 0.22f), 1.2f);

    static boolean isUnlocked(Bender bender, String upgradeName) {
        Element element = Element.getElement("Plant");
        return element != null && "Plant".equals(element.getName())
                && bender.hasElement(element)
                && bender.plrData.canUseUpgrade(upgradeName);
    }

    static boolean spendUnlocked(Bender bender, String upgradeName, float cost) {
        return isUnlocked(bender, upgradeName) && bender.reduceChi(cost);
    }

    static boolean canDamageBlocks(ServerLevel world) {
        try {
            return world.getGameRules().getBoolean(Elementals.BENDING_GRIEFING);
        } catch (LinkageError | RuntimeException ignored) {
            return world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        }
    }

    static List<LivingEntity> entitiesInFront(Player player, double range, double width) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        AABB box = new AABB(eye, end).inflate(width);
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && e.distanceToSqr(eye) <= range * range);
    }

    static List<LivingEntity> entitiesAround(Player player, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && e.distanceToSqr(center) <= radius * radius);
    }

    static void pushAway(Vec3 origin, LivingEntity target, double strength, double lift) {
        Vec3 direction = target.position().subtract(origin).normalize();
        if (direction.lengthSqr() < 0.01) direction = new Vec3(0, 0, 1);
        target.push(direction.x * strength, lift, direction.z * strength);
        target.hurtMarked = true;
    }

    /**
     * Desenha um ARCO curvo de partículas verdes entre {@code start} e
     * {@code end}, com uma "barriga" (bulge) lateral de altura
     * {@code bulgeHeight} — é o mesmo tipo de curva usada pela Water Arc do
     * Elementals base (um traço curvo, não uma linha reta), só que aqui com
     * partículas de folha/vinha em vez de água. Isso é o que deixa o efeito
     * visualmente muito mais perceptível do que a antiga spawnLine reta.
     *
     * @param sideways vetor lateral (perpendicular ao traço) usado pra
     *                 inclinar a barriga do arco pra um dos lados.
     */
    static void spawnArc(ServerLevel world, Vec3 start, Vec3 end, Vec3 sideways,
                         double bulgeHeight, int points) {
        Vec3 chord = end.subtract(start);
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            // curva parabólica: máxima "barriga" em t=0.5, zero nas pontas
            double bulge = 4.0 * bulgeHeight * t * (1.0 - t);
            Vec3 point = start.add(chord.scale(t)).add(sideways.scale(bulge));

            world.sendParticles(VINE_DUST, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
            if (i % 2 == 0) {
                world.sendParticles(LEAF_DUST, point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    /** Anel de partículas verdes (usado nas explosões de espinhos e afins). */
    static void spawnRing(ServerLevel world, Vec3 center, double radius, ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2.0 * Math.PI * i / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.sendParticles(particle, x, center.y, z, 1, 0.0, 0.02, 0.0, 0.0);
        }
    }

    /** Coluna vertical de partículas (espinho/pico saindo do chão). */
    static void spawnSpikeColumn(ServerLevel world, Vec3 base, double height, ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double t = (double) i / (points - 1);
            world.sendParticles(particle, base.x, base.y + t * height, base.z, 1, 0.04, 0.0, 0.04, 0.0);
        }
    }
}