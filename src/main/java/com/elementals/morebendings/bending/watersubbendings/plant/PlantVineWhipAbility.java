package com.elementals.morebendings.bending.watersubbendings.plant;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "vineWhip" — habilidade raiz da árvore de Plant. Um chicote de vinhas que
 * funciona em dois modos, dependendo do que a mira acerta (mesmo raycast de
 * {@code MudTrapAbility}/{@code CrystalShardAbility}, via
 * {@code SapsUtils.raycastFull}):
 *
 *  - Acertou uma entidade viva: a vinha prende o alvo e PUXA ELE até perto
 *    do caster, junto com um dano de impacto (o "chicote" de verdade).
 *  - Acertou só um bloco: a vinha se prende no ponto mirado e PUXA O
 *    PRÓPRIO CASTER até lá -- um grappling hook / locomoção por vinhas.
 *
 * Instantânea (igual CrystalShardAbility/MudSurgeAbility): libera
 * currAbility no final do onCall (e em onRemove), senão o bender trava
 * nesta ability pra sempre depois do primeiro uso.
 */
public class PlantVineWhipAbility implements Ability {

    private static final double RANGE = 14.0;
    private static final double ENTITY_PULL_STRENGTH = 1.4;
    private static final double GRAPPLE_PULL_STRENGTH = 1.6;
    private static final double GRAPPLE_MIN_DISTANCE = 1.5;
    private static final float DAMAGE = 3.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);

        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult eHit
                && eHit.getEntity() instanceof LivingEntity target && target != player) {
            whipEntity(level, player, target);
        } else if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bHit) {
            grappleSelf(level, player, bHit.getLocation());
        } else {
            // Errou tudo -- ainda dá feedback sonoro pra não parecer que a
            // habilidade simplesmente não fez nada.
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VINE_STEP, SoundSource.PLAYERS, 0.6f, 1.2f);
        }

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    /** Alvo acertado: puxa a vítima até perto do caster + dano de impacto. */
    private void whipEntity(ServerLevel level, Player caster, LivingEntity target) {
        Vec3 toCaster = caster.position().subtract(target.position());
        if (toCaster.length() > 0.01) {
            Vec3 pull = toCaster.normalize().scale(ENTITY_PULL_STRENGTH);
            // Mantém uma componente vertical mínima pra não "colar" o alvo
            // no chão quando ele está mais baixo que o caster.
            target.setDeltaMovement(pull.x, Math.max(pull.y, 0.15), pull.z);
            target.hurtMarked = true; // força sync da velocidade pro cliente
        }

        target.hurt(caster.damageSources().playerAttack(caster), DAMAGE);

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 0.8f, 0.9f);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        spawnVineTrail(level, caster.position().add(0, 1.2, 0), target.position().add(0, 1.0, 0));
    }

    /** Errou entidade, acertou bloco: puxa o PRÓPRIO caster até o ponto mirado. */
    private void grappleSelf(ServerLevel level, Player caster, Vec3 anchor) {
        Vec3 toAnchor = anchor.subtract(caster.position());
        if (toAnchor.length() < GRAPPLE_MIN_DISTANCE) {
            return; // ponto muito perto -- não vale a pena puxar
        }

        Vec3 pull = toAnchor.normalize().scale(GRAPPLE_PULL_STRENGTH);
        caster.setDeltaMovement(pull.x, Math.max(pull.y, 0.35), pull.z);
        caster.hurtMarked = true;
        caster.resetFallDistance();

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 0.8f, 1.0f);
        spawnVineTrail(level, caster.position().add(0, 1.2, 0), anchor);
    }

    /** Partículas ao longo da linha entre dois pontos -- dá a sensação de uma
     * vinha física esticando entre o caster e o alvo/âncora. */
    private void spawnVineTrail(ServerLevel level, Vec3 from, Vec3 to) {
        double length = from.distanceTo(to);
        if (length < 0.1) {
            return;
        }
        int points = Math.max(4, (int) (length * 3));
        Vec3 step = to.subtract(from).scale(1.0 / points);

        for (int i = 0; i <= points; i++) {
            Vec3 point = from.add(step.scale(i));
            level.sendParticles(ParticleTypes.COMPOSTER, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}