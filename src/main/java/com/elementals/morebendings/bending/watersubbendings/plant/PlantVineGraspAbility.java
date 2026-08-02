package com.elementals.morebendings.bending.watersubbendings.plant;

import com.elementals.morebendings.effects.MoreBendingsEffects;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "vineGrasp" — quarta habilidade raiz da árvore de Plant (ver {@link
 * PlantElement}), ao lado de vineWhip/vineWall/thornVolley. É a habilidade
 * de controle pedida: prende a vítima em vinhas e deixa o CASTER
 * puxar/controlar/mover, levantar ou esmagar ela em tempo real enquanto
 * segura a tecla.
 * <p>
 * Estrutura inteira decalcada de {@code BoneControlAbility} (mesmo esquema
 * "pickup telecinético" -- funciona em QUALQUER criatura viva, inclusive
 * {@link Player}, ao contrário de {@code bonePuppeteer}, que só consegue
 * controle de movimento de verdade em mortos-vivos com IA pra sequestrar):
 * um {@link Player} não tem IA pra desligar, então o controle domina
 * sozinho sobrescrevendo a velocidade dele todo tick via {@code
 * setDeltaMovement} + {@code hurtMarked = true} (mesma técnica que {@link
 * PlantVineWhipAbility} já usa pra puxar vítimas).
 * <p>
 * Fluxo:
 *  1. {@link #onCall} -- tecla solta: raycast numa criatura viva ao
 *     alcance; agarra ela a {@link #DEFAULT_DISTANCE} do caster, nasce o
 *     modelo do cipó (ver {@link PlantVineGraspVisualEntity}) ligando a mão
 *     do caster à vítima, e trava a ability como {@code currAbility}
 *     (canalizada).
 *  2. {@link #onTick} -- todo tick, olha o que o caster está fazendo pra
 *     escolher o modo de controle:
 *       - Agachado (Shift) -> ESMAGAR: puxa a vítima pro chão e aplica
 *         {@link MoreBendingsEffects#CRUSHED} (lentidão pesada +
 *         dano periódico depois de um tempo, efeito já existente do addon,
 *         originalmente de {@code PressurePointAbility}).
 *       - Olhando bem pra cima (pitch <= {@link #LIFT_PITCH_THRESHOLD}) ->
 *         LEVANTAR: empurra a vítima pra cima continuamente.
 *       - Qualquer outro caso -> PUXAR/CONTROLAR/MOVER: a vítima é atraída
 *         suavemente pro ponto que o caster está mirando (olho + direção do
 *         olhar * distância) -- girar a câmera desloca a vítima nos 3
 *         eixos, andar pra trás puxa ela mais perto, andar pra frente
 *         empurra ela mais longe, tudo com a mesma mecânica.
 *     O cipó visual é reposicionado todo tick pra continuar ligando os dois
 *     pontos (ver {@link PlantVineGraspVisualEntity#updateEndpoints}).
 *  3. {@link #onMiddleClick} -- roda do mouse ajusta a distância de
 *     controle (sem Shift afasta, com Shift aproxima) -- só que aqui Shift
 *     já está ocupado pelo modo ESMAGAR, então o ajuste de distância usa só
 *     a direção da rolagem, sem depender de Shift (ver implementação).
 *  4. {@link #onLeftClick} -- arremessa a vítima na direção mirada
 *     (mesma ideia de "mover" levada ao extremo) e solta o cipó.
 *  5. {@link #onRightClick} -- solta a vítima no lugar, sem arremessar.
 * <p>
 * Enquanto segura: se a vítima for um {@link Mob}, a IA própria fica
 * desligada ({@code setNoAi(true)}) pra não competir com o controle --
 * restaurada em {@link #release} assim que solta (arremessada, cancelada,
 * ou o caster desconecta/a ability é removida).
 */
public class PlantVineGraspAbility implements Ability {

    private static final double GRAB_RANGE = 12.0;
    private static final float CAST_CHI_COST = 20.0f;
    private static final float TICK_CHI_COST = 0.25f;

    private static final int MIN_DISTANCE = 2;
    private static final int MAX_DISTANCE = 8;
    private static final int DEFAULT_DISTANCE = 3;
    private static final int DISTANCE_STEP = 1;

    private static final double MOVE_SPEED = 0.3;
    private static final double LIFT_SPEED = 0.45;
    private static final double CRUSH_PULL_SPEED = 0.4;
    private static final double LAUNCH_SPEED = 1.6;

    /** Pitch (graus, negativo = olhando pra cima) a partir do qual o modo vira LEVANTAR. */
    private static final float LIFT_PITCH_THRESHOLD = -45.0f;

    /** Curta o bastante pra ser reaplicada todo tick sem "vazar" muito além do próximo tick se o controle for solto. */
    private static final int CRUSH_EFFECT_DURATION_TICKS = 30;
    private static final int CRUSH_EFFECT_AMPLIFIER = 0;

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

        PlantVineGraspVisualEntity vine = PlantVineGraspVisualEntity.spawn(level,
                caster.getEyePosition(), victim.position());

        setAbilityData(bender, victim, DEFAULT_DISTANCE, hadAi, vine);

        level.sendParticles(ParticleTypes.COMPOSTER, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                victim.getZ(), 14, 0.3, 0.4, 0.3, 0.1);
        level.playSound(null, victim.blockPosition(), SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 0.9f, 0.7f);

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
            // A vítima morreu ou sumiu -- só solta a trava (e o cipó), não há
            // IA pra restaurar em quem não existe mais.
            discardVine(bender);
            bender.setCurrAbility(null);
            bender.abilityData = null;
            return;
        }

        Player player = bender.player;
        int distance = getDistance(bender);

        if (player.isShiftKeyDown()) {
            crush(victim);
        } else if (player.getXRot() <= LIFT_PITCH_THRESHOLD) {
            lift(victim);
        } else {
            control(player, victim, distance);
        }

        victim.hurtMarked = true;
        victim.fallDistance = 0;

        PlantVineGraspVisualEntity vine = getVine(bender);
        if (vine != null && !vine.isRemoved()) {
            vine.updateEndpoints(player.getEyePosition(), victim.position());
        }
    }

    /** Modo padrão: puxa/controla/move -- a vítima é atraída pro ponto mirado. */
    private void control(Player player, LivingEntity victim, int distance) {
        Vec3 goal = player.getEyePosition().add(player.getLookAngle().scale(distance));
        Vec3 toGoal = goal.subtract(victim.getEyePosition());
        double dist = toGoal.length();
        Vec3 delta = dist < 1.0E-4 ? Vec3.ZERO : toGoal.scale(Math.min(MOVE_SPEED, dist) / dist);
        victim.setDeltaMovement(delta);
        victim.hasImpulse = true;
    }

    /** Olhando pra cima: empurra a vítima continuamente pra cima. */
    private void lift(LivingEntity victim) {
        Vec3 current = victim.getDeltaMovement();
        victim.setDeltaMovement(current.x * 0.6, LIFT_SPEED, current.z * 0.6);
        victim.hasImpulse = true;
    }

    /** Agachado: esmaga a vítima contra o chão + efeito Crushed (lentidão pesada, dano depois de um tempo). */
    private void crush(LivingEntity victim) {
        Vec3 current = victim.getDeltaMovement();
        victim.setDeltaMovement(current.x * 0.5, -CRUSH_PULL_SPEED, current.z * 0.5);
        victim.hasImpulse = true;
        victim.addEffect(new MobEffectInstance(MoreBendingsEffects.CRUSHED,
                CRUSH_EFFECT_DURATION_TICKS, CRUSH_EFFECT_AMPLIFIER));
    }

    /** Roda do mouse -- ajusta a distância de controle. Shift já é o modo ESMAGAR aqui, então não inverte a direção. */
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
        int next = Math.min(distance + DISTANCE_STEP, MAX_DISTANCE);
        if (next == distance) {
            next = Math.max(distance - DISTANCE_STEP, MIN_DISTANCE);
        }
        setAbilityData(bender, victim, next, getHadAi(bender), getVine(bender));
    }

    /** Arremessa a vítima na direção mirada e solta o cipó. */
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
        discardVine(bender);
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
        discardVine(bender);
        bender.setCurrAbility(null);
        bender.abilityData = null;
    }

    /** Devolve a IA original da vítima, se ela tinha uma antes de {@link #onCall}. */
    private void release(LivingEntity victim, boolean hadAi) {
        if (hadAi && victim instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    private void discardVine(Bender bender) {
        PlantVineGraspVisualEntity vine = getVine(bender);
        if (vine != null && !vine.isRemoved()) {
            vine.discard();
        }
    }

    // --- leitura/escrita de bender.abilityData (Object[]{victim, distance, hadAi, vine}) -------------

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

    private PlantVineGraspVisualEntity getVine(Bender bender) {
        Object data = bender.abilityData;
        if (data instanceof Object[] arr && arr.length >= 4 && arr[3] instanceof PlantVineGraspVisualEntity vine) {
            return vine;
        }
        return null;
    }

    private void setAbilityData(Bender bender, LivingEntity victim, int distance, boolean hadAi, PlantVineGraspVisualEntity vine) {
        bender.abilityData = new Object[]{victim, distance, hadAi, vine};
    }
}