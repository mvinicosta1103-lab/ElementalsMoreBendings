package com.elementals.morebendings.bending.earthsubbendings.crystal;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.effects.ElementalsStatusEffects;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "crystalPrison" — quarta habilidade raiz da árvore de Crystal (ver
 * {@link CrystalElement}), fechando o mesmo teto de 4 nós diretos que
 * {@code MudElement} usa. É a habilidade de controle de grupo: um golpe de
 * curto alcance (mesmo raycast de {@code PetrifyingTouchAbility}) que, se
 * acertar uma entidade viva, causa um dano leve, aplica {@code STUNNED}
 * (efeito de verdade do mod base -- trava movimento via
 * {@code makeStuckInBlock}) e, diferente de petrifyingTouch, também ergue
 * uma gaiola de {@link Blocks#AMETHYST_BLOCK} de verdade ao redor da vítima
 * (norte/sul/leste/oeste, dois de altura) -- reforço físico visível em cima
 * do efeito, não só decoração.
 *
 * A gaiola se estilhaça sozinha depois de {@link #PRISON_DURATION_TICKS},
 * junto com o fim do stun -- ver {@link CrystalPrisonManager}, registrado no
 * NeoForge.EVENT_BUS em {@code ElementalsMoreBendingsMod}.
 */
public class CrystalPrisonAbility implements Ability {

    private static final double RANGE = 5.0;
    private static final float CHI_COST = 35.0f;
    private static final float DAMAGE = 2.5f;
    private static final int PRISON_DURATION_TICKS = 100; // 5s
    private static final int STUN_AMPLIFIER = 0;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }
        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        EntityHitResult hit = SapsUtils.raycastEntity(player, RANGE,
                entity -> entity instanceof LivingEntity && entity != player);

        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);
            target.addEffect(new MobEffectInstance(ElementalsStatusEffects.STUNNED.get(),
                    PRISON_DURATION_TICKS, STUN_AMPLIFIER));

            Map<BlockPos, BlockState> original = new HashMap<>();
            List<BlockPos> cage = raiseCage(level, target, original);
            if (!cage.isEmpty()) {
                CrystalPrisonManager.registerCage(level, cage, original, PRISON_DURATION_TICKS);
            }

            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 0.9f, 0.9f);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.2f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    20, 0.35, 0.4, 0.35, 0.0);
        } else {
            // Errou o alvo -- som mais fraco, só pra dar feedback de que a
            // habilidade foi usada mesmo sem acertar nada.
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.5f, 0.8f);
        }

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    /**
     * Ergue blocos de {@code Blocks.AMETHYST_BLOCK} nas quatro direções
     * cardeais ao redor da vítima, em dois níveis de altura (pés e
     * cabeça), só nas posições que já estavam substituíveis (ar, grama
     * alta, etc.) -- nunca sobrescreve terreno sólido de verdade. Retorna
     * as posições alteradas; o {@link BlockState} original de cada uma é
     * salvo em {@code original} ANTES da troca, pra {@link
     * CrystalPrisonManager} conseguir devolver tudo como estava.
     */
    private List<BlockPos> raiseCage(ServerLevel level, LivingEntity target, Map<BlockPos, BlockState> original) {
        List<BlockPos> placed = new ArrayList<>();
        BlockPos feet = target.blockPosition();
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        for (Direction side : sides) {
            for (int h = 0; h <= 1; h++) {
                BlockPos pos = feet.relative(side).above(h);
                BlockState existing = level.getBlockState(pos);
                if (!existing.canBeReplaced()) {
                    continue; // já tem algo sólido ali -- já serve de parede sozinho
                }

                original.put(pos.immutable(), existing);
                level.setBlock(pos, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
                placed.add(pos.immutable());
            }
        }

        return placed;
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}