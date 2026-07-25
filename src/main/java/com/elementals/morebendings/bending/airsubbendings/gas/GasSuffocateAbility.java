package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Ramo de especialização "gasSuffocate" (ver {@link GasSkillTree}) — dano
 * instantâneo de asfixia em quem estiver dentro da nuvem no momento do
 * cast. Ao contrário do {@link GasLeakAbility} (nuvem residual) e do
 * {@link GasIgniteAbility} (explosão), essa é a opção "simples": sem
 * armadilha, sem risco pro próprio caster, só dano direto toda vez que a
 * Gas Cloud é usada.
 *
 * Exclusivo com gasLeak e gasIgnite.
 *
 * Dano escala com:
 *  - gasSuffocateDamageI  → +1.5 de dano
 *  - gasSuffocateDamageII → +1.5 de dano
 */
public class GasSuffocateAbility {

    private static final float BASE_DAMAGE = 2.0f; // 1 coração

    public static void register() {
        // Acionada a partir de GasCloudAbility.execute(), igual as outras
        // especializações — ver applyOnCloud() abaixo.
    }

    public static void applyOnCloud(ServerPlayer caster, ServerLevel level, double radius) {
        if (!GasElement.hasUpgrade(caster, GasSkillTree.GAS_SUFFOCATE)) {
            return;
        }
        float damage = getDamage(caster);
        DamageSource source = level.damageSources().indirectMagic(caster, caster);

        AABB area = caster.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());
        for (LivingEntity entity : nearby) {
            entity.hurt(source, damage);
        }
    }

    public static float getDamage(ServerPlayer player) {
        float damage = BASE_DAMAGE;
        if (GasElement.hasUpgrade(player, GasSkillTree.GAS_SUFFOCATE_DAMAGE_I)) damage += 1.5f;
        if (GasElement.hasUpgrade(player, GasSkillTree.GAS_SUFFOCATE_DAMAGE_II)) damage += 1.5f;
        return damage;
    }

    private GasSuffocateAbility() {
    }
}