package com.elementals.morebendings.bending.airsubbendings.sound;

import com.elementals.morebendings.bending.airsubbendings.common.SoundAbility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Discordant Scream — mira num único alvo na linha de visão e libera um
 * grito dissonante que aplica Weakness + Slowness, simulando atordoamento
 * sonoro. Sem dano direto — é uma habilidade de controle/utilidade.
 */
public class DiscordantScreamAbility extends SoundAbility {

    private static final double BASE_RANGE = 10.0D;
    private static final long BASE_COOLDOWN_MS = 7000L;

    public DiscordantScreamAbility(ServerPlayer bender) {
        super(bender, "discordantScream");
    }

    @Override
    public long getCooldown() {
        return BASE_COOLDOWN_MS;
    }

    @Override
    public boolean execute() {
        ServerPlayer player = getBender();
        if (player == null) {
            return false;
        }

        double range = hasUpgrade("discordantScreamRangeI") ? BASE_RANGE + 4.0D : BASE_RANGE;
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));

        HitResult hit = player.level().clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player
        ));

        Entity target = findEntityInLine(player, start, end);
        if (target == null || !(target instanceof LivingEntity living)) {
            return false;
        }

        int duration = hasUpgrade("discordantScreamDurationI") ? 100 : 60;
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1));

        playSound("elementals:ability.discordant_scream", 1.5F, 0.6F);
        return true;
    }

    // Placeholder — substitua pelo mesmo helper de raytrace de entidade que o
    // addon já usa em MoltenGripAbility ("agarra o inimigo mais próximo à frente").
    private Entity findEntityInLine(ServerPlayer player, Vec3 start, Vec3 end) {
        return null;
    }
}