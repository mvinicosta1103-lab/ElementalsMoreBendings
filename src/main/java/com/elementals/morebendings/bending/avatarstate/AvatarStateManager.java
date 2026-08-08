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
import com.mojang.math.Transformation;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntFunction;

/**
 * Avatar State "de verdade" -- ligado pelo próprio jogador (tecla, ver
 * {@code ToggleAvatarStatePacket}), diferente de {@code /morebending
 * avatar} (que é o comando de operador pra forçar ligar/desligar em
 * qualquer um). Reaproveita {@link MoreBendingCommand#grantAvatarState}/
 * {@link MoreBendingCommand#revokeAvatarState} pra conceder/revogar as
 * bendings (mesmo rastreamento via {@link PlayerAvatarData}, então nunca
 * tira algo que o jogador já tinha por fora), e por cima disso aplica o
 * "boost" (efeitos de status) e o efeito visual (4 anéis de blocos reais
 * girando + olhos brilhantes, ver {@code AvatarStateEyesLayer}).
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
        spawnAllRings(player);
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
        removeAllRings(player);

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
        // Chuva de blocos de terra explodindo pra fora na ativação -- isso
        // aqui continua sendo só partícula de impacto (BlockParticleOption),
        // não faz parte do anel -- some sozinha, é só o "boom" inicial.
        for (int i = 0; i < 30; i++) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, randomEarthBlock()),
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    2, 0.7, 0.5, 0.7, 0.15);
        }
    }

    /**
     * Registrado via NeoForge.EVENT_BUS em ElementalsMoreBendingsMod. Reforça
     * os efeitos periodicamente (senão expirariam) e atualiza os 4 anéis de
     * blocos reais ao redor de quem está no Avatar State -- ver
     * {@link #updateAllRings}.
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
            updateAllRings(player);
        }
    }

    // ==================== Efeito visual: 4 anéis de blocos reais ====================
    //
    // Os 4 elementos-base giram ao redor do corpo inteiro como blocos DE
    // VERDADE (Display.BlockDisplay), não partícula/falling_dust. O ANEL EM
    // SI NÃO GIRA -- cada bloco fica numa posição fixa (ângulo fixo dentro
    // do círculo, calculado só a partir do índice dele, sem nenhum termo de
    // tempo). O que gira, e rápido, é cada bloco individualmente em torno
    // do PRÓPRIO eixo (own-spin), com um eixo diferente por elemento:
    //   - Ar    -> eixo X  (giro "vertical", tipo roda de bicicleta em pé)
    //   - Água  -> eixo Y  (giro "horizontal", tipo pião/disco girando)
    //   - Terra -> eixo diagonal (-1,1,0) (diagonal pra esquerda)
    //   - Fogo  -> eixo diagonal ( 1,1,0) (diagonal pra direita)

    private enum RingElement { FIRE, WATER, EARTH, AIR }

    /** Cores sólidas usadas só no burst de ativação (não fazem parte do anel). */
    private static final DustParticleOptions FIRE_DUST =
            new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.10f), 3.0f);
    private static final DustParticleOptions WATER_DUST =
            new DustParticleOptions(new Vector3f(0.20f, 0.50f, 1.0f), 3.4f);
    private static final DustParticleOptions AIR_DUST =
            new DustParticleOptions(new Vector3f(0.92f, 0.96f, 1.0f), 2.0f);

    // ---- Partículas de ACENTO por cima dos blocos do anel de Água (não
    // substituem os blocos, só reforçam a identidade visual) ----
    // Fumaça azul -- mistura SMOKE puro (sem cor própria) com um tint
    // azul (WATER_RING_SMOKE_TINT), pra dar a sensação de neblina em vez
    // de "cubo colorido flutuando". Rate alto = dispara em quase todo
    // índice do anel, não só uma fração pequena.
    private static final DustParticleOptions WATER_RING_SMOKE_TINT =
            new DustParticleOptions(new Vector3f(0.35f, 0.55f, 0.95f), 1.8f);

    // Blocos variados -- só Água e Terra ainda usam blocos de verdade.
    // Fogo e Ar viraram 100% partícula (ver PARTICLE_CONFIGS).
    private static final BlockState[] EARTH_BLOCKS = {
            Blocks.DIRT.defaultBlockState(),
            Blocks.COARSE_DIRT.defaultBlockState(),
            Blocks.STONE.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState(),
            Blocks.ROOTED_DIRT.defaultBlockState(),
            Blocks.MOSS_BLOCK.defaultBlockState(),
    };
    private static final BlockState WATER_BLOCK = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();

    // Raios/inclinações dos planos dos anéis -- controla o "desenho" de
    // cada anel ao redor do corpo (não tem nada a ver com o own-spin).
    private static final double FIRE_RADIUS = 4.4;
    private static final double WATER_RADIUS = 5.8;
    private static final double EARTH_RADIUS = 5.1;
    private static final double AIR_RADIUS = 7.0;

    private static final double FIRE_TILT = Math.toRadians(12);   // quase deitado, na cintura
    private static final double WATER_TILT = Math.toRadians(68);  // quase em pé, subindo alto
    private static final double EARTH_TILT = Math.toRadians(40);  // diagonal, cruzando os outros
    private static final double AIR_TILT = Math.toRadians(-55);   // diagonal oposta, o mais alto de todos

    /**
     * Configuração fixa de cada anel. {@code spinAxis} é o eixo (já
     * normalizado) em torno do qual CADA BLOCO gira em torno de si mesmo;
     * {@code spinDegPerTick} é a velocidade desse giro (bem alta = "muito
     * rápido" como pedido). {@code jitterRadius}/{@code jitterHeight}
     * dão variação fixa por índice (só a Terra usa, pra parecer pedaços
     * de chão desalinhados -- mas sem recalcular a cada tick, senão o
     * bloco ficaria "tremendo" em vez de só girando).
     */
    private record RingConfig(double radius, double tilt, int count, float scale,
                              Vector3f spinAxis, double spinDegPerTick,
                              IntFunction<BlockState> blockAt,
                              double jitterRadius, double jitterHeight) {
    }

    // Só Água e Terra usam blocos de verdade agora.
    private static final List<RingElement> BLOCK_ELEMENTS = List.of(RingElement.WATER, RingElement.EARTH);

    private static final Map<RingElement, RingConfig> CONFIGS = new EnumMap<>(RingElement.class);
    static {
        CONFIGS.put(RingElement.WATER, new RingConfig(
                WATER_RADIUS, WATER_TILT, 26, 0.40f,
                new Vector3f(0f, 1f, 0f), 30,
                i -> WATER_BLOCK,
                0.0, 0.0));
        CONFIGS.put(RingElement.EARTH, new RingConfig(
                EARTH_RADIUS, EARTH_TILT, 32, 0.45f,
                new Vector3f(-1f, 1f, 0f).normalize(), 26,
                i -> EARTH_BLOCKS[i % EARTH_BLOCKS.length],
                0.4, 0.25));
    }

    /**
     * Configuração dos anéis 100% partícula (Fogo e Ar) -- sem entidade
     * nenhuma, só {@code level.sendParticles} todo tick, na posição FIXA
     * do anel (mesmo princípio de antes: sem termo de tempo na posição,
     * só a densidade/rate é alta). {@code particlesPerPoint} é o que dá
     * o "rate alto" pedido -- cada ponto do anel dispara várias
     * partículas por tick, não só uma.
     */
    private record ParticleRingConfig(double radius, double tilt, int count, int particlesPerPoint) {
    }

    private static final Map<RingElement, ParticleRingConfig> PARTICLE_CONFIGS = new EnumMap<>(RingElement.class);
    static {
        PARTICLE_CONFIGS.put(RingElement.FIRE, new ParticleRingConfig(FIRE_RADIUS, FIRE_TILT, 70, 3));
        PARTICLE_CONFIGS.put(RingElement.AIR, new ParticleRingConfig(AIR_RADIUS, AIR_TILT, 90, 4));
    }

    private static final Map<RingElement, Map<UUID, List<Display.BlockDisplay>>> CHUNKS = new EnumMap<>(RingElement.class);
    static {
        for (RingElement element : BLOCK_ELEMENTS) {
            CHUNKS.put(element, new java.util.HashMap<>());
        }
    }

    private static void spawnAllRings(ServerPlayer player) {
        for (RingElement element : BLOCK_ELEMENTS) {
            spawnRing(element, player);
        }
    }

    private static void removeAllRings(ServerPlayer player) {
        for (RingElement element : BLOCK_ELEMENTS) {
            removeRing(element, player);
        }
    }

    private static void updateAllRings(ServerPlayer player) {
        double baseY = player.getY() + 1.0;
        for (RingElement element : BLOCK_ELEMENTS) {
            updateRing(element, player, baseY);
        }
        drawParticleRing(RingElement.FIRE, player, baseY);
        drawParticleRing(RingElement.AIR, player, baseY);
    }

    /**
     * Anel 100% partícula (Fogo/Ar) -- posição fixa por índice (sem
     * orbitar, mesmo princípio dos anéis de bloco), rate alto de
     * partículas por ponto. Fogo = só FLAME/SMALL_FLAME (nunca LAVA). Ar
     * = só WHITE_SMOKE (fumaça branca pura, nada de dust/bloco).
     */
    private static void drawParticleRing(RingElement element, ServerPlayer player, double baseY) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ParticleRingConfig config = PARTICLE_CONFIGS.get(element);
        int count = config.count();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double localX = config.radius() * Math.cos(angle);
            double localY = config.radius() * Math.sin(angle) * Math.sin(config.tilt());
            double localZ = config.radius() * Math.sin(angle) * Math.cos(config.tilt());
            double x = player.getX() + localX;
            double y = baseY + localY;
            double z = player.getZ() + localZ;

            net.minecraft.core.particles.ParticleOptions particle = switch (element) {
                case FIRE -> (i % 5 == 0) ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME;
                case AIR -> ParticleTypes.WHITE_SMOKE;
                default -> ParticleTypes.CLOUD; // nunca usado (só FIRE/AIR chamam este método)
            };

            level.sendParticles(particle, x, y, z, config.particlesPerPoint(), 0.06, 0.06, 0.06, 0.01);
        }
    }

    /**
     * Partícula de acento tocada na posição atual (real, do tick) de um
     * bloco do anel. Usa {@code index % N} pra não disparar em TODOS os
     * blocos todo tick (ia virar uma nuvem sólida) -- só numa fração
     * deles, o que já basta pra reforçar a identidade do elemento sem
     * abafar os blocos.
     */
    private static void spawnRingAccent(RingElement element, ServerLevel level, int index,
                                        double x, double y, double z) {
        switch (element) {
            case WATER -> {
                // Fumaça azul com RATE ALTO agora -- dispara em quase
                // todo bloco do anel (era 1 a cada 4/8, agora 1 a cada
                // 2/3), alternando SMOKE puro (neblina) com o tint azul
                // (WATER_RING_SMOKE_TINT) pra não virar um borrão sólido
                // de cor por cima dos blocos.
                if (index % 2 == 0) {
                    level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.06, 0.09, 0.06, 0.006);
                }
                if (index % 3 == 0) {
                    level.sendParticles(WATER_RING_SMOKE_TINT, x, y, z, 1, 0.06, 0.06, 0.06, 0.0);
                }
            }
            case EARTH -> {
                // Terra fica só com os blocos de verdade, sem acento --
                // não foi pedido e ia poluir a leitura de "pedra tombando".
            }
            default -> {
                // Fogo e Ar não passam mais por aqui -- viraram anéis
                // 100% partícula, ver drawParticleRing.
            }
        }
    }

    private static void spawnRing(RingElement element, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        removeRing(element, player); // por garantia, nunca duplica se já tinha algo sobrando
        RingConfig config = CONFIGS.get(element);
        List<Display.BlockDisplay> chunks = new ArrayList<>(config.count());
        for (int i = 0; i < config.count(); i++) {
            chunks.add(newRingChunkEntity(level, player, config.blockAt().apply(i)));
        }
        CHUNKS.get(element).put(player.getUUID(), chunks);
    }

    private static Display.BlockDisplay newRingChunkEntity(ServerLevel level, ServerPlayer player, BlockState state) {
        Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
        applyBlockState(display, state);
        display.setNoGravity(true);
        display.setPos(player.getX(), player.getY() + 1.0, player.getZ());
        level.addFreshEntity(display);
        return display;
    }

    private static void removeRing(RingElement element, ServerPlayer player) {
        List<Display.BlockDisplay> chunks = CHUNKS.get(element).remove(player.getUUID());
        if (chunks == null) {
            return;
        }
        for (Display.BlockDisplay display : chunks) {
            display.discard();
        }
    }

    /**
     * Reposiciona (na posição FIXA do anel, sem termo de tempo -- é isso
     * que faz o anel em si não girar) e aplica o own-spin rápido em cada
     * bloco do anel deste elemento.
     */
    private static void updateRing(RingElement element, ServerPlayer player, double baseY) {
        Map<UUID, List<Display.BlockDisplay>> storage = CHUNKS.get(element);
        List<Display.BlockDisplay> chunks = storage.get(player.getUUID());
        if (chunks == null) {
            spawnRing(element, player);
            chunks = storage.get(player.getUUID());
            if (chunks == null) {
                return;
            }
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        RingConfig config = CONFIGS.get(element);
        double t = player.tickCount;
        int count = chunks.size();

        for (int i = 0; i < count; i++) {
            Display.BlockDisplay display = chunks.get(i);
            if (display.isRemoved()) {
                // Chunk sumiu (chunk do mundo descarregou, etc.) -- recria
                // no lugar pra nunca ficar faltando um pedaço do anel.
                display = newRingChunkEntity(level, player, config.blockAt().apply(i));
                chunks.set(i, display);
            }

            // ---- Posição: SÓ depende do índice (ângulo fixo) + jitter
            // fixo por índice. Nenhum termo de tempo aqui -- é isso que
            // mantém o anel parado na posição, sem orbitar.
            double angle = (2 * Math.PI * i) / count;
            double jitterR = config.jitterRadius() != 0.0
                    ? config.jitterRadius() * Math.sin(i * 2.399963) : 0.0;
            double jitterH = config.jitterHeight() != 0.0
                    ? config.jitterHeight() * Math.cos(i * 1.618034) : 0.0;
            double radius = config.radius() + jitterR;

            double localX = radius * Math.cos(angle);
            double localY = radius * Math.sin(angle) * Math.sin(config.tilt());
            double localZ = radius * Math.sin(angle) * Math.cos(config.tilt());

            double worldX = player.getX() + localX;
            double worldY = baseY + localY + jitterH;
            double worldZ = player.getZ() + localZ;
            display.setPos(worldX, worldY, worldZ);
            spawnRingAccent(element, level, i, worldX, worldY, worldZ);

            // ---- Own-spin: aqui sim tem termo de tempo (t), bem rápido
            // (spinDegPerTick alto), em torno do eixo específico do
            // elemento. Um pequeno offset de fase por índice evita que
            // todos os blocos girem exatamente em sincronia (fica mais
            // orgânico), mas não afeta a posição, só a rotação visual.
            float phase = (float) Math.toRadians(t * config.spinDegPerTick() + i * 17);
            Vector3f axis = config.spinAxis();
            Quaternionf rotation = new Quaternionf().rotateAxis(phase, axis.x, axis.y, axis.z);

            float scale = config.scale();
            Transformation transformation = new Transformation(
                    new Vector3f(-scale / 2f, -scale / 2f, -scale / 2f),
                    rotation,
                    new Vector3f(scale),
                    new Quaternionf());
            applyTransformation(display, transformation);
        }
    }

    /**
     * {@code BlockDisplay#setBlockState} e {@code Display#setTransformation}
     * não são públicos (o vanilla só espera que sejam chamados de dentro do
     * próprio pacote {@code net.minecraft.world.entity} ou via NBT/comando).
     * Como o mod está em outro pacote, chamamos via reflection pra contornar
     * isso -- os métodos existem de verdade (foi confirmado pelo próprio
     * erro de compilação, "has private access"/"cannot find symbol" só
     * porque não é visível daqui), então isso é seguro e estável entre
     * builds, só não entre versões do Minecraft que renomeiem o método.
     */
    private static final Method BLOCK_DISPLAY_SET_BLOCK_STATE;
    private static final Method DISPLAY_SET_TRANSFORMATION;

    static {
        try {
            BLOCK_DISPLAY_SET_BLOCK_STATE = Display.BlockDisplay.class.getDeclaredMethod("setBlockState", BlockState.class);
            BLOCK_DISPLAY_SET_BLOCK_STATE.setAccessible(true);
            DISPLAY_SET_TRANSFORMATION = Display.class.getDeclaredMethod("setTransformation", Transformation.class);
            DISPLAY_SET_TRANSFORMATION.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void applyBlockState(Display.BlockDisplay display, BlockState state) {
        try {
            BLOCK_DISPLAY_SET_BLOCK_STATE.invoke(display, state);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Falha ao aplicar block state no chunk do anel", e);
        }
    }

    private static void applyTransformation(Display display, Transformation transformation) {
        try {
            DISPLAY_SET_TRANSFORMATION.invoke(display, transformation);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Falha ao aplicar transformação no chunk do anel", e);
        }
    }

    private static BlockState randomEarthBlock() {
        return EARTH_BLOCKS[ThreadLocalRandom.current().nextInt(EARTH_BLOCKS.length)];
    }

    /** Limpa o UUID de quem desconecta com o Avatar State ligado (sem tentar revogar bendings -- já persistem no NBT). */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ACTIVE.remove(sp.getUUID());
            removeAllRings(sp);
        }
    }
}