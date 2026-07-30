package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "obsidianCrust" — oitava habilidade da árvore de Lava (ver {@link
 * LavaElement}). O oposto funcional de {@code lavaPool}/{@code
 * volcanicEruption}: em vez de criar lava, resfria a lava que já existe
 * ao REDOR DO PRÓPRIO JOGADOR (não é raycast na mira, é uma esfera
 * centrada no caster) instantaneamente pra {@link Blocks#OBSIDIAN} --
 * tanto fonte quanto corrente, sem precisar de água por perto (é o
 * bender "puxando o calor pra fora" na marra, não a reação química
 * vanilla lava+água).
 * <p>
 * Uso principal: defensivo/utilitário -- abrir caminho através de um lago
 * de lava, se proteger de uma erupção de outro lava-bender (inclusive a
 * própria {@link VolcanicEruptionAbility}/{@link LavaPoolAbility} deste
 * addon), ou preparar terreno sólido antes de avançar. Permanente (ao
 * contrário de {@code lavaPool}/{@code magmaSpike}, que revertem sozinhos)
 * -- faz sentido que virar pedra pra sempre seja o "preço" de matar uma
 * fonte de lava de verdade, então NÃO tem manager de reversão.
 * <p>
 * Reaproveita a mesma trinca de efeitos (LARGE_SMOKE + LAVA_EXTINGUISH)
 * que {@link LavaShurikenEntity#dryNearbyWater} já usa pra "água virando
 * obsidiana perto da farpa" -- aqui é lava virando obsidiana perto do
 * jogador, mesma linguagem visual/sonora por consistência.
 * <p>
 * Tem cooldown curto (ao contrário de lavaPool/magmaSpike, que só são
 * limitadas pelo chi) porque, sem isso, dava pra spammar e apagar lagos
 * de lava inteiros instantaneamente -- mesmo esquema de cooldown por
 * UUID que {@link VolcanicEruptionAbility} usa, só bem mais curto (é
 * utilitária, não uma ultimate).
 */
public class ObsidianCrustAbility implements Ability {

    /** Raio (esférico) ao redor do jogador em que lava é convertida. */
    private static final double RADIUS = 4.0;
    private static final float CHI_COST = 25.0f;
    private static final int COOLDOWN_TICKS = 60; // 3s
    /** Trava de segurança: nunca converte mais que isso numa única invocação, mesmo num lago gigante. */
    private static final int MAX_CONVERSIONS = 48;

    private static final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        long now = level.getGameTime();
        long last = lastUse.getOrDefault(caster.getUUID(), -1_000_000L);
        if (now - last < COOLDOWN_TICKS) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        int converted = coolNearbyLava(level, caster.blockPosition());
        if (converted > 0) {
            level.playSound(null, caster.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.9f);
            lastUse.put(caster.getUUID(), now);
        }

        // Instantânea -- não trava a habilidade, igual lavaPool/magmaSpike.
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    /**
     * Varre uma esfera de {@link #RADIUS} blocos centrada no jogador e converte
     * todo bloco de {@link Blocks#LAVA} (fonte ou corrente) pra {@link
     * Blocks#OBSIDIAN}. Retorna quantos blocos foram convertidos.
     */
    private int coolNearbyLava(ServerLevel level, BlockPos center) {
        int radiusBlocks = (int) Math.ceil(RADIUS);
        List<BlockPos> toConvert = new ArrayList<>();

        outer:
        for (int dx = -radiusBlocks; dx <= radiusBlocks; dx++) {
            for (int dy = -radiusBlocks; dy <= radiusBlocks; dy++) {
                for (int dz = -radiusBlocks; dz <= radiusBlocks; dz++) {
                    if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) {
                        continue;
                    }

                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(Blocks.LAVA)) {
                        continue;
                    }

                    toConvert.add(pos.immutable());
                    if (toConvert.size() >= MAX_CONVERSIONS) {
                        break outer;
                    }
                }
            }
        }

        for (BlockPos pos : toConvert) {
            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
        }

        return toConvert.size();
    }
}