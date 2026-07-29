package com.elementals.morebendings.bending.airsubbendings.voiding;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "voidStep" — segunda habilidade raiz da árvore de Void (ver {@link
 * VoidElement}). Instantânea. Diferente de voidBall (ofensiva), essa é
 * de mobilidade: um "passo pelo vazio" -- teleporta o caster pra frente,
 * na direção que está olhando, até {@link #RANGE} blocos.
 *
 * Pra não jogar o jogador dentro de parede, o destino é achado andando de
 * trás pra frente a partir do alcance máximo: o primeiro ponto (olhando
 * da ponta mais longe pra mais perto) em que tanto o bloco dos pés quanto
 * o da cabeça tenham colisão vazia é usado. Se nem o ponto mais perto
 * (1 bloco à frente) for livre, a habilidade não faz nada (evita
 * teleportar o jogador pra dentro de bloco).
 */
public class VoidStepAbility implements Ability {

    private static final double RANGE = 10.0;
    private static final double STEP = 0.5;
    private static final int BASE_COOLDOWN_TICKS = 100; // 5s
    private static final float CAST_CHI_COST = 3.0f;

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

        Vec3 origin = caster.position();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 destination = findSafeDestination(level, origin, look);

        if (destination == null) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                origin.x, origin.y + 1.0, origin.z, 25, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.3f);

        caster.teleportTo(destination.x, destination.y, destination.z);

        level.sendParticles(ParticleTypes.PORTAL,
                destination.x, destination.y + 1.0, destination.z, 25, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.0f);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    /**
     * Anda de {@link #RANGE} até {@link #STEP} (de trás pra frente) e devolve
     * o primeiro ponto onde o caster caberia (pés + cabeça com colisão
     * vazia). Devolve {@code null} se nenhum ponto no caminho for seguro.
     */
    private Vec3 findSafeDestination(ServerLevel level, Vec3 origin, Vec3 look) {
        for (double d = RANGE; d >= STEP; d -= STEP) {
            Vec3 candidate = origin.add(look.scale(d));
            BlockPos feet = BlockPos.containing(candidate.x, candidate.y, candidate.z);
            BlockPos head = feet.above();

            BlockState feetState = level.getBlockState(feet);
            BlockState headState = level.getBlockState(head);

            boolean feetClear = feetState.getCollisionShape(level, feet).isEmpty();
            boolean headClear = headState.getCollisionShape(level, head).isEmpty();

            if (feetClear && headClear) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}