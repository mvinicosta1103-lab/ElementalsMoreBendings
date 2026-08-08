package com.elementals.morebendings.bending.earthsubbendings.lava;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;

/**
 * "lavaGeyser" — sétima habilidade da árvore de Lava (ver {@link
 * LavaElement}), substituindo a antiga {@code lavaArmor} (defensiva,
 * "casca de lava" ao segurar Shift). Agora é ofensiva: mira (raycast via
 * {@link SapsUtils#raycastEntity}, mesmo esquema de {@code
 * BonePuppeteerAbility}) uma criatura viva; no chão sob os pés dela, um
 * jato de lava rompe a superfície e a arremessa pra cima, causando dano e
 * incendiando.
 * <p>
 * O jato tem modelo 3D de verdade (não é só troca de textura no bloco) --
 * ver {@link LavaGeyserVisualEntity}/{@link LavaGeyserVisualEntityRenderer},
 * mesmo esquema de {@link MagmaSpikeVisualEntity}/{@code
 * MagmaSpikeVisualEntityRenderer} usado por {@code magmaSpike}/{@code
 * volcanicEruption}, só que vertical/fino (jato) em vez de uma estalagmite.
 * Diferente deles, porém, o jato fica de pé JORRANDO lava de verdade por
 * alguns segundos ({@link #JET_LIFETIME_TICKS}) em vez de sumir quase
 * instantâneo, e a fuligem que ele solta continua no ar por mais um
 * tempo depois que o jato já recolheu ({@link #SOOT_TAIL_TICKS}) -- ver
 * {@link LavaGeyserVisualEntity#tick()} pra a emissão contínua de
 * partícula em vez de só a explosão pontual do instante do impacto.
 * <p>
 * A ability em si continua instantânea (sem {@code onTick}/sem estado
 * próprio, igual {@code MagmaSpikeAbility}) -- quem carrega a duração
 * agora é a própria {@link LavaGeyserVisualEntity}, então {@code
 * setCurrAbility(null)} ainda é chamado sempre no fim do {@link #onCall}.
 */
public class LavaGeyserAbility implements Ability {

    private static final double RANGE = 14.0;
    private static final float CHI_COST = 30.0f;
    private static final float DAMAGE = 6.0f;
    private static final double LAUNCH_UP = 1.3;
    private static final int IGNITE_SECONDS = 4;
    /** Quanto tempo (em ticks de servidor) o jato de lava fica de pé jorrando de verdade. */
    private static final int JET_LIFETIME_TICKS = 60; // ~3s jorrando
    /** Quanto tempo a fuligem continua sozinha no ar depois que o jato já recolheu. */
    private static final int SOOT_TAIL_TICKS = 40; // ~2s de fuligem assentando após o jato acabar
    /** Vida total da entidade visual (jato + cauda de fuligem). */
    private static final int VISUAL_LIFETIME_TICKS = JET_LIFETIME_TICKS + SOOT_TAIL_TICKS; // ~5s no total

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        EntityHitResult hit = SapsUtils.raycastEntity(player, RANGE,
                entity -> entity instanceof LivingEntity living && living != player && living.isAlive());

        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            caster.displayClientMessage(Component.literal("Nenhum alvo encontrado."), true);
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                target.blockPosition()).below();

        erupt(level, target, ground);

        bender.setCurrAbility(null); // libera a trava pra poder usar de novo
    }

    /** Faz o jato brotar sob {@code ground} e aplica dano/arremesso/ignição em {@code target}. */
    private void erupt(ServerLevel level, LivingEntity target, BlockPos ground) {
        LavaGeyserVisualEntity.spawn(level, ground, JET_LIFETIME_TICKS, VISUAL_LIFETIME_TICKS);

        target.hurt(level.damageSources().lava(), DAMAGE);
        target.push(0, LAUNCH_UP, 0);
        target.hurtMarked = true; // garante que o cliente sincronize o impulso vertical
        target.igniteForSeconds(IGNITE_SECONDS);

        // Explosão de partículas só no instante do impacto -- o jorro contínuo de lava e a
        // fuligem que dura além disso já ficam por conta da própria LavaGeyserVisualEntity
        // (ver seu tick()), então isso aqui é só o "estouro" inicial pra dar impacto na hora.
        level.sendParticles(ParticleTypes.LAVA,
                ground.getX() + 0.5, ground.getY() + 0.6, ground.getZ() + 0.5, 16, 0.25, 0.3, 0.25, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                ground.getX() + 0.5, ground.getY() + 0.8, ground.getZ() + 0.5, 8, 0.2, 0.4, 0.2, 0.02);
        level.playSound(null, ground, SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.7f);
        level.playSound(null, ground, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}