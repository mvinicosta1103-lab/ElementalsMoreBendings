package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * "lavaSurf" — quinta habilidade raiz da árvore de Lava (ver {@link
 * LavaElement}). Mobilidade: o bender surfa em cima de uma onda de lava
 * DE VERDADE (modelo 3D, não mais só partículas) que nasce debaixo dos
 * pés e o acompanha enquanto ele corre -- ver {@link
 * LavaSurfWaveVisualEntity}/{@link LavaSurfWaveVisualEntityRenderer}.
 * <p>
 * REDESENHO: antes exigia segurar Shift (agachado) pra canalizar, igual
 * {@code StaticLegsAbility}/o antigo {@code LavaArmorAbility} (agora
 * {@code lavaGeyser}). Agora o gatilho é {@link Player#isSprinting()}: a
 * surfada continua enquanto o jogador estiver correndo e solta sozinha
 * assim que ele para (anda, fica parado ou agacha) -- não precisa mais
 * ficar segurando Shift o tempo todo. Ainda usa {@link
 * #activatesOnPress()}=true e checa o requisito já no instante do cast
 * pelo mesmo motivo de sempre: sem isso a surfada nasceria e morreria no
 * mesmo tick, gastando chi sem o jogador perceber.
 * <p>
 * Enquanto ativa: Velocidade + Resistência a Fogo (a onda é lava de
 * verdade debaixo do jogador) + zera a distância de queda a cada tick pra
 * nunca tomar dano de queda saindo da surfada. A entidade da onda é
 * reposicionada pros pés do jogador todo tick (ver {@link #onTick}) --
 * estado por-jogador em {@link LavaSurfState}, porque esta ability é uma
 * instância ÚNICA compartilhada por todos os lavabenders (não dá pra
 * guardar a referência da onda como campo de instância aqui).
 */
public class LavaSurfAbility implements Ability {

    private static final float CAST_CHI_COST = 12.0f;
    private static final float TICK_CHI_COST = 0.4f;

    private static final int EFFECT_REFRESH_TICKS = 20; // 1s, reaplicado a cada tick ativo
    private static final int SPEED_AMPLIFIER = 1; // Velocidade II

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

        if (!player.isSprinting()) {
            // Mesmo motivo do antigo requisito de Shift: sem isso a surfada
            // nasceria e morreria no mesmo tick, gastando chi sem o jogador perceber.
            caster.displayClientMessage(
                    Component.literal("Esteja correndo para surfar na lava."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        spawnWave(level, caster);
        applyEffects(player);
        playTrailFeedback(level, player);

        bender.setCurrAbility(this); // canalizada -- solta ao parar de correr, ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !player.isSprinting()) {
            onRemove(bender);
            return;
        }
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }

        applyEffects(player);
        player.fallDistance = 0; // surfando não devia doer ao "descer" da onda
        followWave(caster);

        if (player.level() instanceof ServerLevel level && level.getGameTime() % 4 == 0) {
            playTrailFeedback(level, player);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        if (bender.player instanceof ServerPlayer caster) {
            LavaSurfWaveVisualEntity wave = LavaSurfState.get(caster);
            if (wave != null) {
                wave.discard();
            }
            LavaSurfState.clear(caster);
        }
        bender.setCurrAbility(null);
    }

    /** Nasce a onda debaixo dos pés do jogador e registra em {@link LavaSurfState}. */
    private void spawnWave(ServerLevel level, ServerPlayer caster) {
        LavaSurfWaveVisualEntity existing = LavaSurfState.get(caster);
        if (existing != null && existing.isAlive()) {
            existing.discard(); // não deveria sobrar uma onda velha, mas evita duplicar se sobrar
        }
        LavaSurfWaveVisualEntity wave = LavaSurfWaveVisualEntity.spawn(level, caster.position(), caster.getYRot());
        LavaSurfState.set(caster, wave);
    }

    /** Reposiciona a onda já ativa pros pés atuais do jogador -- chamado todo tick enquanto surfa. */
    private void followWave(ServerPlayer caster) {
        LavaSurfWaveVisualEntity wave = LavaSurfState.get(caster);
        if (wave != null && wave.isAlive()) {
            wave.followPlayer(caster.position(), caster.getYRot());
        }
    }

    private void applyEffects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                EFFECT_REFRESH_TICKS, SPEED_AMPLIFIER, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                EFFECT_REFRESH_TICKS, 0, false, false, true));
    }

    /** Trilha de partículas/som de lava debaixo dos pés -- complementa o modelo 3D da onda, puramente cosmético. */
    private void playTrailFeedback(ServerLevel level, Player player) {
        level.sendParticles(ParticleTypes.LAVA,
                player.getX(), player.getY() + 0.1, player.getZ(), 4, 0.3, 0.02, 0.3, 0.0);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 0.1, player.getZ(), 2, 0.25, 0.02, 0.25, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LAVA_AMBIENT, SoundSource.PLAYERS, 0.3f, 1.6f);
    }
}