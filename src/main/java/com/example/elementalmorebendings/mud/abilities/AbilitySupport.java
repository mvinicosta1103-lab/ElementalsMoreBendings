package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.Elementals;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.Element;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Utilitários compartilhados por todas as habilidades de Mud.
 * Só usa a API pública do Elementals — não depende de nada do jar base do
 * addon Elementals Subbending, porque Mud é um Element inteiramente novo,
 * de propriedade deste addon.
 */
final class AbilitySupport {
    private AbilitySupport() {}

    static boolean isUnlocked(Bender bender, String upgradeName) {
        Element element = Element.getElement("Mud");
        return element != null && "Mud".equals(element.getName())
                && bender.hasElement(element)
                && bender.plrData.canUseUpgrade(upgradeName);
    }

    /**
     * Mud Mastery é o capstone da árvore inteira de Mud (só desbloqueável
     * depois de todos os outros upgrades dos 4 ramos, via
     * {@link MudElement#isSkillTreeComplete}). Quem já dominou a subbending
     * inteira não sofre mais o "cansaço" de gastar Chi pra usar habilidades
     * de Mud — todo custo, inicial ou por tick, é dispensado.
     */
    static boolean hasMudMastery(Bender bender) {
        Element element = Element.getElement("Mud");
        return element != null && "Mud".equals(element.getName())
                && bender.hasElement(element)
                && bender.plrData.canUseUpgrade("mudMastery");
    }

    /** Gasto de custo inicial (cast), já ciente de Mud Mastery. */
    static boolean spendUnlocked(Bender bender, String upgradeName, float cost) {
        if (!isUnlocked(bender, upgradeName)) {
            return false;
        }
        return hasMudMastery(bender) || bender.reduceChi(cost);
    }

    /** Gasto de custo inicial (cast) sem checar upgrade separado — pra habilidades que o próprio addAbility já garante estarem desbloqueadas. */
    static boolean spendChi(Bender bender, float cost) {
        return hasMudMastery(bender) || bender.reduceChi(cost);
    }

    /** Gasto de custo por tick (canais/toggles), já ciente de Mud Mastery. */
    static boolean spendChiPerTick(Bender bender, float cost) {
        return hasMudMastery(bender) || bender.reduceChi(cost, false);
    }

    /** Encerra a habilidade corrente do bender (equivalente ao padrão AbilitySupport.finish do jar base). */
    static void finish(Bender bender) {
        bender.abilityData = null;
        bender.setCurrAbility((Ability) null);
    }

    static boolean canDamageBlocks(ServerLevel world) {
        try {
            return world.getGameRules().getBoolean(Elementals.BENDING_GRIEFING);
        } catch (LinkageError | RuntimeException ignored) {
            return world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        }
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
}