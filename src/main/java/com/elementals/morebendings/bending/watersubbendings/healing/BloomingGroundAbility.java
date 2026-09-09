package com.elementals.morebendings.bending.watersubbendings.healing;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.utils.SapsUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * "bloomingGround" — quarta habilidade raiz da árvore de Healing (ver
 * {@link HealingElement}).
 *
 * Raycast de bloco na direção mirada (mesmo esquema de {@code
 * IceSpikeAbility}); no ponto de impacto, varre um raio de {@link
 * #RADIUS} blocos ao redor e aplica o mesmo efeito de farinha de osso
 * (bonemeal) em qualquer {@link BonemealableBlock} válido encontrado --
 * mudas crescem, capim/flores brotam de grama, culturas avançam de
 * estágio, etc. Reaproveita a própria lógica vanilla do bloco
 * ({@code isValidBonemealTarget}/{@code isBonemealSuccess}/{@code
 * performBonemeal}), então funciona em qualquer coisa "bonemeal-ável" do
 * jogo ou de outros mods, sem precisar de uma lista fixa de blocos.
 *
 * Instantânea: OBRIGATÓRIO liberar {@code currAbility} no final.
 */
public class BloomingGroundAbility implements Ability {

    private static final double RANGE = 10.0;
    private static final int RADIUS = 3;
    /** Quantas vezes tenta aplicar bonemeal em cada bloco válido (blocos vanilla checam sucesso via RNG, igual um item de farinha de osso de verdade). */
    private static final int ATTEMPTS_PER_BLOCK = 3;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        HitResult hit = SapsUtils.raycastFull(player, RANGE, false);
        Vec3 hitPos = hit.getLocation();
        BlockPos center = BlockPos.containing(hitPos.x, hitPos.y, hitPos.z);

        List<BlockPos> bloomed = applyBonemeal(level, center);

        if (bloomed.isEmpty()) {
            caster.displayClientMessage(Component.literal("§7Nothing nearby wants to bloom."), true);
            bender.setCurrAbility(null);
            return;
        }

        level.playSound(null, center, SoundEvents.CROP_PLANTED, SoundSource.PLAYERS, 0.8f, 1.1f);
        bender.setCurrAbility(null); // libera a trava -- ver MudSurgeAbility para explicação completa
    }

    /** @return as posições em que algum crescimento de verdade aconteceu, só pra decidir se toca som/feedback. */
    private List<BlockPos> applyBonemeal(ServerLevel level, BlockPos center) {
        List<BlockPos> bloomed = new ArrayList<>();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (dx * dx + dz * dz > RADIUS * RADIUS) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (tryBonemeal(level, pos)) {
                        bloomed.add(pos.immutable());
                    }
                }
            }
        }
        return bloomed;
    }

    private boolean tryBonemeal(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) {
            return false;
        }
        if (!bonemealable.isValidBonemealTarget(level, pos, state)) {
            return false;
        }

        boolean grew = false;
        for (int attempt = 0; attempt < ATTEMPTS_PER_BLOCK; attempt++) {
            if (!bonemealable.isValidBonemealTarget(level, pos, state)) {
                break; // já cresceu o suficiente / virou outro bloco
            }
            if (bonemealable.isBonemealSuccess(level, level.getRandom(), pos, state)) {
                bonemealable.performBonemeal(level, level.getRandom(), pos, state);
                grew = true;
            }
            state = level.getBlockState(pos);
        }

        if (grew) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        }
        return grew;
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}