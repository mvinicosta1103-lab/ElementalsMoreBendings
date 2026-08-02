package com.elementals.morebendings.bending.watersubbendings.plant;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "rootSnare" — quinta habilidade raiz da árvore de Plant (ver {@link
 * PlantElement}), completando o kit ao lado de vineWhip/vineWall/
 * thornVolley/vineGrasp. Instantânea com cooldown, mesmo esquema de {@code
 * ResonantPulseAbility}: raízes brotam do chão num raio ao redor do caster,
 * prendendo (Lentidão pesada + Fraqueza, igual {@code
 * BonePuppeteerAbility#boneLock} faz pra alvos não-mortos-vivos) todo mundo
 * vivo pego dentro, exceto o próprio caster.
 * <p>
 * Diferente de {@code vineGrasp} (controle de UM alvo específico), esta é a
 * ferramenta de área/grupo -- prender vários inimigos de uma vez, sem
 * controlar o movimento deles de verdade, só travando.
 */
public class PlantRootSnareAbility implements Ability {

    private static final double RADIUS = 4.0;
    private static final int SNARE_DURATION_TICKS = 70; // 3.5s
    private static final int SLOW_AMPLIFIER = 5; // bem perto de imóvel, sem travar de vez
    private static final int WEAKNESS_AMPLIFIER = 1;
    private static final int BASE_COOLDOWN_TICKS = 160; // 8s
    private static final float CAST_CHI_COST = 15.0f;

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

        AABB area = caster.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != caster && entity.isAlive());

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    SNARE_DURATION_TICKS, SLOW_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    SNARE_DURATION_TICKS, WEAKNESS_AMPLIFIER));

            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
                    target.getX(), target.getY() + 0.1, target.getZ(), 8, 0.3, 0.05, 0.3, 0.0);
        }

        level.sendParticles(ParticleTypes.COMPOSTER,
                caster.getX(), caster.getY() + 0.1, caster.getZ(), 20, RADIUS * 0.5, 0.1, RADIUS * 0.5, 0.0);
        // SWEET_BERRY_BUSH_PLACE em vez de algo tipo "rooted_dirt_break" -- já
        // confirmado existente e usado em PlantThornVolleyAbility neste addon;
        // preferi um som testado a arriscar um campo de SoundEvents que eu não
        // consigo confirmar que existe sem conseguir compilar/rodar o jogo aqui.
        level.playSound(null, caster.blockPosition(), SoundEvents.SWEET_BERRY_BUSH_PLACE,
                SoundSource.PLAYERS, 1.0f, 0.7f);

        bender.setCurrAbility(null); // instantânea -- não canaliza
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}