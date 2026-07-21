package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static dev.saperate.elementals.utils.SapsUtils.getEntityLookVector;

/**
 * MudBallAbility ("mudBall")
 * <p>
 * Cópia estrutural da AbilityAirBall do jar base (mesmo padrão
 * segurar-controlar / soltar-arremessar), só que spawnando um
 * {@link MudBallEntity} em vez de um AirBallEntity. Dano e Cegueira (se
 * "mudBallBlindnessI" estiver desbloqueado) já são resolvidos dentro do
 * próprio MudBallEntity.onHitEntity — essa classe só cuida do ciclo de
 * vida da habilidade (spawn -> segurar controlado -> soltar arremessado).
 */
public class MudBallAbility implements Ability {

    private static final float BASE_COST = 20.0f;
    private static final float THROW_SPEED = 0.9f;

    public void onCall(Bender bender, long deltaT) {
        if (!AbilitySupport.spendChi(bender, BASE_COST)) {
            if (bender.abilityData == null) {
                bender.setCurrAbility(null);
            } else {
                onRemove(bender);
            }
            return;
        }

        Player player = bender.player;
        Vector3f look = getEntityLookVector(player, 2.0f).toVector3f();

        MudBallEntity ball = new MudBallEntity(player.level(), player, look.x, look.y, look.z);

        bender.abilityData = ball;
        player.level().addFreshEntity(ball);
        bender.setCurrAbility(this);
    }

    public void onLeftClick(Bender bender, boolean held) {
        MudBallEntity ball = (MudBallEntity) bender.abilityData;
        onRemove(bender);
        if (ball == null) {
            return;
        }

        Player player = bender.player;
        ball.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0f, THROW_SPEED, 0f);
    }

    public void onRightClick(Bender bender, boolean held) {
        onRemove(bender);
    }

    public void onRemove(Bender bender) {
        MudBallEntity ball = (MudBallEntity) bender.abilityData;
        if (ball == null) {
            return;
        }

        ball.setControlled(false);
        bender.setCurrAbility(null);
    }
}