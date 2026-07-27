package com.elementals.morebendings.bending.watersubbendings.spirit;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * "purifyingWater" — primeira habilidade raiz da árvore de Spirit (ver
 * {@link SpiritElement}).
 *
 * Ao ativar, varre uma esfera de raio {@link #RADIUS} ao redor do caster
 * procurando criaturas vivas que estejam DENTRO d'água nesse instante
 * ({@link LivingEntity#isInWaterOrBubble()}) -- essa é a "água" a que a
 * descrição original se refere: não é uma água nova criada pela ability,
 * e sim qualquer corpo de água próximo ao caster onde uma vítima já esteja
 * submersa/molhada. Cada vítima encontrada é registrada no
 * {@link PurifyingWaterManager} com um pequeno atraso ({@link
 * PurifyingWaterManager#CATCH_DELAY_TICKS}) -- ela para, começa a brilhar
 * (Glowing) e só é processada de fato (dissolvida, curada ou consertada)
 * se continuar na água até o fim desse atraso. Se sair da água antes
 * disso, o Manager cancela a captura sozinho.
 *
 * O que acontece na resolução final está todo em {@link
 * PurifyingWaterManager#resolve}, não aqui -- esta classe só localiza os
 * alvos e inicia a captura.
 *
 * Instantânea (sem {@link #onTick} / sem estado próprio nesta classe):
 * por isso, igual a {@code MudSurgeAbility}, é OBRIGATÓRIO chamar
 * {@code bender.setCurrAbility(null)} no final do {@link #onCall} (e
 * também em {@link #onRemove}) -- ver o javadoc de {@code MudSurgeAbility}
 * pra detalhes de por que isso é necessário.
 */
public class PurifyingWaterAbility implements Ability {

    private static final double RADIUS = 8.0;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(RADIUS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && entity.isInWaterOrBubble());

        boolean caughtAny = false;
        for (LivingEntity victim : candidates) {
            if (PurifyingWaterManager.tryCatch(level, victim)) {
                caughtAny = true;
                victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, PurifyingWaterManager.CATCH_DELAY_TICKS + 20, 0));
                level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                        victim.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
            }
        }

        if (caughtAny) {
            level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.0f);
        }

        bender.setCurrAbility(null); // libera a trava -- ver MudSurgeAbility para explicação completa
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}