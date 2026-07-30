package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * "lavaShuriken" — quarta habilidade da árvore de Lava (ver {@link
 * LavaElement}), ao lado de lavaPool/lavaJet/magmaSpike. Controla uma
 * {@link LavaShurikenEntity} em tempo real, igual o Water Blade original
 * (ver javadoc de {@link LavaShurikenEntity}): {@link #onCall} invoca e
 * prende a farpa (fica seguindo a mira, ver {@code controlEntity} na
 * entidade), e {@link #onLeftClick} solta ela reto na direção mirada.
 *
 * Igual ao Water Blade, a habilidade fica "presa" (não libera {@code
 * currAbility}) enquanto a farpa está sob controle -- só libera em
 * {@link #onLeftClick} (arremesso) ou {@link #onRemove} (cancelamento,
 * ex: troca de elemento).
 */
public class LavaShurikenAbility implements Ability {

    private static final float CHI_COST = 20.0f;
    private static final float THROW_SPEED = 1.6f;
    /** Distância à frente do jogador onde a farpa é invocada, já controlada. */
    private static final float SPAWN_DISTANCE = 2.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 spawnPos = SapsUtils.getEntityLookVector(player, SPAWN_DISTANCE);
        LavaShurikenEntity entity = new LavaShurikenEntity(level, player, spawnPos.x, spawnPos.y, spawnPos.z);
        level.addFreshEntity(entity);

        bender.abilityData = entity;
        bender.setCurrAbility(this); // mantém a trava -- só libera no arremesso ou no cancelamento
    }

    @Override
    public void onLeftClick(Bender bender, boolean started) {
        if (!(bender.abilityData instanceof LavaShurikenEntity entity) || entity.isRemoved()) {
            onRemove(bender);
            return;
        }

        entity.setControlled(false);
        entity.setDeltaMovement((Entity) bender.player, bender.player.getXRot(), bender.player.getYRot(), 0.0f,
                THROW_SPEED, 0.0f);

        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        if (bender.abilityData instanceof LavaShurikenEntity entity && !entity.isRemoved()) {
            entity.setControlled(false);
        }
        bender.abilityData = null;
        bender.setCurrAbility(null);
    }
}