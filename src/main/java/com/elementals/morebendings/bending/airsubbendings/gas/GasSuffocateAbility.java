package com.elementals.morebendings.bending.airsubbendings.gas;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Ramo de especialização "gasSuffocate" (ver {@link GasElement}) — dano
 * direto e instantâneo em todo mundo (mobs inclusos) que estiver dentro
 * da nuvem no momento do cast. É o caminho "burst" do trio: sem duração,
 * sem efeito residual -- só dano cru na hora.
 * <p>
 * REWORK: Suffocate/Leak/Ignite deixaram de ser "efeitos alternados por
 * uma tecla de cycle" e viraram abilities independentes, cada uma com
 * tecla própria -- não existe mais uma especialização "ativa" pra
 * checar aqui. Só faz efeito se o jogador tiver comprado o nó.
 * <p>
 * Dano escala com:
 *  - gasSuffocateDamageI  → +1.5 de dano
 *  - gasSuffocateDamageII → +1.5 de dano
 */
public class GasSuffocateAbility {

    private static final float BASE_DAMAGE = 2.0f; // 1 coração

    public static void applyOnCloud(ServerPlayer caster, ServerLevel level, double radius) {
        if (!GasElement.hasUpgrade(caster, GasElement.GAS_SUFFOCATE)) {
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
        if (GasElement.hasUpgrade(player, GasElement.GAS_SUFFOCATE_DAMAGE_I)) damage += 1.5f;
        if (GasElement.hasUpgrade(player, GasElement.GAS_SUFFOCATE_DAMAGE_II)) damage += 1.5f;
        return damage;
    }

    private GasSuffocateAbility() {
    }
}