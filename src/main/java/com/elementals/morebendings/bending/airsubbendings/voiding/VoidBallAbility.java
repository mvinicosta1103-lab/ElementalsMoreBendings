package com.elementals.morebendings.bending.airsubbendings.voiding;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "voidBall" — primeira habilidade raiz da árvore de Void (ver {@link
 * VoidElement}). Instantânea, igual {@code LavaPoolAbility}/{@code
 * MudTrapAbility}: usa {@link SapsUtils#raycastFull} pra achar o ponto de
 * impacto (bloco ou entidade) na direção olhada, sem precisar de uma
 * entidade de projétil de verdade.
 *
 * No ponto de impacto, abre uma implosão de vácuo: toda criatura viva
 * dentro de {@link #PULL_RADIUS} (usando {@link
 * SapsUtils#getEntitiesInRadius}, que já exclui o próprio caster) é
 * puxada com força na direção do centro. Quem já estiver perto o
 * suficiente do centro (dentro de {@link #CRUSH_RADIUS}) também recebe
 * dano -- "esmagado" pelo colapso.
 */
public class VoidBallAbility implements Ability {

    private static final double RANGE = 12.0;
    private static final double PULL_RADIUS = 6.0;
    private static final double CRUSH_RADIUS = 2.0;
    private static final double PULL_STRENGTH = 0.6;
    private static final float CRUSH_DAMAGE = 6.0f;
    private static final int BASE_COOLDOWN_TICKS = 120; // 6s
    private static final float CAST_CHI_COST = 5.0f;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -100000L);
        if (now - last < BASE_COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 center = hit.getLocation();

        level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z,
                80, 0.3, 0.3, 0.3, 0.6);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                40, PULL_RADIUS * 0.3, PULL_RADIUS * 0.3, PULL_RADIUS * 0.3, 0.02);
        level.playSound(null, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z,
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.0f, 0.7f);

        List<LivingEntity> caught = SapsUtils.getEntitiesInRadius(center, (float) PULL_RADIUS, level, caster);

        DamageSource voidDamage = level.damageSources().indirectMagic(caster, caster);

        for (LivingEntity target : caught) {
            Vec3 toCenter = center.subtract(target.position());
            double distance = toCenter.length();
            if (distance > 0.001) {
                Vec3 pull = toCenter.normalize().scale(PULL_STRENGTH);
                target.push(pull.x, pull.y * 0.5, pull.z);
                target.hurtMarked = true;
            }

            if (distance <= CRUSH_RADIUS) {
                target.hurt(voidDamage, CRUSH_DAMAGE);
            }
        }

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}