package com.elementals.morebendings.bending.earthsubbendings.glass;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/**
 * "glassShards" — habilidade raiz (e única, por enquanto) da árvore de
 * Glass. Placeholder simples em modo hitscan (raycast na hora, sem
 * entidade própria) -- igual ao que Crystal usava antes de ganhar a
 * {@code CrystalShardEntity} de verdade (ver comentário histórico em
 * {@link com.elementals.morebendings.bending.earthsubbendings.crystal.CrystalShardAbility}).
 * Dispara um estilhaço de vidro na mira do jogador, causando dano leve a
 * quem for atingido.
 *
 * TODO: trocar por uma entidade de projétil de verdade (GlassShardEntity +
 * renderer), no mesmo esquema de CrystalShardEntity/BoneSpikeEntity, assim
 * que a arte/hitbox for definida -- por enquanto é só pra não deixar a
 * sub-bending sem nenhuma ability funcional.
 */
public class GlassShardsAbility implements Ability {

    private static final double RANGE = 15.0;
    private static final float DAMAGE = 3.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        EntityHitResult hit = SapsUtils.raycastEntity(player, RANGE,
                entity -> entity instanceof LivingEntity && entity != player);
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 1.2f);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}