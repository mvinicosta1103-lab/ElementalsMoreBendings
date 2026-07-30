package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * "mudSurge" — habilidade raiz (e única, por enquanto) da árvore de Mud.
 * Implementa {@link Ability} de verdade, então pode ser vinculada a uma
 * tecla via {@code Element.addAbility} / {@code Bender.bindAbility}, igual
 * qualquer outra habilidade do mod base.
 * <p>
 * REDESENHO: antes isso era um raycast AABB instantâneo -- checava quem
 * estava numa caixa na frente do jogador NA HORA do cast e aplicava
 * Lentidão, sem nada voando de verdade (só um leque de partículas
 * cosméticas ao longo da linha). Agora é uma onda de verdade: {@link
 * #CHUNK_COUNT} pedaços de lama endurecida ({@link MudSurgeChunkEntity})
 * nascem lado a lado, espalhados na perpendicular da mira, e avançam juntos
 * na direção mirada -- cada pedaço PERFURA quem acerta (não se desmancha no
 * primeiro alvo, ver javadoc de {@link MudSurgeChunkEntity}), então a leva
 * inteira varre uma fileira de inimigos de uma vez, não um cone único
 * calculado no instante do cast. É isso que faz virar uma AoE de verdade em
 * vez de um "hitscan disfarçado de projétil".
 * <p>
 * Instantânea do lado do CAST (sem {@link #onTick} / sem estado próprio na
 * ability em si -- o avanço da onda continua sozinho, cada
 * {@link MudSurgeChunkEntity} cuida do próprio tick): por isso, igual a
 * {@code CrystalShardAbility}, é OBRIGATÓRIO chamar
 * {@code bender.setCurrAbility(null)} no final do {@link #onCall} (e também
 * em {@link #onRemove}). O framework marca {@code currAbility} como "em uso"
 * assim que a tecla é pressionada e só aceita uma nova ativação -- de
 * QUALQUER habilidade, não só desta -- depois que ela for liberada de volta
 * pra {@code null}. Sem essa chamada, depois do primeiro uso o bender fica
 * travado nesta ability pra sempre.
 */
public class MudSurgeAbility implements Ability {

    private static final double RANGE = 8.0;
    /** Quantos pedaços formam a frente da onda -- distribuídos igualmente na perpendicular da mira. */
    private static final int CHUNK_COUNT = 5;
    /** Distância entre pedaços vizinhos da leva, em blocos -- largura total da onda = (CHUNK_COUNT - 1) * SPACING. */
    private static final double SPACING = 0.75;
    /** Velocidade de avanço de cada pedaço, em blocos/tick. */
    private static final double SPEED = 0.5;
    /** Ticks até um pedaço sumir sozinho se não bater em nada -- RANGE/SPEED com folga. */
    private static final int LIFETIME_TICKS = (int) Math.ceil(RANGE / SPEED) + 4;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        // Direção só no plano horizontal -- a onda avança rente ao chão, não
        // sobe/desce com o pitch da câmera (senão o jogador mirando pra baixo
        // faria os pedaços mergulharem no chão na hora).
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z).normalize();
        // Perpendicular no plano horizontal (rotaciona 90° em torno de Y) -- é
        // ao longo dela que os pedaços se espalham lado a lado.
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);

        Vec3 origin = new Vec3(player.getX(), player.getY() + 0.1, player.getZ());

        for (int i = 0; i < CHUNK_COUNT; i++) {
            double lateralOffset = (i - (CHUNK_COUNT - 1) / 2.0) * SPACING;
            Vec3 spawnPos = origin.add(side.scale(lateralOffset)).add(forward.scale(0.6));

            MudSurgeChunkEntity chunk = new MudSurgeChunkEntity(
                    level, player, spawnPos.x, spawnPos.y, spawnPos.z, forward, SPEED, LIFETIME_TICKS);
            level.addFreshEntity(chunk);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.MUD_PLACE, SoundSource.PLAYERS, 0.9f, 0.85f);

        // Rajada de partículas de largada, só pra marcar o instante do cast --
        // o resto do trajeto já é coberto pelos próprios MudSurgeChunkEntity.
        for (int i = 0; i < CHUNK_COUNT; i++) {
            double lateralOffset = (i - (CHUNK_COUNT - 1) / 2.0) * SPACING;
            Vec3 point = origin.add(side.scale(lateralOffset)).add(forward.scale(0.6));
            level.sendParticles(ParticleTypes.SPLASH, point.x, point.y, point.z, 4, 0.15, 0.05, 0.15, 0.01);
        }

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo (e usar outras abilities)
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}