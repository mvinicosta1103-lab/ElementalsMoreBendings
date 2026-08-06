package com.elementals.morebendings.bending.earthsubbendings.glass;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * "glassArmor" — terceira habilidade raiz da árvore de Glass (ver
 * {@link GlassElement}). Toggle passivo (mesmo esquema de {@code
 * CrystalArmorAbility}): aperta uma vez pra vestir uma casca fina de
 * vidro, que fica "armada" indefinidamente até o próximo golpe sofrido --
 * quando estilhaça sozinha, absorvendo parte do dano daquele golpe e se
 * desligando. Diferente de {@code CrystalArmorAbility}, não drena chi por
 * tick: o custo é só o cast inicial pra vestir, já que a couraça é
 * "descartável" (um único uso) em vez de um efeito contínuo.
 * em {@code LivingIncomingDamageEvent} (mesmo esquema de {@code
 * LavaArmorCombatHandler}/{@code PlasmaBoostCombatHandler}) -- esta classe
 * só liga/desliga a flag em {@link #ACTIVE}.
 */
public class GlassArmorAbility implements Ability {

    static final float ABSORB_FRACTION = 0.4f; // reduz 40% do golpe que estilhaça a couraça
    private static final float CAST_CHI_COST = 10.0f;

    private static final Set<UUID> ACTIVE = new HashSet<>();

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

        UUID id = caster.getUUID();
        if (ACTIVE.contains(id)) {
            // Desliga manualmente, sem estilhaçar -- desiste da couraça sem gastar de novo depois.
            ACTIVE.remove(id);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5f, 1.4f);
        } else {
            if (!bender.reduceChi(CAST_CHI_COST)) {
                bender.setCurrAbility(null);
                return;
            }
            ACTIVE.add(id);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.6f, 1.0f);
            level.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.3, 0.5, 0.3, 0.02);
        }

        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
        Player player = bender.player;
        if (player instanceof ServerPlayer caster) {
            ACTIVE.remove(caster.getUUID());
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.contains(playerId);
    }

    /** Chamado pelo {@link GlassArmorCombatHandler} assim que a couraça estilhaça num golpe. */
    public static void shatter(UUID playerId) {
        ACTIVE.remove(playerId);
    }
}