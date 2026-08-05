package com.elementals.morebendings.bending.earthsubbendings.lava;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Dono de todas as faixas de {@link LavaFlowAbility} ativas no servidor.
 * Mesmo esquema independente de {@link LavaPoolManager}/{@link
 * MagmaSpikeManager} (lista própria, dirigida por um listener em {@code
 * ServerTickEvent.Post} registrado no {@code NeoForge.EVENT_BUS} -- ver
 * {@code ElementalsMoreBendingsMod}), mas com um estado a mais: cada
 * entrada passa primeiro por uma fase de CRESCIMENTO (uma fileira nova a
 * cada poucos ticks, cada vez mais larga -- ver {@link
 * LavaFlowAbility#MAX_STEPS}) antes de entrar na mesma fase de
 * resfriamento por contagem regressiva que lavaPool usa.
 */
public final class LavaFlowManager {

    private static final List<FlowEntry> ACTIVE = new ArrayList<>();

    private LavaFlowManager() {
    }

    /**
     * Registra um novo fluxo. {@code origin} é a coluna onde o jogador
     * estava (a fileira 0 nasce 1 bloco à frente dela, na direção {@code
     * dir}); {@code dir} é o vetor horizontal normalizado (y = 0) da
     * direção que o jogador olhava no momento do cast. {@code caster} só é
     * usado pra avisar o jogador no chat se o fluxo terminar de crescer
     * sem achar NENHUM bloco derretível (terreno não reconhecido) -- sem
     * isso a ability falha calada e parece "quebrada".
     */
    public static void startFlow(ServerLevel level, BlockPos origin, Vec3 dir, Player caster) {
        ACTIVE.add(new FlowEntry(level, origin, dir, caster));
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<FlowEntry> it = ACTIVE.iterator();
        while (it.hasNext()) {
            FlowEntry entry = it.next();
            entry.tick();
            if (entry.isFinished()) {
                it.remove();
            }
        }
    }

    /** Uma faixa de lavaFlow, crescendo fileira por fileira e depois esfriando de uma vez. */
    private static final class FlowEntry {
        final ServerLevel level;
        final BlockPos origin;
        final Vec3 dir;
        final Player caster;
        /** Perpendicular a `dir` no plano horizontal -- usada pra alargar a faixa dos dois lados. */
        final Vec3 perp;
        final Set<BlockPos> placed = new HashSet<>();

        int nextStep = 0;
        int ticksUntilNextStep = LavaFlowAbility.STEP_INTERVAL_TICKS;
        boolean growing = true;
        int coolTicksLeft = LavaFlowAbility.COOL_AFTER_TICKS;
        boolean warnedEmpty = false;

        FlowEntry(ServerLevel level, BlockPos origin, Vec3 dir, Player caster) {
            this.level = level;
            this.origin = origin;
            this.dir = dir;
            this.caster = caster;
            this.perp = new Vec3(-dir.z, 0, dir.x);
        }

        void tick() {
            if (growing) {
                ticksUntilNextStep--;
                if (ticksUntilNextStep <= 0) {
                    growStep(nextStep);
                    nextStep++;
                    ticksUntilNextStep = LavaFlowAbility.STEP_INTERVAL_TICKS;
                    if (nextStep >= LavaFlowAbility.MAX_STEPS) {
                        growing = false;
                        warnIfNothingPlaced();
                    }
                }
            } else {
                coolTicksLeft--;
                if (coolTicksLeft <= 0) {
                    cool();
                }
            }
        }

        boolean isFinished() {
            return !growing && coolTicksLeft <= 0;
        }

        /** Coloca mais uma fileira de lava, tanto mais larga quanto mais longe da origem. */
        void growStep(int step) {
            double distance = step + 1;
            Vec3 center = origin.getCenter().add(dir.scale(distance));

            int radius = Math.min(step / LavaFlowAbility.STEPS_PER_WIDEN, LavaFlowAbility.MAX_RADIUS);

            for (int offset = -radius; offset <= radius; offset++) {
                Vec3 point = center.add(perp.scale(offset));
                BlockPos column = BlockPos.containing(point.x, point.y, point.z);
                BlockPos ground = findMoltenableGround(column);

                if (ground == null || !placed.add(ground)) {
                    continue; // nada derretível aqui, ou já colocada por uma fileira anterior
                }

                level.setBlock(ground, Blocks.LAVA.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.LAVA,
                        ground.getX() + 0.5, ground.getY() + 1.0, ground.getZ() + 0.5, 6, 0.15, 0.1, 0.15, 0.0);
            }
        }

        /**
         * Desce a partir do topo real da coluna (heightmap), "derretendo"
         * (removendo) qualquer neve/gelo no caminho, até achar o primeiro
         * bloco de terreno que a lava pode substituir. Sem isso, uma
         * coluna coberta de neve nunca acha nada em {@link
         * LavaFlowAbility#MOLTENABLE} e a fileira inteira fica com buracos
         * -- exatamente o bug em biomas nevados.
         */
        private BlockPos findMoltenableGround(BlockPos column) {
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            // No máximo 4 blocos de profundidade procurando terreno de verdade sob a cobertura de neve/gelo.
            for (int i = 0; i < 4; i++) {
                pos = pos.below();
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();

                if (LavaFlowAbility.MELTABLE_OVERLAY.contains(block)) {
                    level.removeBlock(pos, false);
                    continue;
                }
                if (LavaFlowAbility.MOLTENABLE.contains(block)) {
                    return pos;
                }
                return null; // achou algo sólido que não é terreno derretível -- para aqui
            }
            return null;
        }

        /** Vira basalto -- só quem ainda for lava (o jogador pode ter mudado o bloco nesse meio tempo). */
        void cool() {
            for (BlockPos pos : placed) {
                if (level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.setBlock(pos, Blocks.BASALT.defaultBlockState(), 3);
                    level.sendParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.0);
                }
            }
            coolTicksLeft = 0;
        }

        /**
         * Se o fluxo terminou de crescer e NENHUM bloco foi colocado em
         * nenhuma fileira, o terreno inteiro na frente do jogador não é
         * reconhecido como derretível -- avisa em vez de falhar calado.
         */
        private void warnIfNothingPlaced() {
            if (!warnedEmpty && placed.isEmpty() && caster != null) {
                warnedEmpty = true;
                caster.sendSystemMessage(Component.literal(
                                "Lava Flow não achou nenhum bloco de terreno reconhecido na sua frente (chão não é terra/pedra/areia/etc).")
                        .withStyle(ChatFormatting.RED));
            }
        }
    }
}