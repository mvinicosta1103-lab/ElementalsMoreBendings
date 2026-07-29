package com.elementals.morebendings.bending.airsubbendings.sound;

import com.elementals.morebendings.bending.airsubbendings.common.SoundAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Resonance Pulse — dispara uma frequência ressonante que estilhaça blocos
 * frágeis (vidro, gelo) num raio à frente do bender e desorienta mobs
 * próximos com Nausea.
 */
public class ResonancePulseAbility extends SoundAbility {

    private static final double BASE_RANGE = 6.0D;
    private static final double BASE_RADIUS = 2.5D;
    private static final long BASE_COOLDOWN_MS = 8000L;

    public ResonancePulseAbility(ServerPlayer bender) {
        super(bender, "resonancePulse");
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

        Level level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 impact = origin.add(look.scale(BASE_RANGE));

        double radius = hasUpgrade("resonancePulseRadiusI") ? BASE_RADIUS + 1.0D : BASE_RADIUS;

        BlockPos center = BlockPos.containing(impact);
        int shattered = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset((int) -radius, (int) -radius, (int) -radius),
                center.offset((int) radius, (int) radius, (int) radius))) {
            if (pos.distSqr(center) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (isFragile(state)) {
                level.destroyBlock(pos, true, player);
                shattered++;
            }
        }

        AABB area = new AABB(impact.x - radius, impact.y - radius, impact.z - radius,
                impact.x + radius, impact.y + radius, impact.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }

        if (shattered > 0 || !targets.isEmpty()) {
            playSound("elementals:ability.resonance_pulse", 1.2F, 0.9F);
            return true;
        }
        return false;
    }

    private boolean isFragile(BlockState state) {
        Block block = state.getBlock();
        String key = block.builtInRegistryHolder().key().location().getPath();
        return key.contains("glass") || key.contains("ice");
    }
}