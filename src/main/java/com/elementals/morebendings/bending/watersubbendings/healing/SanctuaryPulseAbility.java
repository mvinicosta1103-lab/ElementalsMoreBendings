package com.elementals.morebendings.bending.watersubbendings.healing;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "sanctuaryPulse" — terceira habilidade raiz da árvore de Healing (ver
 * {@link HealingElement}).
 *
 * Pulso instantâneo em área ao redor do caster (raio {@link #RADIUS}):
 * cura {@link #HEAL_AMOUNT} em cada criatura viva não-hostil pega dentro
 * (o próprio caster incluso). Mobs hostis ({@link Monster}) são ignorados
 * -- não faz sentido curar quem está atacando o grupo.
 *
 * Instantânea: OBRIGATÓRIO liberar {@code currAbility} no final.
 */
public class SanctuaryPulseAbility implements Ability {

    private static final double RADIUS = 6.0;
    private static final float HEAL_AMOUNT = 4.0f;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(RADIUS);
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && !(entity instanceof Monster));

        for (LivingEntity ally : allies) {
            ally.heal(HEAL_AMOUNT);
            level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + ally.getBbHeight() * 0.5,
                    ally.getZ(), 4, 0.25, 0.3, 0.25, 0.02);
        }

        level.sendParticles(ParticleTypes.SPLASH, center.x, center.y + 0.2, center.z,
                40, RADIUS * 0.6, 0.2, RADIUS * 0.6, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.9f, 1.4f);

        caster.displayClientMessage(Component.literal(
                "§bSanctuary pulse healed " + allies.size() + " nearby creature(s)."), true);

        bender.setCurrAbility(null); // libera a trava -- ver MudSurgeAbility para explicação completa
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}