package com.example.elementalmorebendings.crystal.abilities;

import com.example.elementalmorebendings.common.MasterySupport;
import dev.saperate.elementals.Elementals;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Utilitários compartilhados por todas as habilidades de Crystal. Mesmo
 * padrão de Mud/Lava/Plant: só usa a API pública do Elementals — não
 * depende de nenhuma classe interna de outro addon.
 */
final class AbilitySupport {
    private AbilitySupport() {}

    /** Cor "ametista" usada nas partículas de poeira (dust) das habilidades de Crystal. */
    static final DustParticleOptions CRYSTAL_DUST =
            new DustParticleOptions(new Vector3f(0.62f, 0.43f, 0.87f), 1.4f);
    static final DustParticleOptions CRYSTAL_DUST_DARK =
            new DustParticleOptions(new Vector3f(0.36f, 0.18f, 0.58f), 1.1f);

    static boolean isUnlocked(Bender bender, String upgradeName) {
        Element element = Element.getElement("Crystal");
        return element != null && "Crystal".equals(element.getName())
                && bender.hasElement(element)
                && bender.plrData.canUseUpgrade(upgradeName);
    }

    /**
     * Crystal Mastery: quando TODOS os upgrades da árvore de Crystal estão
     * desbloqueados (checagem genérica, ver {@link MasterySupport} — cobre
     * automaticamente qualquer ramo/upgrade que vier a ser adicionado no
     * futuro), a subbending é considerada masterizada. Quem já dominou a
     * subbending inteira não sofre mais o "cansaço" de gastar Chi pra usar
     * habilidades de Crystal — todo custo, inicial ou por tick, é
     * dispensado.
     */
    static boolean hasCrystalMastery(Bender bender) {
        return MasterySupport.isElementMastered(bender, "Crystal");
    }

    /** Gasto de custo inicial (cast), já ciente de Crystal Mastery. */
    static boolean spendUnlocked(Bender bender, String upgradeName, float cost) {
        if (!isUnlocked(bender, upgradeName)) {
            return false;
        }
        return hasCrystalMastery(bender) || bender.reduceChi(cost);
    }

    /** Gasto de custo inicial (cast) sem checar upgrade separado — pra habilidades que o próprio addAbility já garante estarem desbloqueadas. */
    static boolean spendChi(Bender bender, float cost) {
        return hasCrystalMastery(bender) || bender.reduceChi(cost);
    }

    /** Gasto de custo por tick (canais/toggles), já ciente de Crystal Mastery. */
    static boolean spendChiPerTick(Bender bender, float cost) {
        return hasCrystalMastery(bender) || bender.reduceChi(cost, false);
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

    /** Estouro de partículas de cristal (ametista + glint), usado nos impactos/eclosões. */
    static void spawnShatterBurst(ServerLevel world, Vec3 center, int count, double spread) {
        world.sendParticles(CRYSTAL_DUST, center.x, center.y, center.z, count, spread, spread, spread, 0.02);
        world.sendParticles(CRYSTAL_DUST_DARK, center.x, center.y, center.z, count / 2, spread * 0.7, spread * 0.7, spread * 0.7, 0.01);
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                center.x, center.y, center.z, Math.max(2, count / 4), spread * 0.4, spread * 0.4, spread * 0.4, 0.01);
    }
}