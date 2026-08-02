package com.elementals.morebendings.bending.firesubbendings.combustion;

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

/**
 * "combustionSight" — segunda habilidade nova de {@link CombustionElement},
 * nó-folha gratuito (sem filhos, mesmo esquema de {@code combustionVent}).
 *
 * O "terceiro olho" de P'Li/Combustion Man não serve só pra mirar a
 * explosão -- no lore ele também é descrito como hipersensível a calor.
 * Aqui isso vira uma visão térmica de verdade: enquanto o bender segura
 * Shift com a ability ativa (mesmo esquema de canalização por Shift de
 * {@code LavaArmorAbility}/{@code AtmosphericDomeAbility}), ele:
 *  - Ganha Visão Noturna pra si mesmo (enxerga no escuro/fumaça).
 *  - Marca com Brilho (Glowing) toda criatura viva num raio ao redor,
 *    inclusive atrás de paredes -- o efeito clássico de "ver assinaturas
 *    de calor através de obstáculos".
 *
 * Sem risco, sem dano, só utilidade -- puramente uma habilidade de
 * percepção/reconhecimento. Sem cooldown: o custo de chi por tick é o
 * único freio.
 */
public class CombustionSightAbility implements Ability {

    private static final float CAST_CHI_COST = 6.0f;
    private static final float TICK_CHI_COST = 0.25f;

    private static final double RADIUS = 20.0;
    private static final int EFFECT_REFRESH_TICKS = 25; // reaplicado a cada tick ativo (>20 pra não piscar)

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

        if (!player.isShiftKeyDown()) {
            // Mesmo motivo do LavaArmorAbility: sem isso a visão nasceria e
            // morreria no mesmo tick, gastando chi sem o jogador perceber.
            caster.displayClientMessage(
                    Component.literal("Segure Shift para manter a Visão Térmica."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        applyEffects(level, caster);
        playCastFeedback(level, player);

        bender.setCurrAbility(this); // canalizada -- solta ao soltar Shift, ver onTick
    }

    @Override
    public void onTick(Bender bender) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !player.isShiftKeyDown()) {
            onRemove(bender);
            return;
        }
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            onRemove(bender);
            return;
        }

        // Reaplica em vez de deixar rodar todo tick -- mesma lógica de
        // "refresh" do LavaArmor, só que aqui o intervalo é maior porque
        // não precisa ser tão responsivo quanto uma armadura.
        if (caster.tickCount % EFFECT_REFRESH_TICKS == 0) {
            applyEffects(level, caster);
        }
    }

    @Override
    public void onRemove(Bender bender) {
        Player player = bender.player;
        // Remove a visão noturna na hora em vez de deixar o refresh
        // expirar sozinho -- fica mais responsivo ao soltar Shift. O
        // brilho em quem já foi marcado só termina sozinho (curta
        // duração), sem necessidade de rastrear e limpar cada entidade.
        player.removeEffect(MobEffects.NIGHT_VISION);
        bender.setCurrAbility(null);
    }

    private void applyEffects(ServerLevel level, ServerPlayer caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                EFFECT_REFRESH_TICKS + 5, 0, false, false, true));

        AABB area = caster.getBoundingBox().inflate(RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != caster)) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    EFFECT_REFRESH_TICKS + 5, 0, false, false, false));
        }
    }

    private void playCastFeedback(ServerLevel level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.5f, 1.4f);
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
        level.sendParticles(ParticleTypes.SMALL_FLAME,
                eyePos.x, eyePos.y, eyePos.z, 3, 0.02, 0.02, 0.02, 0.005);
    }
}