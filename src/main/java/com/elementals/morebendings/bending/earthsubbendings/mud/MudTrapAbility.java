package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * "mudTrap" — segunda habilidade raiz da árvore de Mud (ver {@link MudElement}).
 *
 * Dispara uma única linha de lama rente ao chão na direção mirada (raycast
 * via {@link SapsUtils#raycastFull}, mesmo utilitário que a
 * {@code AbilityEarthTrap} do mod base usa). A linha agora REALMENTE
 * transforma o bloco de chão sob cada ponto amostrado em {@link Blocks#MUD}
 * (antes só mandava partícula — ver {@link #layMudPath}). Se acertar uma
 * entidade viva, a vítima fica imóvel e começa a afundar no chão — ver
 * {@link MudTrapState} pros detalhes de sufocamento/reconstrução do terreno.
 *
 * Canalizada: ao acertar, a habilidade NÃO libera {@code currAbility}
 * (diferente da {@code CrystalShardAbility}, que é instantânea) — fica
 * travada como a ability atual do bender até o jogador soltar o agachar,
 * então {@link #onTick} continua sendo chamado tick a tick nesse meio tempo.
 *
 * IMPORTANTE: por padrão o framework só chama {@code onCall} quando a tecla
 * da habilidade é SOLTA (é assim que {@code AbilityEarthTrap} do mod base
 * funciona também, já que ela não sobrescreve {@code activatesOnPress()}).
 * Só que jogadores tendem a SEGURAR a tecla da armadilha esperando ela agir
 * na hora -- e enquanto a tecla continua pressionada, o evento de "soltar"
 * nunca acontece, então {@code onCall} nunca roda e nada acontece, não
 * importa se o jogador está agachado ou não. Por isso aqui a gente
 * sobrescreve {@link #activatesOnPress()} pra {@code true}: o raycast
 * dispara IMEDIATAMENTE ao apertar a tecla, sem precisar soltar.
 *
 * A armadilha só continua ativa enquanto o jogador estiver agachado. Por
 * isso exigimos agachar já no instante do cast (ver checagem em
 * {@link #onCall}): sem isso, a armadilha nasceria e morreria no mesmo tick
 * (o {@code onTick} seguinte via {@code isShiftKeyDown() == false} e
 * cancelaria tudo antes do jogador perceber qualquer coisa acontecendo).
 */
public class MudTrapAbility implements Ability {

    private static final double RANGE = 10.0;

    /** Blocos de chão "naturais" que a lama pode substituir ao longo do caminho. */
    private static final Set<Block> MUDDABLE = Set.of(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.DIRT_PATH, Blocks.PODZOL, Blocks.MYCELIUM,
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.CLAY
    );

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

        if (MudTrapManager.hasActiveTrap(caster)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);

        boolean hitLivingEntity = hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult eHit
                && eHit.getEntity() instanceof LivingEntity victim2 && victim2 != player;

        // A coluna da vítima fica por conta do MudTrapState (afunda/sufoca/restaura);
        // aqui a gente só cuida do resto do caminho até ela (ou até o ponto mirado, se errou).
        BlockPos victimColumn = hitLivingEntity
                ? ((LivingEntity) ((EntityHitResult) hit).getEntity()).blockPosition()
                : null;

        layMudPath(level, player.position(), hit.getLocation(), victimColumn);

        if (!hitLivingEntity) {
            bender.setCurrAbility(null); // errou o alvo -- não trava a habilidade
            return;
        }

        LivingEntity victim = (LivingEntity) ((EntityHitResult) hit).getEntity();

        if (!player.isShiftKeyDown()) {
            // Sem isso, a armadilha começava e cancelava no mesmo tick (onTick
            // vê isShiftKeyDown()==false e libera na hora) -- parecia que a
            // habilidade simplesmente não fazia nada, mesmo acertando o alvo.
            caster.displayClientMessage(
                    Component.literal("Segure Shift para prender o alvo na lama."), true);
            bender.setCurrAbility(null);
            return;
        }

        MudTrapManager.startTrap(level, caster, victim);
        // Sem setCurrAbility(null) aqui de propósito: fica canalizada.
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !MudTrapManager.hasActiveTrap(caster)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!player.isShiftKeyDown()) {
            MudTrapManager.release(caster);
            bender.setCurrAbility(null);
        }
        // Enquanto agachado com a armadilha ativa, o MudTrapManager (via
        // tick do servidor) já cuida sozinho do afundamento/sufocamento.
    }

    @Override
    public void onRemove(Bender bender) {
        if (bender.player instanceof ServerPlayer caster) {
            MudTrapManager.release(caster);
        }
    }

    /**
     * Converte o chão sob a linha mirada em {@link Blocks#MUD}, coluna por
     * coluna (antes esse método só mandava partícula e não alterava bloco
     * nenhum). A coluna informada em {@code skipColumn} (pés da vítima, se
     * houver) é ignorada aqui de propósito -- ela é tratada por
     * {@link MudTrapState}, que precisa saber o bloco original pra poder
     * restaurar ao soltar a armadilha.
     */
    private void layMudPath(ServerLevel level, Vec3 from, Vec3 to, BlockPos skipColumn) {
        double length = from.distanceTo(to);
        if (length < 0.1) {
            return;
        }

        Vec3 step = to.subtract(from).scale(1.0 / length);
        int points = Math.max(4, (int) (length * 2));

        BlockPos lastColumn = null;
        boolean placedAny = false;

        for (int i = 0; i <= points; i++) {
            Vec3 point = from.add(step.scale(length * i / points));
            BlockPos column = BlockPos.containing(point.x, point.y, point.z);
            if (column.equals(lastColumn)) {
                continue; // já processamos essa coluna
            }
            lastColumn = column;

            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).below();
            if (ground.equals(skipColumn)) {
                continue;
            }

            BlockState state = level.getBlockState(ground);
            if (!MUDDABLE.contains(state.getBlock())) {
                // Ainda mostra a partícula rente ao chão pra dar feedback visual
                // mesmo em blocos que não viram lama (pedra, caminho já de lama, etc).
                level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.MUD.defaultBlockState()),
                        point.x, point.y + 0.1, point.z, 2, 0.08, 0.02, 0.08, 0.0);
                continue;
            }

            level.setBlock(ground, Blocks.MUD.defaultBlockState(), 3);
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.MUD.defaultBlockState()),
                    ground.getX() + 0.5, ground.getY() + 1.05, ground.getZ() + 0.5, 6, 0.25, 0.05, 0.25, 0.0);
            placedAny = true;
        }

        if (placedAny) {
            level.playSound(null, BlockPos.containing(from), SoundEvents.MUD_PLACE, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }
}