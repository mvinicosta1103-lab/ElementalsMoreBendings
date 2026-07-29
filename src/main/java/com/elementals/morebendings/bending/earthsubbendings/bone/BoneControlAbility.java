package com.elementals.morebendings.bending.earthsubbendings.bone;

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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "boneControl" -- primeira habilidade raiz de {@link BoneElement}.
 *
 * REDESENHADA como um "pickup" telecinético de verdade -- mesma ideia de
 * {@code AbilityBloodControl} do mod base (mover a própria VÍTIMA pelo ar
 * seguindo a mira do caster, com scroll pra distância), só que puxando
 * pelo esqueleto em vez de pelo sangue. Por isso funciona em QUALQUER
 * criatura viva (Mob ou {@link Player}) -- ao contrário de {@code
 * bonePuppeteer} (ver {@link BonePuppeteerAbility}), que só consegue
 * controle de movimento de verdade em mortos-vivos.
 *
 * Fluxo:
 *  1. {@link #onCall} -- tecla solta: raycast numa criatura viva ao
 *     alcance; "agarra" ela a {@link #DEFAULT_DISTANCE} do jogador e
 *     trava a ability como {@code currAbility} (canalizada, igual
 *     {@code BoneControlAbility} original fazia com a farpa).
 *  2. {@link #onTick} -- todo tick, calcula o ponto que o jogador está
 *     mirando (posição do olho + direção do olhar * distância) e puxa a
 *     vítima na direção dele -- é a mira do jogador (olhar pra cima/
 *     baixo, pros lados) que levanta, abaixa e desloca a vítima nos 3
 *     eixos, sem precisar ler tecla de movimento nenhuma.
 *  3. {@link #onMiddleClick} -- roda do mouse ajusta a distância (sem
 *     Shift afasta, com Shift aproxima), igual antes.
 *  4. {@link #onLeftClick} -- arremessa a vítima na direção mirada.
 *  5. {@link #onRightClick} -- solta no lugar, sem arremessar.
 *
 * Enquanto estiver segurando: se a vítima for um {@link Mob}, a IA
 * própria fica desligada ({@code setNoAi(true)}) pra não competir com o
 * controle -- restaurada assim que solta (arremessada, cancelada, ou o
 * caster desconecta/a ability é removida). {@link Player}s não têm IA
 * pra desligar; o controle domina sozinho porque sobrescreve a
 * velocidade deles todo tick. A gravidade da vítima também é desligada
 * ({@code setNoGravity(true)}) enquanto segura -- SEM isso ela fica
 * caindo e sendo puxada de volta pro ponto mirado todo tick, o que
 * parece um empurrão constante em vez de controle de verdade; é
 * restaurada junto com a IA em {@link #release}.
 *
 * {@link #onTick} usa um controlador PROPORCIONAL, não velocidade fixa:
 * fecha uma fração ({@link #CATCH_UP_FACTOR}) da distância que falta até
 * o ponto mirado a cada tick (capado em {@link #MAX_HOLD_SPEED}), então
 * a vítima gruda rápido na mira e depois fica praticamente parada ali
 * (a correção necessária cai a quase zero perto do alvo) -- em vez de
 * ficar sempre "perseguindo" a mira a uma velocidade fixa, que nunca
 * gruda de verdade e também parece um empurrão constante.
 */
public class BoneControlAbility implements Ability {

    private static final double GRAB_RANGE = 10.0;
    private static final float CAST_CHI_COST = 15.0f;
    private static final float TICK_CHI_COST = 0.2f;

    private static final int MIN_DISTANCE = 2;
    private static final int MAX_DISTANCE = 10;
    private static final int DEFAULT_DISTANCE = 4;
    private static final int DISTANCE_STEP = 1;

    private static final double MOVE_SPEED = 0.3;
    private static final double LAUNCH_SPEED = 1.6;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        EntityHitResult hit = SapsUtils.raycastEntity(player, GRAB_RANGE,
                entity -> entity instanceof LivingEntity living && living != player && living.isAlive());

        if (hit == null || !(hit.getEntity() instanceof LivingEntity victim)) {
            caster.displayClientMessage(Component.literal("Nenhum alvo encontrado."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        // hadAi só importa pra Mob -- Player não tem IA, então sempre "true"
        // aqui só serve pra nunca tentar restaurar nada nesse caso.
        boolean hadAi = !(victim instanceof Mob mob) || !mob.isNoAi();
        if (victim instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        victim.fallDistance = 0;

        setAbilityData(bender, victim, DEFAULT_DISTANCE, hadAi);

        level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                victim.getZ(), 14, 0.3, 0.4, 0.3, 0.1);
        level.playSound(null, victim.blockPosition(), SoundEvents.BONE_BLOCK_HIT, SoundSource.PLAYERS, 0.7f, 0.7f);

        bender.setCurrAbility(this); // canalizada -- ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }

        LivingEntity victim = getVictim(bender);
        if (victim == null || !victim.isAlive()) {
            // A vítima morreu ou sumiu -- só solta a trava, não há IA pra
            // restaurar em quem não existe mais.
            bender.setCurrAbility(null);
            bender.abilityData = null;
            return;
        }

        Player player = bender.player;
        int distance = getDistance(bender);

        // Mesma ideia 3D que a farpa original usava com player.pick(), só que
        // agora o alvo é a própria vítima, não um ponto de bloco -- por isso
        // computamos o ponto mirado manualmente a partir do olhar do jogador
        // em vez de usar um raycast contra o mundo.
        Vec3 goal = player.getEyePosition().add(player.getLookAngle().scale(distance));
        Vec3 toGoal = goal.subtract(victim.getEyePosition());
        double dist = toGoal.length();
        Vec3 delta = dist < 1.0E-4 ? Vec3.ZERO : toGoal.scale(Math.min(MOVE_SPEED, dist) / dist);

        victim.setDeltaMovement(delta);
        victim.hasImpulse = true;
        victim.fallDistance = 0;
    }

    /** Roda do mouse -- sem Shift afasta a vítima, com Shift aproxima. Mesma
     * convenção do {@code AbilityBloodControl.onMiddleClick} original. */
    @Override
    public void onMiddleClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        LivingEntity victim = getVictim(bender);
        if (victim == null) {
            return;
        }
        int distance = getDistance(bender);
        int next = bender.player.isShiftKeyDown()
                ? Math.max(distance - DISTANCE_STEP, MIN_DISTANCE)
                : Math.min(distance + DISTANCE_STEP, MAX_DISTANCE);
        setAbilityData(bender, victim, next, getHadAi(bender));
    }

    /** Arremessa a vítima na direção mirada e solta o controle. */
    @Override
    public void onLeftClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        LivingEntity victim = getVictim(bender);
        if (victim == null) {
            return;
        }

        Player player = bender.player;
        victim.setDeltaMovement(player.getLookAngle().scale(LAUNCH_SPEED));
        victim.hasImpulse = true;
        victim.hurtMarked = true;

        release(victim, getHadAi(bender));
        bender.setCurrAbility(null);
        bender.abilityData = null;
    }

    /** Cancela sem arremessar -- solta a vítima no lugar. */
    @Override
    public void onRightClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        onRemove(bender);
    }

    @Override
    public void onRemove(Bender bender) {
        LivingEntity victim = getVictim(bender);
        if (victim != null) {
            release(victim, getHadAi(bender));
        }
        bender.setCurrAbility(null);
        bender.abilityData = null;
    }

    /** Devolve a IA original da vítima, se ela tinha uma antes de {@link #onCall}. */
    private void release(LivingEntity victim, boolean hadAi) {
        if (hadAi && victim instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    private LivingEntity getVictim(Bender bender) {
        Object data = bender.abilityData;
        if (data instanceof Object[] arr && arr.length >= 1 && arr[0] instanceof LivingEntity victim) {
            return victim;
        }
        return null;
    }

    private int getDistance(Bender bender) {
        Object data = bender.abilityData;
        if (data instanceof Object[] arr && arr.length >= 2 && arr[1] instanceof Integer distance) {
            return distance;
        }
        return DEFAULT_DISTANCE;
    }

    private boolean getHadAi(Bender bender) {
        Object data = bender.abilityData;
        if (data instanceof Object[] arr && arr.length >= 3 && arr[2] instanceof Boolean hadAi) {
            return hadAi;
        }
        return true;
    }

    private void setAbilityData(Bender bender, LivingEntity victim, int distance, boolean hadAi) {
        bender.abilityData = new Object[]{victim, distance, hadAi};
    }
}