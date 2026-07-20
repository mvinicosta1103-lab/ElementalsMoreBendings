package com.example.elementalmorebendings.mud.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * MudShellAbility ("mudShell")
 * Ramo 1 (Defesa) — habilidade bônus anexada dentro do ramo "mudWall": o
 * jogador se cobre com uma casca de lama endurecida, ganhando Resistência
 * (menos dano recebido) por um tempo. "mudShellHardenedI" deixa a casca
 * mais "endurecida" (Resistência II em vez de I) e "mudShellDurationI"
 * estende quanto tempo ela dura.
 */
public class MudShellAbility implements Ability {

    private static final float BASE_COST = 22.0f;
    private static final int BASE_DURATION = 100; // 5s

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "mudShell", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        int amplifier = bender.plrData.canUseUpgrade("mudShellHardenedI") ? 1 : 0; // Resistência II ou I
        int duration = bender.plrData.canUseUpgrade("mudShellDurationI") ? BASE_DURATION + 100 : BASE_DURATION;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier));

        ServerLevel world = player.serverLevel();
        world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                player.getX(), player.getY() + 1.0, player.getZ(),
                35, 0.4, 0.6, 0.4, 0.04);

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}