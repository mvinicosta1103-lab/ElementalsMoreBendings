package com.elementals.morebendings.bending.earthsubbendings.lava;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * "lavaShuriken" — quarta habilidade da árvore de Lava (ver {@link
 * LavaElement}), ao lado de lavaPool/lavaJet/magmaSpike. Controla uma
 * {@link LavaShurikenEntity} em tempo real, igual o Water Blade original
 * (ver javadoc de {@link LavaShurikenEntity}): {@link #onCall} invoca e
 * prende a farpa (fica seguindo a mira, ver {@code controlEntity} na
 * entidade), e {@link #onLeftClick} solta ela reto na direção mirada.
 *
 * Igual ao Water Blade, a habilidade fica "presa" (não libera {@code
 * currAbility}) enquanto a farpa está sob controle -- só libera em
 * {@link #onLeftClick} (arremesso) ou {@link #onRemove} (cancelamento,
 * ex: troca de elemento).
 *
 * CONSOME UM BLOCO DE VERDADE (2ª iteração, pedido explícito): igual o
 * {@code findSource} + {@code world.destroyBlock(source, false, owner)}
 * do mod de referência (decompilação de
 * dev.jayden.elementalssubbending.elements.abilities.LavaShurikenAbility/
 * LavaShurikenManager#create), {@link #findEarthSource} acha um bloco
 * "dobrável" de terra (mirado primeiro, senão o mais próximo do jogador)
 * e {@link #onCall} o destrói SEM soltar item (mesma convenção de
 * {@code level.destroyBlock(pos, false, player)} já usada em
 * {@code MudTrapState}/{@code AtmosphericDomeAbility} neste addon) antes
 * de spawnar a farpa exatamente onde o bloco estava -- pra parecer que o
 * bloco literalmente virou a Lava Shuriken. Como a entidade nasce em
 * sozinha do chão até a mira do jogador nos ticks seguintes.
 */
public class LavaShurikenAbility implements Ability {

    private static final float CHI_COST = 20.0f;
    private static final float THROW_SPEED = 1.6f;
    /** Alcance do raycast pra tentar achar terra bem na mira antes de procurar ao redor dos pés. */
    private static final double RAYCAST_RANGE = 8.0;
    /** Raio (em blocos) da busca em espiral ao redor do jogador, se o raycast não achar nada dobrável. */
    private static final int EARTH_SEARCH_RADIUS = 3;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer serverPlayer) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        BlockPos source = findEarthSource(serverPlayer, bender);
        if (source == null) {
            player.displayClientMessage(
                    Component.literal("A Lava Shuriken precisa de terra dobrável por perto."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        // O bloco vira a farpa: some do mundo (sem soltar item) e a entidade nasce
        // exatamente no centro dele.
        level.destroyBlock(source, false, player);
        level.playSound(null, source, SoundEvents.LAVA_POP, SoundSource.PLAYERS, 0.8f, 1.1f);

        Vec3 spawnPos = Vec3.atCenterOf(source);
        LavaShurikenEntity entity = new LavaShurikenEntity(level, player, spawnPos.x, spawnPos.y, spawnPos.z);
        level.addFreshEntity(entity);

        bender.abilityData = entity;
        bender.setCurrAbility(this); // mantém a trava -- só libera no arremesso ou no cancelamento
    }

    @Override
    public void onLeftClick(Bender bender, boolean started) {
        if (!(bender.abilityData instanceof LavaShurikenEntity entity) || entity.isRemoved()) {
            onRemove(bender);
            return;
        }

        entity.setControlled(false);
        entity.setDeltaMovement((Entity) bender.player, bender.player.getXRot(), bender.player.getYRot(), 0.0f,
                THROW_SPEED, 0.0f);

        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        if (bender.abilityData instanceof LavaShurikenEntity entity && !entity.isRemoved()) {
            entity.setControlled(false);
        }
        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    /**
     * Acha o bloco de terra "dobrável" (ver {@link EarthElement#isBlockBendable})
     * que vai virar a farpa: primeiro tenta o que o jogador está mirando
     * (raycast até {@link #RAYCAST_RANGE}), senão varre em anéis crescentes
     * ao redor dos pés do jogador (até {@link #EARTH_SEARCH_RADIUS}, um
     * bloco acima/abaixo) e devolve o primeiro que achar. Mesma ideia de
     * duas etapas do {@code findSource} do mod de referência.
     */
    private static BlockPos findEarthSource(ServerPlayer player, Bender bender) {
        HitResult hit = SapsUtils.raycastFull(player, RAYCAST_RANGE, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            if (EarthElement.isBlockBendable(pos, bender)) {
                return pos;
            }
        }

        BlockPos center = player.blockPosition();
        for (int radius = 0; radius <= EARTH_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Só o "anel" externo do raio atual -- os anéis menores já foram
                    // checados nas voltas anteriores do loop de fora.
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (EarthElement.isBlockBendable(pos, bender)) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }
}