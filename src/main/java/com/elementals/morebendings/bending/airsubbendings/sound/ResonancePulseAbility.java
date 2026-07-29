package com.elementals.morebendings.bending.airsubbendings.sound;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "resonancePulse" — dispara uma frequência ressonante que estilhaça
 * blocos frágeis (vidro, gelo) num raio à frente do bender e desorienta
 * (Náusea) quem estiver perto do ponto de impacto.
 *
 * NOTA: não confundir com {@link ResonantPulseAbility} ("resonantPulse"),
 * a habilidade raiz gratuita original que faz os alvos brilharem
 * (eco-localização) — nomes parecidos, upgrades e efeitos diferentes.
 * Considere renomear uma das duas se a proximidade dos nomes causar
 * confusão na árvore de skills.
 *
 *  - resonancePulseRadiusI -> +1.0 de raio de impacto
 */
public class ResonancePulseAbility implements Ability {

    private static final double BASE_RANGE = 6.0;
    private static final double BASE_RADIUS = 2.5;
    private static final int BASE_COOLDOWN_TICKS = 160; // 8s
    private static final float CAST_CHI_COST = 4.0f;

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

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }
        lastUse.put(caster.getUUID(), now);

        Vec3 look = caster.getLookAngle();
        Vec3 origin = caster.position().add(0, caster.getEyeHeight(), 0);
        Vec3 impact = origin.add(look.scale(BASE_RANGE));

        double radius = SoundElement.hasUpgrade(caster, SoundElement.RESONANCE_PULSE_RADIUS_I)
                ? BASE_RADIUS + 1.0 : BASE_RADIUS;

        BlockPos center = BlockPos.containing(impact);
        int shattered = 0;
        int r = (int) Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distSqr(center) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isFragile(state)) {
                level.destroyBlock(pos, true, caster);
                shattered++;
            }
        }

        AABB area = new AABB(impact.x - radius, impact.y - radius, impact.z - radius,
                impact.x + radius, impact.y + radius, impact.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != caster && e.isAlive());
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }

        if (shattered > 0 || !targets.isEmpty()) {
            level.playSound(null, center, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.9f);
        }

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }

    private boolean isFragile(BlockState state) {
        Block block = state.getBlock();
        String key = block.builtInRegistryHolder().key().location().getPath();
        return key.contains("glass") || key.contains("ice");
    }
}
