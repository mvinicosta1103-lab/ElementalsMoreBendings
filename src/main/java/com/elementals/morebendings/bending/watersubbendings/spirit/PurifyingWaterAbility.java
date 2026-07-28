package com.elementals.morebendings.bending.watersubbendings.spirit;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * procurando criaturas vivas. Cada uma é testada em {@link
 * PurifyingWaterManager#tryCatch} pra ver se pode ser capturada -- a
 * "água" aceita não é mais só água literal debaixo da vítima:
 *
 *  - Água/bolha/chuva sobre a vítima (fonte natural direta).
 *  - Ambiente ao redor dela contendo neve, gelo (todas as variantes),
 *    caldeirão, kelp, grama/folhagem ou folhas de árvore -- mesma lista
 *    que o mod base usa em {@code WaterElement#isBlockBendable} pra
 *    considerar um bloco "água bendável", mais {@code BlockTags.FLOWERS}
 *    (flores não entram nessa lista do mod base, então checamos à parte).
 *  - Se nada disso estiver por perto, tenta puxar 1 unidade de água do
 *    inventário do CASTER via {@code WaterElement#tryRetrieveWater}
 *    (funciona com Water Pouch OU um vidro de água comum -- é o mesmo
 *    método que o resto do mod base usa) e conjura uma poça temporária
 *    embaixo da vítima só pra viabilizar a captura. O bloco original é
 *    restaurado quando a captura termina (ver {@link
 *    PurifyingWaterManager}).
 *
 * Cada vítima capturada fica registrada no Manager com um atraso ({@link
 * PurifyingWaterManager#CATCH_DELAY_TICKS}) -- ela para, começa a
 * brilhar (Glowing) e só é processada de fato (dissolvida, curada ou
 * consertada) se o ambiente continuar válido até o fim desse atraso.
 *
 * Instantânea (sem {@link #onTick} / sem estado próprio nesta classe):
 * por isso, igual a {@code MudSurgeAbility}, é OBRIGATÓRIO chamar
 * {@code bender.setCurrAbility(null)} no final do {@link #onCall} (e
 * também em {@link #onRemove}).
 */
public class PurifyingWaterAbility implements Ability {

    private static final double RADIUS = 8.0;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        Vec3 center = player.position();
        AABB area = new AABB(center, center).inflate(RADIUS);

        // Não exige mais isInWaterOrBubble() aqui -- a checagem de ambiente
        // (natural ou via água reservada do caster) acontece dentro de tryCatch.
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive());

        boolean caughtAny = false;
        for (LivingEntity victim : candidates) {
            if (PurifyingWaterManager.tryCatch(level, caster, victim)) {
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
            caster.displayClientMessage(Component.literal(
                    "Nenhuma criatura próxima de água/neve/folhagem, e sem água reservada (Water Pouch/garrafa) pra improvisar."), true);
        }

        bender.setCurrAbility(null); // libera a trava -- ver MudSurgeAbility para explicação completa
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}