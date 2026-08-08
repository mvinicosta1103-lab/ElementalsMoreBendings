package com.elementals.morebendings.bending.avatarstate;

import com.elementals.morebendings.commands.MoreBendingCommand;
import com.elementals.morebendings.data.PlayerAvatarData;
import com.elementals.morebendings.network.packets.SyncAvatarStatePacket;
import com.elementals.morebendings.registry.ModAttachments;
import commonnetwork.api.Dispatcher;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.air.AirElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.elements.fire.FireElement;
import dev.saperate.elementals.elements.water.WaterElement;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Avatar State "de verdade" -- ligado pelo próprio jogador (tecla, ver
 * {@code ToggleAvatarStatePacket}), diferente de {@code /morebending
 * avatar} (que é o comando de operador pra forçar ligar/desligar em
 * qualquer um). Reaproveita {@link MoreBendingCommand#grantAvatarState}/
 * {@link MoreBendingCommand#revokeAvatarState} pra conceder/revogar as
 * bendings (mesmo rastreamento via {@link PlayerAvatarData}, então nunca
 * tira algo que o jogador já tinha por fora), e por cima disso aplica o
 * "boost" (efeitos de status) e o efeito visual (partículas dos 4
 * elementos girando + olhos brilhantes, ver {@code AvatarStateEyesLayer}).
 * <p>
 * Só pode ser ligado por quem já domina os 4 elementos-base (Air, Water,
 * Earth, Fire) -- ninguém "ganha" o Avatar sem antes ser um bender
 * completo por conta própria; ver {@link #isEligible(ServerPlayer)}.
 */
public final class AvatarStateManager {

    private static final Set<UUID> ACTIVE = new HashSet<>();

    // Reforçado a cada ~4s (80 ticks) enquanto ativo, com folga de sobra
    // pra nunca deixar o efeito cair antes do próximo reforço.
    private static final int EFFECT_DURATION_TICKS = 140;
    private static final int EFFECT_REFRESH_INTERVAL = 80;

    private AvatarStateManager() {
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.contains(player.getUUID());
    }

    public static boolean isEligible(ServerPlayer player) {
        Bender bender = Bender.getBender(player);
        return bender.hasElement(AirElement.get())
                && bender.hasElement(WaterElement.get())
                && bender.hasElement(EarthElement.get())
                && bender.hasElement(FireElement.get());
    }

    /** @return o novo estado (true = ligou, false = desligou/não conseguiu ligar). */
    public static boolean toggle(ServerPlayer player) {
        if (isActive(player)) {
            deactivate(player);
            return false;
        }
        return activate(player);
    }

    public static boolean activate(ServerPlayer player) {
        if (isActive(player)) {
            return true;
        }
        if (!isEligible(player)) {
            player.displayClientMessage(Component.literal(
                    "§7Você precisa dominar os 4 elementos-base (Ar, Água, Terra e Fogo) antes de entrar no Avatar State."), true);
            return false;
        }

        PlayerAvatarData avatarData = player.getData(ModAttachments.AVATAR);
        if (!avatarData.isAvatarState()) {
            MoreBendingCommand.grantAvatarState(player.createCommandSourceStack().withSuppressedOutput(), player, avatarData);
            avatarData.setAvatarState(true);
        }

        ACTIVE.add(player.getUUID());
        applyBuffs(player);
        spawnActivationBurst(player);
        broadcastSync(player, true);
        player.displayClientMessage(Component.literal("§bVocê entrou no Avatar State!"), true);
        return true;
    }

    public static void deactivate(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }
        ACTIVE.remove(player.getUUID());

        PlayerAvatarData avatarData = player.getData(ModAttachments.AVATAR);
        if (avatarData.isAvatarState()) {
            MoreBendingCommand.revokeAvatarState(player, avatarData);
            avatarData.setAvatarState(false);
        }
        removeBuffs(player);
        broadcastSync(player, false);
        player.displayClientMessage(Component.literal("§7Você saiu do Avatar State."), true);
    }

    private static void applyBuffs(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_DURATION_TICKS, 0, true, false));
    }

    private static void removeBuffs(ServerPlayer player) {
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.REGENERATION);
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.WATER_BREATHING);
        // Speed é deixada decair sozinha (some em poucos segundos) pra não
        // cortar o movimento do jogador de forma abrupta ao desligar.
    }

    private static void broadcastSync(ServerPlayer player, boolean active) {
        SyncAvatarStatePacket packet = new SyncAvatarStatePacket(player.getUUID(), active);
        for (ServerPlayer online : player.getServer().getPlayerList().getPlayers()) {
            Dispatcher.sendToClient(packet, online);
        }
    }

    private static void spawnActivationBurst(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1.0, player.getZ(),
                1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                80, 0.6, 1.0, 0.6, 0.12);
        level.sendParticles(FIRE_DUST, player.getX(), player.getY() + 1.0, player.getZ(),
                50, 0.8, 0.4, 0.8, 0.0);
        level.sendParticles(WATER_DUST, player.getX(), player.getY() + 1.0, player.getZ(),
                50, 0.8, 1.2, 0.8, 0.0);
        level.sendParticles(AIR_DUST, player.getX(), player.getY() + 1.0, player.getZ(),
                40, 1.0, 0.8, 1.0, 0.0);
        // Chuva de blocos de terra explodindo pra fora na ativação -- some
        // sozinha (partícula BLOCK não fica presa, só voa e desaparece).
        for (int i = 0; i < 30; i++) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, randomEarthBlock()),
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    2, 0.7, 0.5, 0.7, 0.15);
        }
    }

    /**
     * Registrado via NeoForge.EVENT_BUS em ElementalsMoreBendingsMod. Reforça
     * os efeitos periodicamente (senão expirariam) e desenha os anéis
     * majestosos ao redor de quem está no Avatar State -- ver
     * {@link #spawnMajesticRings}.
     */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        Iterator<UUID> it = ACTIVE.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }
            if (player.tickCount % EFFECT_REFRESH_INTERVAL == 0) {
                applyBuffs(player);
            }
            spawnMajesticRings(player);
        }
    }

    // ==================== Efeito visual: 4 anéis majestosos ====================

    // Cores sólidas (independem de luz) de cada elemento -- usadas junto
    // com partículas "de verdade" (chama, água, poeira, nuvem) pra dar
    // volume sem perder a identidade de cada uma.
    private static final DustParticleOptions FIRE_DUST =
            new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.10f), 3.0f);
    private static final DustParticleOptions WATER_DUST =
            new DustParticleOptions(new Vector3f(0.20f, 0.50f, 1.0f), 3.4f);
    private static final DustParticleOptions EARTH_DUST =
            new DustParticleOptions(new Vector3f(0.42f, 0.30f, 0.16f), 2.4f);
    private static final DustParticleOptions AIR_DUST =
            new DustParticleOptions(new Vector3f(0.92f, 0.96f, 1.0f), 2.0f);

    // Blocos que aparecem girando no anel de Terra -- bem variados pra não
    // ficar um bloco só repetido, dá a sensação de "pedaços de chão"
    // arrancados e girando ao redor do corpo.
    private static final BlockState[] EARTH_BLOCKS = {
            Blocks.DIRT.defaultBlockState(),
            Blocks.COARSE_DIRT.defaultBlockState(),
            Blocks.STONE.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(),
            Blocks.ROOTED_DIRT.defaultBlockState(),
            Blocks.MOSS_BLOCK.defaultBlockState(),
    };

    // Pontos por volta do círculo -- Água e Ar bem densos de propósito pra
    // parecerem uma faixa CONTÍNUA (sem gaps entre os pontos), não uma
    // sequência de partículas soltas.
    private static final int POINTS_FIRE = 90;
    private static final int POINTS_WATER = 150;
    private static final int POINTS_EARTH = 90;
    private static final int POINTS_AIR = 150;

    // Raios bem maiores que antes -- os anéis ficam longe o suficiente do
    // corpo pra não poluir a visão de quem É o Avatar (primeira pessoa),
    // só visíveis "de fora"/em volta, tipo uma redoma.
    private static final double FIRE_RADIUS = 4.4;
    private static final double WATER_RADIUS = 5.8;
    private static final double EARTH_RADIUS = 5.1;
    private static final double AIR_RADIUS = 7.0;

    // Terra ganha um jitter de posição pra dar espessura de verdade
    // (pedaços soltos tombando), não uma linha fina.
    private static final double EARTH_JITTER = 0.4;

    private static final double FIRE_TILT = Math.toRadians(12);   // quase deitado, na cintura
    private static final double WATER_TILT = Math.toRadians(68);  // quase em pé, subindo alto
    private static final double EARTH_TILT = Math.toRadians(40);  // diagonal, cruzando os outros
    private static final double AIR_TILT = Math.toRadians(-55);   // diagonal oposta, o mais alto de todos

    /**
     * Os 4 elementos-base girando ao redor do corpo inteiro, bem afastados
     * (raios grandes, ver constantes acima) pra não atrapalhar a visão de
     * quem está no Avatar State. Cada elemento tem uma linguagem visual
     * própria, parecida com a referência (Avatar: A Lenda de Aang):
     * <ul>
     *     <li><b>Água</b> -- UMA faixa contínua só (sem camada duplicada),
     *     composta majoritariamente de gotas de água de verdade
     *     (FALLING_WATER/SPLASH) em vez de poeira quadrada -- isso é o que
     *     elimina a "nuvem de cubos azuis" poluída de antes. WATER_DUST
     *     entra só como um terço dos pontos, pra dar corpo/cor sólida sem
     *     dominar o visual.</li>
     *     <li><b>Fogo</b> -- bem denso e chamativo: mais pontos no anel +
     *     mais partículas por ponto (chama, lava e brilho de cor
     *     misturados), pra ficar bem "berrante".</li>
     *     <li><b>Terra</b> -- vários blocos de verdade girando, com jitter
     *     de posição pra parecer pedaços soltos tombando, não uma linha
     *     perfeita.</li>
     *     <li><b>Ar</b> -- as mesmas partículas de fumaça de antes, só que
     *     com rate BEM maior (mais pontos + mais partículas por ponto).</li>
     * </ul>
     */
    private static void spawnMajesticRings(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double baseY = player.getY() + 1.0;
        double t = player.tickCount;
        double spinFire = Math.toRadians(t * 3.4);
        double spinWater = Math.toRadians(-t * 2.2);
        double spinEarth = Math.toRadians(t * 1.6);
        double spinAir = Math.toRadians(-t * 4.2);

        // ---- Fogo: bem denso e chamativo, mas SÓ fogo puro -- sem LAVA
        // (que solta fuligem/fumaça cinza como efeito colateral) nem
        // FIRE_DUST (poeira quadrada). Só FLAME + SMALL_FLAME, mais
        // pontos (POINTS_FIRE) e 3 partículas por ponto.
        drawElementalRing(level, player, baseY, FIRE_RADIUS, FIRE_TILT, spinFire, POINTS_FIRE,
                i -> (i % 4 == 0) ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, 3);

        // ---- Água: UMA faixa contínua só, feita majoritariamente de gotas
        // de água de verdade (chunk d'água) -- não mais duas camadas de
        // poeira quadrada empilhadas, que era o que poluía a tela na
        // ativação. WATER_DUST some pra dar cor sólida só 1 em cada 3
        // pontos; o resto é água "de verdade" caindo/splashando.
        drawElementalRing(level, player, baseY, WATER_RADIUS, WATER_TILT, spinWater, POINTS_WATER,
                i -> (i % 9 == 0) ? ParticleTypes.SPLASH
                        : (i % 3 == 0) ? WATER_DUST
                        : ParticleTypes.FALLING_WATER, 2);

        // ---- Terra: vários blocos tombando, com jitter de posição ----
        drawElementalRingJittered(level, player, baseY, EARTH_RADIUS, EARTH_TILT, spinEarth, POINTS_EARTH,
                i -> (i % 6 == 0) ? EARTH_DUST : new BlockParticleOption(ParticleTypes.BLOCK, randomEarthBlock()),
                EARTH_JITTER);

        // ---- Ar: mesma fumaça de antes, rate BEM maior -- mais pontos
        // (POINTS_AIR) + 5 partículas por ponto (era 3).
        drawElementalRing(level, player, baseY, AIR_RADIUS, AIR_TILT, spinAir, POINTS_AIR,
                i -> (i % 5 == 0) ? AIR_DUST : ParticleTypes.CLOUD, 5);

        // Brilhos de destaque correndo pelos anéis -- reforça a sensação
        // de "energia" cruzando o corpo, igual as fagulhas claras na
        // referência.
        if (t % 4 == 0) {
            spawnHighlight(level, player, baseY, FIRE_RADIUS, FIRE_TILT, spinFire, ParticleTypes.FLAME);
            spawnHighlight(level, player, baseY, WATER_RADIUS, WATER_TILT, spinWater, ParticleTypes.END_ROD);
            spawnHighlight(level, player, baseY, AIR_RADIUS, AIR_TILT, spinAir, ParticleTypes.END_ROD);
        }
    }

    private static BlockState randomEarthBlock() {
        return EARTH_BLOCKS[ThreadLocalRandom.current().nextInt(EARTH_BLOCKS.length)];
    }

    private interface RingParticleFactory {
        net.minecraft.core.particles.ParticleOptions particleFor(int index);
    }

    /** @param particlesPerPoint quantas partículas mandar em cada ponto do anel (rate/densidade visual). */
    private static void drawElementalRing(ServerLevel level, ServerPlayer player, double baseY,
                                          double radius, double tilt, double spin, int points,
                                          RingParticleFactory factory, int particlesPerPoint) {
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            sendRingPoint(level, player, baseY, factory.particleFor(i), radius, tilt, spin, angle, particlesPerPoint);
        }
    }

    /**
     * Igual a {@link #drawElementalRing}, mas com um pequeno deslocamento
     * ALEATÓRIO (recalculado a cada tick) no raio e na altura de cada
     * ponto -- usado só pela Terra, pra parecer pedaços de chão soltos
     * tombando ao redor do corpo em vez de uma linha perfeitamente lisa.
     */
    private static void drawElementalRingJittered(ServerLevel level, ServerPlayer player, double baseY,
                                                  double radius, double tilt, double spin, int points,
                                                  RingParticleFactory factory, double jitter) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double jitteredRadius = radius + random.nextDouble(-jitter, jitter);
            sendRingPoint(level, player, baseY + random.nextDouble(-jitter, jitter),
                    factory.particleFor(i), jitteredRadius, tilt, spin, angle, 1);
        }
    }

    private static void spawnHighlight(ServerLevel level, ServerPlayer player, double baseY,
                                       double radius, double tilt, double spin,
                                       net.minecraft.core.particles.ParticleOptions type) {
        // Dois pontos opostos no anel, pra parecer um brilho correndo dos
        // dois lados ao mesmo tempo.
        sendRingPoint(level, player, baseY, type, radius, tilt, spin, 0, 1);
        sendRingPoint(level, player, baseY, type, radius, tilt, spin, Math.PI, 1);
    }

    private static void sendRingPoint(ServerLevel level, ServerPlayer player, double baseY,
                                      net.minecraft.core.particles.ParticleOptions particle,
                                      double radius, double tilt, double spin, double angle,
                                      int count) {
        // Círculo "deitado" no plano XZ, inclinado (tilt) pra levantar um
        // lado em direção ao céu, depois girado (spin) em torno do eixo
        // vertical -- é isso que dá o efeito de anel inclinado rodando.
        double localX = radius * Math.cos(angle);
        double localY = radius * Math.sin(angle) * Math.sin(tilt);
        double localZ = radius * Math.sin(angle) * Math.cos(tilt);

        double worldX = localX * Math.cos(spin) - localZ * Math.sin(spin);
        double worldZ = localX * Math.sin(spin) + localZ * Math.cos(spin);

        // count > 1 espalha um pouquinho as partículas extras (spread
        // pequeno) em vez de empilhar todas exatamente no mesmo pixel --
        // é isso que dá a sensação de "mais denso"/"rate maior" (usado no
        // Fogo e no Ar) sem parecer um bug de partícula duplicada.
        double spread = count > 1 ? 0.10 : 0.0;
        level.sendParticles(particle,
                player.getX() + worldX, baseY + localY, player.getZ() + worldZ,
                count, spread, spread, spread, 0.0);
    }

    /** Limpa o UUID de quem desconecta com o Avatar State ligado (sem tentar revogar bendings -- já persistem no NBT). */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ACTIVE.remove(sp.getUUID());
        }
    }
}