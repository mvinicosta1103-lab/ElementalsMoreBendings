package com.elementals.morebendings.bending.watersubbendings.blood;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.entities.blood.BloodShotEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * "bloodTwinShot" -- nó enxertado no fim de {@code bloodShotEfficiencyII},
 * leaf do ramo {@code bloodShot} da árvore REAL de Blood (ver {@link
 * BloodMasteryGraft}).
 * <br><br>
 * Não recria a lógica de {@code AbilityBloodShot} do zero -- reaproveita
 * diretamente a entidade {@link BloodShotEntity} já existente no mod base
 * (mesmo "tijolo" de construção, função nova): em vez de UM projétil
 * teleguiado que carrega os efeitos ativos do caster, dispara DOIS ao
 * mesmo tempo em leque (pequeno desvio de yaw pra cada lado), cobrindo o
 * tema "Efficiency" do ramo -- não é mais barato por tiro, mas dobra a
 * chance de acerto num alvo em movimento por um custo total menor que
 * dois {@code AbilityBloodShot} manuais.
 * <br><br>
 * Cada BloodShotEntity aqui já nasce "destravado" ({@code setControlled(false)})
 * e com velocidade fixa na direção do leque -- diferente do
 * {@code AbilityBloodShot} original, que nasce "controlado" (teleguiado
 * até o 5º tick). Isso é proposital: Twin Shot é uma rajada instantânea,
 * não uma habilidade canalizada.
 */
public class BloodTwinShotAbility implements Ability {

    private static final float CHI_COST = 24.0f;
    private static final float SPREAD_YAW_DEGREES = 9.0f;
    private static final float SHOT_SPEED = 3.2f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.getData().canUseUpgrade(BloodMasteryGraft.BLOOD_TWIN_SHOT)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(this, CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        float baseYaw = caster.getYRot();
        float pitch = caster.getXRot();
        fireShot(level, caster, pitch, baseYaw - SPREAD_YAW_DEGREES);
        fireShot(level, caster, pitch, baseYaw + SPREAD_YAW_DEGREES);

        // Mesmo custo de "molhar a garganta" da versão base -- ver
        // AbilityBloodShot -- só que uma vez só pros dois tiros.
        player.hurt(player.damageSources().dryOut(), 2.0f);

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.5f, 1.3f);

        bender.setCurrAbility(null);
    }

    private void fireShot(ServerLevel level, ServerPlayer caster, float pitch, float yaw) {
        Vector3f spawnDir = directionFromRotation(pitch, yaw);
        var eye = caster.getEyePosition();
        double spawnX = eye.x + spawnDir.x * 1.2;
        double spawnY = eye.y + spawnDir.y * 1.2;
        double spawnZ = eye.z + spawnDir.z * 1.2;

        BloodShotEntity shot = new BloodShotEntity(level, caster, spawnX, spawnY, spawnZ, caster.getActiveEffects());
        shot.setControlled(false);
        shot.setDeltaMovement(caster, pitch, yaw, 0.0f, SHOT_SPEED, 0.0f);
        level.addFreshEntity(shot);
    }

    private Vector3f directionFromRotation(float pitch, float yaw) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        float x = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
        float y = (float) (-Math.sin(pitchRad));
        float z = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        return new Vector3f(x, y, z);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}