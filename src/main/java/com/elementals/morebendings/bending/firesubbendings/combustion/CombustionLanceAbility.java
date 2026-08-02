package com.elementals.morebendings.bending.firesubbendings.combustion;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "combustionLance" — terceira habilidade nova de {@link CombustionElement},
 * nó-folha gratuito (sem filhos, mesmo esquema de {@code combustionVent}).
 *
 * Contraste deliberado com {@link CombustionExplosionAbility}: onde o
 * Blast é um tiro invisível de alto risco (autodano se errar a mira/tempo),
 * o Molten Lance é um feixe VISÍVEL, contínuo e seguro -- sem foco
 * obrigatório, sem chance de backfire, sem explosão em área. Em troca, o
 * dano por segundo é bem mais baixo que o Blast. Pensado como a opção de
 * "chip damage"/finalização estável, pro jogador que não quer arriscar o
 * autodano do Blast toda vez.
 *
 * Canaliza enquanto Shift estiver segurado (mesmo esquema de
 * {@code LavaArmorAbility}): a cada {@link #HIT_INTERVAL_TICKS} ticks,
 * faz um raycast de entidade na mira atual e, se acertar algo vivo, aplica
 * um pouco de dano de queimadura e acende o alvo por um instante. Sem
 * cooldown -- o único freio é o custo de chi por tick.
 */
public class CombustionLanceAbility implements Ability {

    private static final float CAST_CHI_COST = 5.0f;
    private static final float TICK_CHI_COST = 0.4f;

    private static final double RANGE = 20.0;
    private static final int HIT_INTERVAL_TICKS = 5; // 0.25s entre cada "pulso" de dano
    private static final float DAMAGE_PER_HIT = 1.5f;
    private static final int IGNITE_TICKS = 40; // 2s de fogo por pulso que acerta

    private int activeTicks = 0;

    @Override
    public boolean activatesOnPress() {
        return true;
    }

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!player.isShiftKeyDown()) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        activeTicks = 0;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.6f, 0.9f);

        bender.setCurrAbility(this); // canalizada -- solta ao soltar Shift, ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !player.isShiftKeyDown()) {
            onRemove(bender);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            onRemove(bender);
            return;
        }
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }

        activeTicks++;

        // O rastro do feixe é visível o tempo todo -- diferente do Blast,
        // este é um dano contínuo e óbvio, sem elemento de surpresa.
        drawBeamParticles(level, caster);

        if (activeTicks % HIT_INTERVAL_TICKS == 0) {
            pulseDamage(level, caster);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        activeTicks = 0;
        bender.setCurrAbility(null);
    }

    private void pulseDamage(ServerLevel level, ServerPlayer caster) {
        EntityHitResult hit = SapsUtils.raycastEntity(caster, RANGE,
                entity -> entity instanceof LivingEntity living && living != caster && living.isAlive());

        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }

        target.hurt(level.damageSources().playerAttack(caster), DAMAGE_PER_HIT);
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), IGNITE_TICKS));
        target.hurtMarked = true;

        level.sendParticles(ParticleTypes.LAVA,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                3, 0.15, 0.15, 0.15, 0.0);
    }

    /** Feedback visual do feixe -- pequenas fagulhas ao longo da linha de mira até o alcance máximo. */
    private void drawBeamParticles(ServerLevel level, ServerPlayer caster) {
        Vec3 origin = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();

        EntityHitResult hit = SapsUtils.raycastEntity(caster, RANGE,
                entity -> entity instanceof LivingEntity living && living != caster && living.isAlive());
        double reach = hit != null ? origin.distanceTo(hit.getLocation()) : RANGE;

        for (double d = 0.5; d < reach; d += 1.0) {
            Vec3 point = origin.add(look.scale(d));
            level.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
    }
}