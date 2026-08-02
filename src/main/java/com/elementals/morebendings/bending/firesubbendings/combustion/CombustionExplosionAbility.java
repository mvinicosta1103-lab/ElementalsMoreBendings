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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "combustionExplosion" — habilidade raiz de {@link CombustionElement}, a
 * explosão principal.
 *
 * SEM cooldown -- o único freio contra spam é o custo de chi (foco +
 * manutenção por tick + disparo). Segurar o foco tempo demais ou soltar
 * cedo demais continua arriscado (autodano), então ainda não dá pra
 * simplesmente martelar a tecla sem cuidado, mas não existe mais um
 * temporizador fixo entre um tiro e outro.
 *
 * Fluxo (canaliza, igual {@code BoneControlAbility} -- não sobrescreve
 * {@link #activatesOnPress()}, então dispara ao SOLTAR a tecla de cast,
 * o que inicia o foco):
 *  1. {@link #onCall} -- solta a tecla de cast: acende o foco (partícula
 *     no olho, som de ignição) e trava a ability como {@code currAbility}.
 *  2. {@link #onTick} -- todo tick: cobra um custo pequeno de chi (manter
 *     a concentração cansa) e mostra onde a explosão vai acontecer AGORA
 *     se o jogador soltar (segue a mira via raycast). Se o jogador segurar
 *     o foco por tempo DEMAIS sem soltar, a energia acumulada estoura sem
 *     aviso -- overload, autodano maior.
 *  3. {@link #onLeftClick} -- solta a explosão. Se soltar ANTES do tempo
 *     mínimo de mira, a concentração não travou direito e o golpe volta
 *     pro próprio bender (autodano menor) -- fiel ao lore de Combustion
 *     Man/P'Li: precisa de mira deliberada, não dá pra "spammar".
 *  4. {@link #onRightClick} -- cancela sem soltar (sem punição, só perde
 *     o chi já gasto canalizando).
 *
 * Igual a P'Li: SEMPRE nasce um {@link CombustionBoltEntity} de verdade ao
 * soltar -- e esse bolt é invisível em pleno voo (sem modelo, sem rastro
 * de partículas; ver {@link CombustionBoltEntityRenderer}), então quem
 * está do outro lado só vê o clarão do disparo e a explosão no impacto,
 * nunca o tiro chegando. O upgrade "combustionGuidance" não muda mais
 * ISSO -- ele só liga o campo {@link CombustionBoltEntity#guided}, que
 * permite ao bolt "puxar" levemente a rota na direção do que o dono está
 * olhando durante os primeiros instantes de voo (homing), igual
 * P'Li/Combustion Man perseguindo um alvo em movimento. Sem Guidance, o
 * bolt ainda é o mesmo tiro invisível, só que reto -- fixo na direção do
 * disparo, sem corrigir rota no ar.
 */
public class CombustionExplosionAbility implements Ability {

    private static final float INITIATE_CHI_COST = 6.0f;
    private static final float TICK_CHI_COST = 0.5f;
    private static final float FIRE_CHI_COST = 12.0f;

    private static final int BASE_MIN_CHARGE_TICKS = 15; // 0.75s
    private static final int CHARGE_REDUCTION_PER_LEVEL = 4;
    private static final int MIN_CHARGE_FLOOR = 7; // 0.35s, mesmo com os 2 níveis

    /** Fixo -- segurar demais é sempre arriscado, upgrade nenhum resolve isso. */
    private static final int MAX_CHARGE_TICKS = 100; // 5s

    private static final double BASE_RANGE = 28.0;

    private static final float BASE_DAMAGE = 8.0f;
    private static final float POWER_I_DAMAGE_BONUS = 3.0f;
    private static final float POWER_II_DAMAGE_BONUS = 4.0f;

    private static final double BASE_RADIUS = 2.5;
    private static final double POWER_I_RADIUS_BONUS = 0.5;
    private static final double POWER_II_RADIUS_BONUS = 0.75;

    // -- backfire por soltura precoce (mira não travou) --
    private static final float PREMATURE_BACKFIRE_DAMAGE = 5.0f;
    private static final double PREMATURE_BACKFIRE_RADIUS = 1.75;
    private static final int PREMATURE_BLINDNESS_TICKS = 30; // 1.5s
    private static final int PREMATURE_CONFUSION_TICKS = 60; // 3s

    // -- backfire por overload (segurou foco demais) -- mais punitivo --
    private static final float OVERLOAD_BACKFIRE_DAMAGE = 8.0f;
    private static final double OVERLOAD_BACKFIRE_RADIUS = 2.25;
    private static final int OVERLOAD_BLINDNESS_TICKS = 60; // 3s
    private static final int OVERLOAD_CONFUSION_TICKS = 100; // 5s

    // Antes era 1.0 (tunado pra um bolt visível e lento). Agora que TODO
    // tiro é esse bolt -- e ele é invisível -- precisa ser rápido o
    // suficiente pra parecer quase instantâneo, igual P'Li, sem virar
    // hitscan de verdade (ainda dá pra desviar se você reagir rápido).
    private static final float BOLT_SPEED = 2.6f;

    /**
     * Por jogador (esta instância de Ability é ÚNICA, compartilhada por
     * todo mundo que usa Combustion -- ver {@code CombustionElement}), se
     * o som de "pode soltar com segurança" já tocou nesta canalização.
     * Limpo em {@link #endChannel} pra não vazar entre canalizações.
     */
    private static final Map<UUID, Boolean> readyCuePlayed = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();

        if (!bender.reduceChi(INITIATE_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        bender.abilityData = now; // guarda o tick de início do foco
        readyCuePlayed.put(caster.getUUID(), false); // reseta o aviso sonoro de "pode soltar" desta canalização

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, 0.5f, 0.6f);
        Vec3 eyePos = player.getEyePosition();
        level.sendParticles(ParticleTypes.SMALL_FLAME,
                eyePos.x, eyePos.y, eyePos.z, 3, 0.02, 0.02, 0.02, 0.005);
        caster.displayClientMessage(
                Component.literal("Mirando... espere o som confirmar antes de clicar."), true);

        bender.setCurrAbility(this); // canalizada -- ver onTick/onLeftClick/onRightClick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            endChannel(bender);
            return;
        }

        Long start = getStart(bender);
        if (start == null) {
            endChannel(bender);
            return;
        }

        long elapsed = level.getGameTime() - start;

        // Segurou a concentração tempo demais -- estoura sozinha, sem aviso.
        if (elapsed >= MAX_CHARGE_TICKS) {
            caster.displayClientMessage(
                    Component.literal("Você segurou o foco por tempo demais e ele estourou!"), true);
            CombustionExplosionUtils.selfBackfire(level, caster,
                    OVERLOAD_BACKFIRE_DAMAGE, OVERLOAD_BACKFIRE_RADIUS,
                    OVERLOAD_BLINDNESS_TICKS, OVERLOAD_CONFUSION_TICKS);
            endChannel(bender);
            return;
        }

        // Manter o foco cansa -- se o chi acabar no meio do caminho, a
        // concentração só se desfaz (sem punição extra, só frustrada).
        if (!bender.reduceChi(TICK_CHI_COST)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.3f);
            endChannel(bender);
            return;
        }

        // Feedback visual de onde a explosão vai cair AGORA, a cada poucos ticks.
        if (elapsed % 4 == 0) {
            HitResult aim = SapsUtils.raycastFull(player, BASE_RANGE, false);
            Vec3 point = aim.getLocation();
            level.sendParticles(ParticleTypes.SMALL_FLAME, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // Assim que cruza o tempo mínimo de mira, toca um som distinto UMA
        // vez -- é o sinal claro de "agora pode clicar sem risco de
        // soltura precoce". Sem isso, o jogador não tem como saber quando
        // a janela abriu e acaba testando o clique cedo demais, tomando
        // backfire sem entender o motivo.
        int minCharge = getMinChargeTicks(caster);
        if (elapsed >= minCharge && !Boolean.TRUE.equals(readyCuePlayed.get(caster.getUUID()))) {
            readyCuePlayed.put(caster.getUUID(), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4f, 2.0f);
        }
    }

    @Override
    public void onLeftClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            endChannel(bender);
            return;
        }
        Long start = getStart(bender);
        if (start == null) {
            return; // não está canalizando -- clique solto de outro contexto
        }

        long elapsed = level.getGameTime() - start;
        int minCharge = getMinChargeTicks(caster);

        if (elapsed < minCharge) {
            // Soltou cedo demais -- a mira não travou, o golpe volta pro
            // próprio bender em vez de sair na direção pretendida.
            caster.displayClientMessage(
                    Component.literal("Você clicou cedo demais -- a mira não travou! Espere o som antes de soltar."),
                    true);
            CombustionExplosionUtils.selfBackfire(level, caster,
                    PREMATURE_BACKFIRE_DAMAGE, PREMATURE_BACKFIRE_RADIUS,
                    PREMATURE_BLINDNESS_TICKS, PREMATURE_CONFUSION_TICKS);
            endChannel(bender);
            return;
        }

        if (!bender.reduceChi(FIRE_CHI_COST)) {
            // Ficou sem chi bem na hora de soltar -- fizzle, sem tiro e
            // sem punição extra (já pagou o preço da canalização).
            endChannel(bender);
            return;
        }

        fire(caster, level);
        endChannel(bender);
    }

    /** Cancela o foco sem soltar -- sem autodano, só perde o chi já gasto. */
    @Override
    public void onRightClick(Bender bender, boolean started) {
        if (!started) {
            return;
        }
        Player player = bender.player;
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.1f);
        }
        endChannel(bender);
    }

    @Override
    public void onRemove(Bender bender) {
        endChannel(bender); // interrupção externa (troca de elemento, logout, etc.) -- sem punição
    }

    /**
     * Igual a P'Li: sempre nasce um bolt invisível de verdade, nunca mais
     * um raycast instantâneo puro. Guidance só liga o homing do bolt
     * (ver {@link CombustionBoltEntity#guided}) -- o resto (invisibilidade,
     * dano, raio) é idêntico com ou sem o upgrade.
     */
    private void fire(ServerPlayer caster, ServerLevel level) {
        float damage = getDamage(caster);
        double radius = getRadius(caster);
        boolean guided = CombustionElement.hasUpgrade(caster, CombustionElement.COMBUSTION_GUIDANCE);

        CombustionBoltEntity bolt = new CombustionBoltEntity(level, caster);
        bolt.damage = damage;
        bolt.explosionRadius = radius;
        bolt.guided = guided;
        bolt.setDeltaMovement(caster, caster.getXRot(), caster.getYRot(), 0.0f, BOLT_SPEED, 0.5f);
        level.addFreshEntity(bolt);

        // Som audível no disparo (o alvo pode ouvir o "whoosh", só não
        // consegue ver o tiro chegando) -- mesmo princípio de P'Li/
        // Combustion Man: o aviso é sonoro/pela postura do bender, nunca
        // visual.
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    private void endChannel(Bender bender) {
        bender.setCurrAbility(null);
        bender.abilityData = null;
        readyCuePlayed.remove(bender.player.getUUID());
    }

    private Long getStart(Bender bender) {
        return bender.abilityData instanceof Long start ? start : null;
    }

    public static int getMinChargeTicks(ServerPlayer player) {
        int ticks = BASE_MIN_CHARGE_TICKS;
        if (CombustionElement.hasUpgrade(player, CombustionElement.COMBUSTION_CHARGE_I)) ticks -= CHARGE_REDUCTION_PER_LEVEL;
        if (CombustionElement.hasUpgrade(player, CombustionElement.COMBUSTION_CHARGE_II)) ticks -= CHARGE_REDUCTION_PER_LEVEL;
        return Math.max(ticks, MIN_CHARGE_FLOOR);
    }

    public static float getDamage(ServerPlayer player) {
        float damage = BASE_DAMAGE;
        if (CombustionElement.hasUpgrade(player, CombustionElement.COMBUSTION_POWER_I)) damage += POWER_I_DAMAGE_BONUS;
        if (CombustionElement.hasUpgrade(player, CombustionElement.COMBUSTION_POWER_II)) damage += POWER_II_DAMAGE_BONUS;
        return damage;
    }

    public static double getRadius(ServerPlayer player) {
        double radius = BASE_RADIUS;
        if (CombustionElement.hasUpgrade(player, CombustionElement.COMBUSTION_POWER_I)) radius += POWER_I_RADIUS_BONUS;
        if (CombustionElement.hasUpgrade(player, CombustionElement.COMBUSTION_POWER_II)) radius += POWER_II_RADIUS_BONUS;
        return radius;
    }
}