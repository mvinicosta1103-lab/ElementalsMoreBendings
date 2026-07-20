package com.example.elementalmorebendings.lava.abilities;

import dev.saperate.elementals.Elementals;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Utilitários compartilhados por todas as habilidades deste addon.
 * Só usa a API pública do Elementals (não depende de nada do addon do
 * Jsumpter, já que suas classes internas não são acessíveis daqui).
 */
final class AbilitySupport {
    private AbilitySupport() {}

    static boolean isUnlocked(Bender bender, String upgradeName) {
        Element element = Element.getElement("Lava");
        return element != null && "Lava".equals(element.getName())
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

    static void pullToward(Vec3 origin, LivingEntity target, double strength, double lift) {
        Vec3 direction = origin.subtract(target.position()).normalize();
        if (direction.lengthSqr() < 0.01) direction = new Vec3(0, 0, 1);
        target.push(direction.x * strength, lift, direction.z * strength);
        target.hurtMarked = true;
    }
}
