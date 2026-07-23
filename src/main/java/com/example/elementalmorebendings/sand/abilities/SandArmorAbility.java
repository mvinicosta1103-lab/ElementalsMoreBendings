package com.example.elementalmorebendings.sand.abilities;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * SandArmorAbility ("sandArmor")
 * Ramo 1 (Defesa) — habilidade bônus anexada dentro do ramo "sandWall":
 * o jogador compacta uma couraça de arenito sobre a própria pele,
 * instantaneamente. Concede Resistência a dano e Absorção (corações extras
 * temporários) por um tempo — "sandArmorDurationI" estende quanto tempo a
 * couraça dura, "sandArmorHardenedI" deixa ela mais "endurecida"
 * (Resistência II em vez de I, mais corações de Absorção). Mesmo padrão de
 * {@code CrystalArmorAbility}/{@code MudShellAbility}.
 */
public class SandArmorAbility implements Ability {

    private static final float BASE_COST = 16.0f;
    private static final int BASE_DURATION = 140; // 7s
    private static final int HARDENED_BONUS_DURATION = 100; // +5s com o upgrade de duração
    private static final float BASE_ABSORPTION = 4.0f; // 2 corações
    private static final float HARDENED_ABSORPTION = 8.0f; // 4 corações

    public void onCall(Bender bender, long deltaT) {
        Player player2 = bender.player;
        if (!(player2 instanceof ServerPlayer player)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!AbilitySupport.spendUnlocked(bender, "sandArmor", BASE_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        int amplifier = bender.plrData.canUseUpgrade("sandArmorHardenedI") ? 1 : 0;
        int duration = bender.plrData.canUseUpgrade("sandArmorDurationI")
                ? BASE_DURATION + HARDENED_BONUS_DURATION : BASE_DURATION;
        float absorption = bender.plrData.canUseUpgrade("sandArmorHardenedI")
                ? HARDENED_ABSORPTION : BASE_ABSORPTION;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0));
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), absorption));

        ServerLevel world = player.serverLevel();
        AbilitySupport.spawnSandBurst(world, player.position().add(0, player.getBbHeight() * 0.5, 0), 24, 0.5);
        world.playSound(null, player.blockPosition(), SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.9f, 0.8f);

        bender.setCurrAbility(null);
    }

    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}