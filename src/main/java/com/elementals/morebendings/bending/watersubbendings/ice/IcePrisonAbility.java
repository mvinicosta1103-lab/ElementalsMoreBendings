package com.elementals.morebendings.bending.watersubbendings.ice;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/**
 * "icePrison" — nó filho de {@link IceElement#FROST_NOVA} (mesmo esquema de
 * {@code CrystalArmorAbility} pendurada embaixo de {@code crystalWall}):
 * encapsula um alvo numa cela de gelo. Diferente de {@code
 * CrystalPrisonAbility} (duração fixa de 5s, solta sozinha), esta é
 * CANALIZADA e de duração mínima garantida -- ver {@link IcePrisonState}
 * pros detalhes exatos da trava de 60s.
 *
 * Igual {@code MudTrapAbility}, sobrescreve {@link #activatesOnPress()} pra
 * disparar o raycast imediatamente ao apertar a tecla (sem precisar
 * soltar), e exige o jogador já estar agachado no instante do cast --
 * senão a prisão nasceria mas o {@code onTick} seguinte não teria como
 * saber se aquilo foi um agachar de verdade ou só o dedo passando pela
 * tecla.
 */
public class IcePrisonAbility implements Ability {

    private static final double RANGE = 6.0;
    private static final float CHI_COST = 40.0f;

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

        if (IcePrisonManager.hasActivePrison(caster)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!player.isShiftKeyDown()) {
            caster.displayClientMessage(
                    Component.literal("Fique agachado ao mirar para prender o alvo numa prisão de gelo."), true);
            bender.setCurrAbility(null);
            return;
        }

        EntityHitResult hit = SapsUtils.raycastEntity(player, RANGE,
                entity -> entity instanceof LivingEntity && entity != player);

        if (hit == null || !(hit.getEntity() instanceof LivingEntity victim)) {
            bender.setCurrAbility(null); // errou o alvo -- não trava a habilidade
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        IcePrisonManager.startPrison(level, caster, victim);
        // Sem setCurrAbility(null) aqui de propósito: fica canalizada, ver onTick.
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !IcePrisonManager.hasActivePrison(caster)) {
            bender.setCurrAbility(null);
        }
        // Enquanto a prisão estiver ativa, o IcePrisonManager (via tick do
        // servidor) já cuida sozinho do stun/duração mínima/release -- essa
        // Ability só larga a trava de currAbility quando o Manager decide
        // (tick a tick, através de hasActivePrison) que a prisão acabou.
    }

    @Override
    public void onRemove(Bender bender) {
        if (bender.player instanceof ServerPlayer caster) {
            IcePrisonManager.release(caster);
        }
    }
}