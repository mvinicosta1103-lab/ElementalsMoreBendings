package com.example.elementalmorebendings.sand.abilities;

import com.example.elementalmorebendings.common.MasterySupport;
import dev.saperate.elementals.Elementals;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.earth.EarthElement;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Utilitários compartilhados por todas as habilidades de Sand. Mesmo
 * padrão de Crystal/Mud/Lava/Plant: só usa a API pública do Elementals —
 * não depende de nenhuma classe interna de outro addon, e é
 * package-private porque cada subbending tem a sua própria cópia
 * autocontida (ver o mesmo comentário em crystal.abilities.AbilitySupport).
 */
final class AbilitySupport {
    private AbilitySupport() {}

    /** Cor "areia" usada nas partículas de poeira (dust) das habilidades de Sand. */
    static final DustParticleOptions SAND_DUST =
            new DustParticleOptions(new Vector3f(0.87f, 0.78f, 0.55f), 1.3f);
    static final DustParticleOptions SAND_DUST_DARK =
            new DustParticleOptions(new Vector3f(0.63f, 0.51f, 0.33f), 1.0f);

    static boolean isUnlocked(Bender bender, String upgradeName) {
        Element element = Element.getElement("Sand");
        return element != null && "Sand".equals(element.getName())
                && bender.hasElement(element)
                && bender.plrData.canUseUpgrade(upgradeName);
    }

    /**
     * Sand Mastery: mesma lógica de Crystal/Mud Mastery (ver
     * {@link MasterySupport}) — quando todos os upgrades da árvore de Sand
     * estão desbloqueados, as habilidades de Sand deixam de custar Chi.
     */
    static boolean hasSandMastery(Bender bender) {
        return MasterySupport.isElementMastered(bender, "Sand");
    }

    static boolean spendUnlocked(Bender bender, String upgradeName, float cost) {
        if (!isUnlocked(bender, upgradeName)) {
            return false;
        }
        return hasSandMastery(bender) || bender.reduceChi(cost);
    }

    static boolean spendChi(Bender bender, float cost) {
        return hasSandMastery(bender) || bender.reduceChi(cost);
    }

    static boolean spendChiPerTick(Bender bender, float cost) {
        return hasSandMastery(bender) || bender.reduceChi(cost, false);
    }

    /**
     * Sand herda diretamente tudo que Earth já bende (a tag
     * elementals:earth_bendable_blocks já cobre areia, arenito, terra
     * etc.) — diferente de Crystal, não precisa de nenhuma tag bônus
     * própria, então isso é só um alias fino sobre
     * {@code EarthElement.isBlockBendable} pra manter o mesmo padrão de
     * chamada nas outras habilidades de Sand.
     */
    static boolean isSandBendable(BlockState state, Bender bender) {
        return EarthElement.isBlockBendable(state, bender);
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

    /** Rajada de partículas de areia (poeira clara + escura), usada nos impactos/explosões. */
    static void spawnSandBurst(ServerLevel world, Vec3 center, int count, double spread) {
        world.sendParticles(SAND_DUST, center.x, center.y, center.z, count, spread, spread, spread, 0.02);
        world.sendParticles(SAND_DUST_DARK, center.x, center.y, center.z, count / 2, spread * 0.7, spread * 0.7, spread * 0.7, 0.01);
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                center.x, center.y, center.z, Math.max(2, count / 4), spread * 0.4, spread * 0.4, spread * 0.4, 0.01);
    }
}