package com.elementals.morebendings.bending.watersubbendings.ice;

import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "iceRing" — nó raiz novo da árvore de Ice (ver {@link IceElement}).
 * Invoca um anel de gelo (duas argolas entrelaçadas girando ao redor do
 * bender, ver referência visual pedida) que:
 *
 *  - Fica GIRANDO ao redor do jogador enquanto ativo (ver {@link #onTick}),
 *    empurrando pra fora qualquer {@link LivingEntity} hostil que chegue
 *    perto demais ({@link #DEFEND_RADIUS}) -- mesmo esquema de
 *    {@code AvatarStateManager#applyAirRingRepulsionEffect}, só que sem
 *    dano, é puramente defensivo.
 *  - Quebra periodicamente blocos frágeis ({@link #BREAKABLE}) que
 *    estejam dentro da faixa do anel -- vidro, folhagem, neve, teia etc.
 *  - Left Click ({@link #onLeftClick}) arranca UM estilhaço do anel e
 *    arremessa na mira do jogador (reaproveita {@link IceShardEntity},
 *    mesma entidade de {@code iceShard}). Depois de {@link #MAX_SHARDS}
 *    estilhaços arremessados o anel se esgota e desfaz sozinho.
 *  - Right Click ({@link #onRightClick}) arremessa de uma vez TODOS os
 *    estilhaços que ainda sobrarem do anel, um pra cada direção ao redor
 *    do jogador (padrão radial 360°) -- desfaz o anel na hora
 *    ("desformando o anel", ver enunciado).
 *
 * Instância ÚNICA compartilhada por todos os icebenders (mesmo esquema de
 * {@code LavaShurikenAbility}/{@code LavaSurfAbility}) -- todo o estado
 * por-jogador (quantos estilhaços já saíram) fica em {@code
 * bender.abilityData}, nunca em campo de instância desta classe.
 */
public class IceRingAbility implements Ability {

    private static final float CAST_CHI_COST = 18.0f;
    private static final float TICK_CHI_COST = 0.15f;

    /** Quantos estilhaços o anel aguenta arremessar antes de se esgotar sozinho. */
    private static final int MAX_SHARDS = 20;

    /** Raio do anel (distância do centro do jogador até a "casca" de partículas). */
    private static final double RING_RADIUS = 1.6;
    private static final double RING_HEIGHT_OFFSET = 1.1; // ~altura do peito
    private static final int POINTS_PER_LOOP = 26; // pontos de partícula por argola

    /** Empurra pra fora qualquer entidade viva (hostil) que chegue até aqui. */
    private static final double DEFEND_RADIUS = RING_RADIUS + 0.6;
    private static final double PUSH_STRENGTH = 0.5;
    private static final int DEFEND_TICK_INTERVAL = 4; // a cada 0.2s

    private static final float SHARD_SPEED = 2.4f;
    private static final float SHARD_DIVERGENCE = 1.5f;
    private static final int BLOCK_BREAK_INTERVAL = 10; // a cada 0.5s

    /** Blocos frágeis o suficiente pro anel simplesmente quebrar ao passar por perto. */
    private static final Set<Block> BREAKABLE = Set.of(
            Blocks.GLASS, Blocks.GLASS_PANE, Blocks.COBWEB,
            Blocks.SNOW, Blocks.SHORT_GRASS, Blocks.FERN,
            Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
            Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.TORCH, Blocks.WALL_TORCH
    );

    @Override
    public boolean activatesOnPress() {
        return true;
    }

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;

        // Segunda pressionada da tecla com o anel já ativo -- cancela em vez de
        // tentar invocar um segundo anel por cima.
        if (bender.abilityData instanceof RingData) {
            onRemove(bender);
            return;
        }

        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            bender.setCurrAbility(null);
            return;
        }

        if (!bender.reduceChi(CAST_CHI_COST)) {
            bender.setCurrAbility(null);
            return;
        }

        bender.abilityData = new RingData();
        bender.setCurrAbility(this); // canalizada -- só solta ao esgotar/cancelar, ver onLeftClick/onRightClick/onRemove

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.7f, 1.4f);
        spawnRingParticles(level, player, 0);
    }

    @Override
    public void onTick(Bender bender) {
        if (!(bender.abilityData instanceof RingData data)) {
            onRemove(bender);
            return;
        }
        Player player = bender.player;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            onRemove(bender);
            return;
        }
        if (!bender.reduceChi(TICK_CHI_COST)) {
            onRemove(bender);
            return;
        }

        data.age++;
        spawnRingParticles(level, player, data.age);

        if (data.age % DEFEND_TICK_INTERVAL == 0) {
            pushAwayNearbyEnemies(level, caster);
        }
        if (data.age % BLOCK_BREAK_INTERVAL == 0) {
            breakFragileBlocksNearby(level, caster);
        }
    }

    @Override
    public void onLeftClick(Bender bender, boolean started) {
        if (!started || !(bender.abilityData instanceof RingData data)) {
            return;
        }
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        IceShardEntity shard = new IceShardEntity(level, player);
        shard.setDeltaMovement(player, player.getXRot(), player.getYRot(), 0.0f, SHARD_SPEED, SHARD_DIVERGENCE);
        level.addFreshEntity(shard);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.4f, 1.7f);

        data.shardsFired++;
        if (data.shardsFired >= MAX_SHARDS) {
            // Anel esgotado -- some sozinho (ver JavaDoc da classe).
            despawnRing(level, player, false);
            bender.abilityData = null;
            bender.setCurrAbility(null);
        }
    }

    @Override
    public void onRightClick(Bender bender, boolean started) {
        if (!started || !(bender.abilityData instanceof RingData data)) {
            return;
        }
        Player player = bender.player;
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        int remaining = MAX_SHARDS - data.shardsFired;
        if (remaining <= 0) {
            // Não deveria sobrar chamada com o anel já esgotado, mas por
            // garantia: só desfaz sem arremessar nada.
            despawnRing(level, player, true);
            bender.abilityData = null;
            bender.setCurrAbility(null);
            return;
        }

        burstAllShards(level, player, remaining);
        despawnRing(level, player, true);
        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    @Override
    public void onRemove(Bender bender) {
        Player player = bender.player;
        if (bender.abilityData instanceof RingData && player.level() instanceof ServerLevel level) {
            despawnRing(level, player, false);
        }
        bender.abilityData = null;
        bender.setCurrAbility(null);
    }

    // ------------------------------------------------------------------
    // Visual: duas argolas de partículas entrelaçadas, girando ao redor
    // do jogador (referência: anéis de água/gelo cruzados na imagem).
    // ------------------------------------------------------------------

    private void spawnRingParticles(ServerLevel level, Player player, int age) {
        Vec3 center = player.position().add(0, RING_HEIGHT_OFFSET, 0);
        float baseYaw = player.getYRot();
        float spin = age * 6.0f; // graus por tick -- velocidade de rotação do anel

        // Argola A: inclinada "pra frente", girando num sentido.
        drawLoop(level, center, baseYaw + spin, 55f);
        // Argola B: inclinada pro outro lado, girando no sentido oposto --
        // é o cruzamento das duas que cria o efeito de "nós" entrelaçados.
        drawLoop(level, center, baseYaw - spin * 1.3f, -55f);
    }

    /**
     * Desenha uma argola circular de {@link #POINTS_PER_LOOP} pontos de
     * partícula: {@code yawDeg} gira a argola ao redor do eixo Y (vertical,
     * em pé) e {@code tiltDeg} inclina o plano da argola, exatamente como
     * girar um bambolê inclinado ao redor do jogador.
     */
    private void drawLoop(ServerLevel level, Vec3 center, float yawDeg, float tiltDeg) {
        double yaw = Math.toRadians(yawDeg);
        double tilt = Math.toRadians(tiltDeg);

        for (int i = 0; i < POINTS_PER_LOOP; i++) {
            double a = (2 * Math.PI * i) / POINTS_PER_LOOP;
            // Ponto no plano local da argola (círculo unitário no plano XY).
            double lx = Math.cos(a) * RING_RADIUS;
            double ly = Math.sin(a) * RING_RADIUS;

            // Inclina o plano em volta do eixo X local (tilt).
            double tx = lx;
            double ty = ly * Math.cos(tilt);
            double tz = ly * Math.sin(tilt);

            // Gira em volta do eixo Y (yaw) pra orientar a argola conforme o jogador.
            double x = tx * Math.cos(yaw) + tz * Math.sin(yaw);
            double z = -tx * Math.sin(yaw) + tz * Math.cos(yaw);

            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    center.x + x, center.y + ty, center.z + z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ------------------------------------------------------------------
    // Defesa: empurra inimigos que cheguem perto demais do anel.
    // ------------------------------------------------------------------

    private void pushAwayNearbyEnemies(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(DEFEND_RADIUS);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && entity.attackable());

        Vec3 center = player.position();
        for (LivingEntity target : nearby) {
            double dist = target.position().distanceTo(center);
            if (dist > DEFEND_RADIUS) {
                continue;
            }
            Vec3 away = target.position().subtract(center);
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5);
            }
            Vec3 push = away.normalize().scale(PUSH_STRENGTH).add(0, 0.15, 0);
            target.push(push.x, push.y, push.z);
            target.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------------
    // "Quebra blocos": varre a faixa do anel e destrói o que for frágil.
    // ------------------------------------------------------------------

    private void breakFragileBlocksNearby(ServerLevel level, ServerPlayer player) {
        Vec3 center = player.position().add(0, RING_HEIGHT_OFFSET, 0);
        Set<BlockPos> checked = new HashSet<>();

        for (int i = 0; i < POINTS_PER_LOOP; i++) {
            double a = (2 * Math.PI * i) / POINTS_PER_LOOP;
            double x = center.x + Math.cos(a) * RING_RADIUS;
            double y = center.y + Math.sin(a) * (RING_RADIUS * 0.4); // achatado -- só a faixa perto da altura do peito
            double z = center.z + Math.sin(a) * RING_RADIUS;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!checked.add(pos)) {
                continue;
            }
            if (BREAKABLE.contains(level.getBlockState(pos).getBlock())) {
                level.destroyBlock(pos, false, player); // sem soltar item -- vira só o efeito do anel
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    // ------------------------------------------------------------------
    // Arremesso final (Right Click): todos os estilhaços restantes de uma vez,
    // em leque 360° ao redor do jogador -- "desformando" o anel.
    // ------------------------------------------------------------------

    private void burstAllShards(ServerLevel level, Player player, int count) {
        Vec3 origin = player.position().add(0, RING_HEIGHT_OFFSET, 0);
        for (int i = 0; i < count; i++) {
            double a = (2 * Math.PI * i) / count;
            double dx = Math.cos(a);
            double dz = Math.sin(a);

            IceShardEntity shard = new IceShardEntity(level, player);
            shard.setPos(origin.x, origin.y, origin.z);
            // Direção explícita (radial) em vez da mira do jogador -- ver
            // AbstractElementalsEntity#setDeltaMovement(double,double,double,float,float).
            shard.setDeltaMovement(dx, 0.05, dz, SHARD_SPEED, 0.0f);
            level.addFreshEntity(shard);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    private void despawnRing(ServerLevel level, Player player, boolean burst) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                burst ? SoundEvents.GLASS_BREAK : SoundEvents.GLASS_PLACE,
                SoundSource.PLAYERS, 0.6f, burst ? 0.9f : 1.8f);
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + RING_HEIGHT_OFFSET, player.getZ(),
                24, RING_RADIUS * 0.6, 0.4, RING_RADIUS * 0.6, 0.02);
        if (player instanceof ServerPlayer caster) {
            caster.displayClientMessage(Component.literal(
                    burst ? "§bO anel de gelo se despedaça." : "§7O anel de gelo se esgotou."), true);
        }
    }

    /** Estado por-jogador -- guardado em {@code bender.abilityData}, ver JavaDoc da classe. */
    private static final class RingData {
        int shardsFired = 0;
        int age = 0;
    }
}