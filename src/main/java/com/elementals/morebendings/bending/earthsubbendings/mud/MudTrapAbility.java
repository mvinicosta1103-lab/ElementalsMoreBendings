package com.elementals.morebendings.bending.earthsubbendings.mud;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "mudTrap" — segunda habilidade raiz da árvore de Mud (ver {@link MudElement}).
 *
 * Dispara uma única linha de lama rente ao chão na direção mirada (raycast
 * via {@link SapsUtils#raycastFull}, mesmo utilitário que a
 * {@code AbilityEarthTrap} do mod base usa). Se acertar uma entidade viva,
 * a vítima fica imóvel e começa a afundar no chão — ver {@link MudTrapState}
 * pros detalhes de sufocamento/reconstrução do terreno.
 *
 * Canalizada: ao acertar, a habilidade NÃO libera {@code currAbility}
 * (diferente da {@code CrystalShardAbility}, que é instantânea) — fica
 * travada como a ability atual do bender até o jogador soltar o agachar,
 * então {@link #onTick} continua sendo chamado tick a tick nesse meio tempo.
 */
public class MudTrapAbility implements Ability {

    private static final double RANGE = 10.0;

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
        drawMudLine(level, player.position(), hit.getLocation());

        if (hit.getType() != HitResult.Type.ENTITY || !(hit instanceof EntityHitResult eHit)
                || !(eHit.getEntity() instanceof LivingEntity victim) || victim == player) {
            bender.setCurrAbility(null); // errou o alvo -- não trava a habilidade
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

    private void drawMudLine(ServerLevel level, Vec3 from, Vec3 to) {
        double length = from.distanceTo(to);
        if (length < 0.1) {
            return;
        }
        Vec3 step = to.subtract(from).scale(1.0 / length);
        int points = Math.max(4, (int) (length * 2));
        for (int i = 0; i <= points; i++) {
            Vec3 point = from.add(step.scale(length * i / points));
            level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.MUD.defaultBlockState()),
                    point.x, point.y + 0.1, point.z, 2, 0.08, 0.02, 0.08, 0.0);
        }
    }
}