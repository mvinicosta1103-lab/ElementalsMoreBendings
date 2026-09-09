package com.elementals.morebendings.bending.avatarstate;

import com.elementals.morebendings.commands.MoreBendingCommand;
import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
import com.elementals.morebendings.bending.avatarstate.moves.EnergyElement;
import com.elementals.morebendings.data.PlayerAvatarData;
import com.elementals.morebendings.network.packets.SyncAvatarStatePacket;
import com.elementals.morebendings.registry.ModAttachments;
import commonnetwork.api.Dispatcher;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.air.AirElement;
import dev.saperate.elementals.elements.earth.EarthElement;
import dev.saperate.elementals.elements.fire.FireElement;
import dev.saperate.elementals.elements.water.WaterElement;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.elements.NoneElement;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
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

    // Anéis desligados individualmente (por elemento) pelo próprio
    // jogador enquanto está no Avatar State -- ver #toggleRing. Vazio ou
    // sem entrada = todos ligados. Resetado (removido) sempre que o
    // Avatar State inteiro desliga, pra nunca "vazar" pra próxima
    // ativação.
    private static final Map<UUID, Set<RingElement>> DISABLED_RINGS = new HashMap<>();

    // ---- Timer de 60s dos anéis "automáticos" ----
    //
    // Ao ativar o Avatar State, os 4 anéis ligam sozinhos (como sempre
    // ligaram) só que agora por tempo limitado: se o jogador não mexer
    // MANUALMENTE (keybind) num anel específico dentro desses 60s, ele
    // se desliga sozinho quando o tempo acaba. Isso é o que dá
    // "individualidade" aos anéis pedida pelo usuário: assim que o
    // jogador toca a keybind de um anel (pra ligar OU desligar), aquele
    // anel específico sai do timer automático de vez -- passa a ficar
    // só sob controle manual, na posição que o jogador deixou, até a
    // próxima vez que ele apertar a tecla de novo (sem prazo nenhum).

    /** 60 segundos (1200 ticks) -- duração do período automático dos anéis. */
    private static final int RING_AUTO_DURATION_TICKS = 1200;

    /** Tick (Entity#tickCount do jogador) em que o período automático
     * expira. Removido do mapa assim que o desligamento automático
     * acontece (ou quando o Avatar State inteiro desliga/reativa) --
     * nunca dispara duas vezes pra mesma ativação. */
    private static final Map<UUID, Long> RING_AUTO_EXPIRE_TICK = new HashMap<>();

    /** Anéis que o jogador já tocou manualmente (keybind) desde a
     * última ativação -- ficam imunes ao desligamento automático de
     * 60s, esteja o anel ligado ou desligado no momento. */
    private static final Map<UUID, Set<RingElement>> MANUAL_RINGS = new HashMap<>();

    // ---- Progressão (ver AvatarProgressionLevel) ----
    //
    // Cada ativação "trava" o nível de progressão que o jogador tinha NO
    // MOMENTO de entrar (ACTIVE_ENTRY_LEVEL) -- é esse nível travado que
    // decide a duração desta ativação específica e o cooldown aplicado
    // quando ela terminar, mesmo que #activate seja chamado de novo (por
    // instinto de sobrevivência) e o contador mude no meio do caminho.

    /** Nível travado da ativação atual de cada jogador -- ver acima. Removido em #finishDeactivation. */
    private static final Map<UUID, AvatarProgressionLevel> ACTIVE_ENTRY_LEVEL = new HashMap<>();

    /** Tick do mundo (level#getGameTime) em que a ativação atual começou -- ausente = sem limite de duração nesta ativação (Mestre) ou não está ativo. */
    private static final Map<UUID, Long> ACTIVATION_START_TICK = new HashMap<>();

    /** Tick do mundo até quando a entrada MANUAL (tecla) fica bloqueada por cooldown. Ausente ou já expirado = sem cooldown pendente. */
    private static final Map<UUID, Long> COOLDOWN_UNTIL_TICK = new HashMap<>();

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

    /** @return o nível de progressão ATUAL do jogador (baseado no histórico -- ver {@code PlayerAvatarData#getTransformationCount}). */
    public static AvatarProgressionLevel progressionLevel(ServerPlayer player) {
        int count = player.getData(ModAttachments.AVATAR).getTransformationCount();
        return AvatarProgressionLevel.forTransformationCount(count);
    }

    /** @return ticks restantes de cooldown antes da entrada manual liberar de novo (0 = sem cooldown pendente). */
    public static long cooldownRemainingTicks(ServerPlayer player) {
        Long until = COOLDOWN_UNTIL_TICK.get(player.getUUID());
        if (until == null || !(player.level() instanceof ServerLevel level)) {
            return 0L;
        }
        return Math.max(0L, until - level.getGameTime());
    }

    /** @return ticks restantes até a ativação ATUAL desligar sozinha, ou {@code -1} se não estiver ativo ou não tiver limite de duração (Mestre). */
    public static long durationRemainingTicks(ServerPlayer player) {
        Long start = ACTIVATION_START_TICK.get(player.getUUID());
        AvatarProgressionLevel entryLevel = ACTIVE_ENTRY_LEVEL.get(player.getUUID());
        if (start == null || entryLevel == null || !entryLevel.hasDurationLimit()
                || !(player.level() instanceof ServerLevel level)) {
            return -1L;
        }
        return Math.max(0L, entryLevel.getDurationTicks() - (level.getGameTime() - start));
    }

    /** Formata ticks (20/s) como "Xm Ys" (ou só "Ys" se menos de um minuto), pras mensagens de cooldown/duração. */
    private static String formatTicksAsTime(long ticks) {
        long totalSeconds = (ticks + 19) / 20; // arredonda pra cima -- nunca mostra "0s" com cooldown ainda ativo
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }

    /** @return o novo estado (true = ligou, false = desligou/não conseguiu ligar). */
    public static boolean toggle(ServerPlayer player) {
        if (isActive(player)) {
            deactivate(player);
            return false;
        }
        return activateManual(player);
    }

    /**
     * Entrada VOLUNTÁRIA pela tecla (ver {@code ToggleAvatarStatePacket}) --
     * diferente de {@link #activate} puro (chamado também pelo instinto de
     * sobrevivência em {@link AvatarNearDeathGuardian}, que NUNCA passa por
     * aqui, então a emergência sempre funciona independente de nível ou
     * cooldown). Aqui sim: só libera se o nível atual permitir entrada
     * manual (ver {@link AvatarProgressionLevel#allowsManualEntry}) e não
     * houver cooldown pendente da ativação anterior.
     */
    public static boolean activateManual(ServerPlayer player) {
        if (isActive(player)) {
            return true;
        }
        if (!isEligible(player)) {
            player.displayClientMessage(Component.literal(
                    "§7You need to master all 4 base elements (Air, Water, Earth, and Fire) before entering the Avatar State."), true);
            return false;
        }

        AvatarProgressionLevel level = progressionLevel(player);
        if (!level.allowsManualEntry()) {
            player.displayClientMessage(Component.literal(
                    "§7You haven't mastered the Avatar State yet -- for now, only your survival instinct (near death) can trigger it."), true);
            return false;
        }

        long cooldownLeft = cooldownRemainingTicks(player);
        if (cooldownLeft > 0) {
            player.displayClientMessage(Component.literal(
                    "§7The Avatar State is still recovering -- " + formatTicksAsTime(cooldownLeft) + " left."), true);
            return false;
        }

        return activate(player);
    }

    /**
     * Liga/desliga o anel de UM elemento específico enquanto o jogador
     * está no Avatar State (ver {@code ToggleFireRingPacket} e as outras
     * 3 packets equivalentes). Não faz nada (retorna {@code null}) se o
     * jogador nem está no Avatar State -- não tem anel nenhum ativo pra
     * mexer. O estado é lembrado em {@link #DISABLED_RINGS} e respeitado
     * ali até o próximo {@link #updateAllRings}.
     *
     * @return o novo estado do anel (true = ligado, false = desligado),
     * ou {@code null} se o jogador não está no Avatar State.
     */
    public static Boolean toggleRing(ServerPlayer player, RingElement element) {
        if (!isActive(player)) {
            return null;
        }
        // A partir do momento que a keybind é usada, este anel específico
        // sai do timer automático de 60s (ver #checkRingAutoExpire) --
        // fica só sob controle manual dali em diante, ligado ou desligado.
        MANUAL_RINGS.computeIfAbsent(player.getUUID(), id -> EnumSet.noneOf(RingElement.class)).add(element);
        Set<RingElement> disabled = DISABLED_RINGS.computeIfAbsent(player.getUUID(), id -> EnumSet.noneOf(RingElement.class));
        boolean nowEnabled;
        if (disabled.contains(element)) {
            disabled.remove(element);
            nowEnabled = true;
        } else {
            disabled.add(element);
            nowEnabled = false;
            if (BLOCK_ELEMENTS.contains(element)) {
                // Anéis de bloco têm entidade de verdade -- some na hora.
                // Anéis de partícula (Fogo/Ar) não têm entidade, só param
                // de ser desenhados no próximo tick (ver updateAllRings).
                removeRing(element, player);
            }
        }
        return nowEnabled;
    }

    private static boolean isRingEnabled(ServerPlayer player, RingElement element) {
        Set<RingElement> disabled = DISABLED_RINGS.get(player.getUUID());
        return disabled == null || !disabled.contains(element);
    }

    public static boolean activate(ServerPlayer player) {
        if (isActive(player)) {
            return true;
        }
        if (!isEligible(player)) {
            player.displayClientMessage(Component.literal(
                    "§7You need to master all 4 base elements (Air, Water, Earth, and Fire) before entering the Avatar State."), true);
            return false;
        }

        PlayerAvatarData avatarData = player.getData(ModAttachments.AVATAR);
        Bender bender = Bender.getBender(player);
        if (!avatarData.isAvatarState()) {
            lockCurrentBendingsAndGrantAvatar(player, bender, avatarData);
            avatarData.setAvatarState(true);
        }

        // Trava o nível de progressão PRA ESTA ativação (com base no
        // histórico ATÉ AGORA, antes de contar a transformação que está
        // começando) -- é ele que decide a duração desta sessão e o
        // cooldown quando ela terminar, ver AvatarProgressionLevel.
        AvatarProgressionLevel entryLevel = AvatarProgressionLevel.forTransformationCount(avatarData.getTransformationCount());
        avatarData.incrementTransformationCount();
        ACTIVE_ENTRY_LEVEL.put(player.getUUID(), entryLevel);
        if (entryLevel.hasDurationLimit() && player.level() instanceof ServerLevel level) {
            ACTIVATION_START_TICK.put(player.getUUID(), level.getGameTime());
        } else {
            ACTIVATION_START_TICK.remove(player.getUUID());
        }
        COOLDOWN_UNTIL_TICK.remove(player.getUUID()); // ativação bem sucedida -- qualquer cooldown residual não se aplica mais

        ACTIVE.add(player.getUUID());
        MANUAL_RINGS.remove(player.getUUID()); // nenhum anel foi tocado manualmente ainda nesta ativação
        RING_AUTO_EXPIRE_TICK.put(player.getUUID(), player.tickCount + (long) RING_AUTO_DURATION_TICKS);
        applyBuffs(player);
        grantAvatarFlight(player);
        spawnAllRings(player);
        spawnActivationBurst(player);
        broadcastSync(player, true);
        player.displayClientMessage(Component.literal(
                "§bYou entered the Avatar State (" + entryLevel.getDisplayName()
                        + ")! Your other bendings are locked -- only Avatar and Energy are available to cycle."), true);
        return true;
    }

    public static void deactivate(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }
        ACTIVE.remove(player.getUUID());
        finishDeactivation(player);
    }

    /**
     * Limpeza compartilhada de "sair do Avatar State" -- chamada tanto por
     * {@link #deactivate} (saída pela tecla) quanto pelo laço de {@link
     * #onServerTick} quando a duração expira sozinha. Nunca mexe em {@link
     * #ACTIVE} -- cada chamador já cuidou disso do jeito certo pra sua
     * situação (direto vs. via {@code Iterator#remove}, pra nunca
     * disparar {@code ConcurrentModificationException} durante o tick).
     */
    private static void finishDeactivation(ServerPlayer player) {
        removeAllRings(player);
        DISABLED_RINGS.remove(player.getUUID());
        MANUAL_RINGS.remove(player.getUUID());
        RING_AUTO_EXPIRE_TICK.remove(player.getUUID());
        ACTIVATION_START_TICK.remove(player.getUUID());
        AvatarProgressionLevel enteredLevel = ACTIVE_ENTRY_LEVEL.remove(player.getUUID());

        PlayerAvatarData avatarData = player.getData(ModAttachments.AVATAR);
        if (avatarData.isAvatarState()) {
            restoreLockedBendings(player, Bender.getBender(player), avatarData);
            avatarData.setAvatarState(false);
        }
        removeBuffs(player);
        revokeAvatarFlight(player);

        // Cooldown da PRÓXIMA entrada manual -- usa o nível de quando esta
        // ativação COMEÇOU, não o nível atual (que só muda na próxima
        // entrada de qualquer forma, mas mantém a intenção explícita).
        if (enteredLevel != null && enteredLevel.hasCooldown() && player.level() instanceof ServerLevel level) {
            COOLDOWN_UNTIL_TICK.put(player.getUUID(), level.getGameTime() + enteredLevel.getCooldownTicks());
        } else {
            COOLDOWN_UNTIL_TICK.remove(player.getUUID());
        }

        broadcastSync(player, false);
        player.displayClientMessage(Component.literal("§7You left the Avatar State. Your previous bendings have been restored."), true);
    }

    /**
     * Tira uma "foto" de tudo que o jogador tem em {@code Bender#plrData.elements}
     * (a lista que o Cycle Elements percorre) e guarda em {@link PlayerAvatarData}
     * -- depois troca a lista inteira só pelos 2 elementos do Avatar ({@link
     * AvatarElement}/{@link EnergyElement}). Isso NÃO apaga o progresso de upgrade
     * de nenhuma bending (fica gravado à parte, em {@code PlayerData#upgrades},
     * que nunca é tocado aqui) -- só esconde as bendings do Cycle Elements
     * enquanto o Avatar State estiver ligado. Ver {@link #restoreLockedBendings}
     * pra devolução.
     */
    private static void lockCurrentBendingsAndGrantAvatar(ServerPlayer player, Bender bender, PlayerAvatarData avatarData) {
        List<String> snapshot = new ArrayList<>();
        for (Element element : bender.plrData.elements) {
            if (element != NoneElement.get()) {
                snapshot.add(element.getName());
            }
        }
        avatarData.setSavedElements(snapshot);
        avatarData.setSavedActiveElementIndex(bender.plrData.activeElementIndex);

        bender.plrData.elements.clear();
        bender.plrData.elements.add(AvatarElement.get());
        bender.plrData.elements.add(EnergyElement.get());
        bender.plrData.activeElementIndex = 0;
        bender.bindDefaultAbilities();
        bender.syncElements();
        MoreBendingCommand.syncAndPersist(bender, player);
    }

    /**
     * Desfaz {@link #lockCurrentBendingsAndGrantAvatar}: tira Avatar/Energy da
     * lista e devolve exatamente os elementos que estavam salvos (mesma ordem,
     * mesmo elemento ativo de antes quando possível). Se algum elemento salvo
     * não existir mais (mod removido, renomeado, etc.) ele é simplesmente
     * ignorado em vez de travar a restauração dos outros.
     */
    private static void restoreLockedBendings(ServerPlayer player, Bender bender, PlayerAvatarData avatarData) {
        bender.plrData.elements.clear();
        for (String name : avatarData.getSavedElements()) {
            Element element = Element.getElement(name);
            if (element != null && element.getName().equalsIgnoreCase(name)) {
                bender.plrData.elements.add(element);
            }
        }
        if (bender.plrData.elements.isEmpty()) {
            bender.plrData.elements.add(NoneElement.get());
        }

        int savedIndex = avatarData.getSavedActiveElementIndex();
        bender.plrData.activeElementIndex =
                (savedIndex >= 0 && savedIndex < bender.plrData.elements.size()) ? savedIndex : 0;

        bender.bindDefaultAbilities();
        bender.syncElements();
        MoreBendingCommand.syncAndPersist(bender, player);
        avatarData.clearSavedElements();
    }

    private static void applyBuffs(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, EFFECT_DURATION_TICKS, 0, true, false));
        // Jump Boost II (agilidade extra ao pular) e Slow Falling (queda de
        // pena -- amortece qualquer queda mesmo se o voo estiver desligado
        // no momento).
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, EFFECT_DURATION_TICKS, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, EFFECT_DURATION_TICKS, 0, true, false));
        // Health Boost II -- +4 corações extras (8 HP) por cima da vida
        // máxima normal, enquanto o Avatar State estiver ligado.
        player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, EFFECT_DURATION_TICKS, 1, true, false));
    }

    private static void removeBuffs(ServerPlayer player) {
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.REGENERATION);
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.WATER_BREATHING);
        player.removeEffect(MobEffects.SATURATION);
        player.removeEffect(MobEffects.HEALTH_BOOST);
        // Speed, Jump Boost e Slow Falling são deixados decair sozinhos
        // (somem em poucos segundos) pra não cortar o movimento do
        // jogador de forma abrupta ao desligar.
    }

    /**
     * Concede voo de verdade mesmo em Survival/Adventure -- mesmo mecanismo
     * usado por {@code FlyingAbility} (Air): liga {@code mayfly} (permite
     * ativar/desativar o voo com duplo-espaço) e já entra voando
     * ({@code flying = true}) no instante da ativação. Chamado em
     * {@link #activate} e em {@link #onPlayerLoggedIn} (pra devolver o voo
     * a quem loga já com o Avatar State ligado). NÃO é reforçado a cada
     * tick de buff (ver {@link #onServerTick}) de propósito -- senão o
     * jogador que pousasse e desligasse o voo manualmente (duplo-espaço)
     * seria forçado de volta ao ar a cada poucos segundos.
     */
    private static void grantAvatarFlight(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        abilities.mayfly = true;
        abilities.flying = true;
        player.onUpdateAbilities();
    }

    /**
     * Desfaz {@link #grantAvatarFlight}. Só desliga {@code flying}/{@code
     * mayfly} se o jogador estiver em Survival/Adventure -- em
     * Creative/Spectator o voo já é do próprio modo de jogo, então nunca
     * mexemos nele (mesma checagem de {@code FlyingAbility#stopFlying}).
     */
    private static void revokeAvatarFlight(ServerPlayer player) {
        if (player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                || player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
            Abilities abilities = player.getAbilities();
            abilities.flying = false;
            abilities.mayfly = false;
            player.onUpdateAbilities();
        }
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
        AvatarFxScheduler.tick(); // anima pilares/ondas/etc mesmo se ACTIVE ficar vazio no meio de uma animação
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
            if (hasDurationExpired(player)) {
                player.displayClientMessage(Component.literal(
                        "§7Your time as the Avatar has run out -- you were pulled back to normal."), true);
                it.remove(); // via Iterator -- nunca chamar #deactivate aqui, causaria ConcurrentModificationException
                finishDeactivation(player);
                continue;
            }
            if (player.tickCount % EFFECT_REFRESH_INTERVAL == 0) {
                applyBuffs(player);
            }
            checkRingAutoExpire(player);
            updateAllRings(player);
            applyRingCombatEffects(player);
        }
    }

    /** @return se a ativação ATUAL do jogador já passou da duração do seu nível travado (sempre {@code false} pro Mestre -- sem limite). */
    private static boolean hasDurationExpired(ServerPlayer player) {
        Long start = ACTIVATION_START_TICK.get(player.getUUID());
        if (start == null) {
            return false;
        }
        AvatarProgressionLevel entryLevel = ACTIVE_ENTRY_LEVEL.get(player.getUUID());
        if (entryLevel == null || !entryLevel.hasDurationLimit() || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return (level.getGameTime() - start) >= entryLevel.getDurationTicks();
    }

    /**
     * Desliga sozinho qualquer anel que ainda esteja no período
     * automático de 60s desde a ativação (ver {@link #RING_AUTO_DURATION_TICKS})
     * e que o jogador nunca tenha tocado manualmente (ver {@link #MANUAL_RINGS}).
     * Dispara no máximo uma vez por ativação -- some de {@link #RING_AUTO_EXPIRE_TICK}
     * assim que roda, então anéis ligados/desligados depois disso via
     * keybind não sofrem mais nenhum desligamento automático.
     */
    private static void checkRingAutoExpire(ServerPlayer player) {
        Long expireTick = RING_AUTO_EXPIRE_TICK.get(player.getUUID());
        if (expireTick == null || player.tickCount < expireTick) {
            return;
        }
        RING_AUTO_EXPIRE_TICK.remove(player.getUUID());

        Set<RingElement> manual = MANUAL_RINGS.getOrDefault(player.getUUID(), EnumSet.noneOf(RingElement.class));
        Set<RingElement> disabled = DISABLED_RINGS.computeIfAbsent(player.getUUID(), id -> EnumSet.noneOf(RingElement.class));
        boolean anyTurnedOff = false;
        for (RingElement element : RingElement.values()) {
            if (manual.contains(element)) {
                continue; // jogador já assumiu o controle manual deste anel -- não mexe
            }
            if (disabled.add(element)) {
                anyTurnedOff = true;
                if (BLOCK_ELEMENTS.contains(element)) {
                    removeRing(element, player); // anéis de partícula somem sozinhos no próximo updateAllRings
                }
            }
        }
        if (anyTurnedOff) {
            player.displayClientMessage(Component.literal(
                    "§7The elemental rings turned off automatically after 60 seconds. Use each ring's keybind to bring one back individually."), true);
        }
    }

    // ==================== Efeito visual: 4 anéis de blocos reais ====================
    //
    // Os 4 elementos-base giram ao redor do corpo inteiro como blocos DE
    // VERDADE (Display.BlockDisplay), não partícula/falling_dust. O ANEL EM
    // SI NÃO GIRA -- cada bloco fica numa posição fixa (ângulo fixo dentro
    // do círculo, calculado só a partir do índice dele, sem nenhum termo de
    // tempo). O que gira agora, RÁPIDO, é o PONTO NO CÍRCULO em torno do
    // eixo do elemento -- isto é, cada bloco/partícula percorre o anel
    // (orbita), não só gira parado no próprio lugar. O eixo de cada
    // elemento é o eixo de rotação do círculo inteiro (perpendicular ao
    // plano onde o anel vive):
    //   - Ar    -> eixo X (1,0,0)  -> plano Y-Z -> anel VERTICAL
    //   - Água  -> eixo Y (0,1,0)  -> plano X-Z -> anel HORIZONTAL
    //   - Terra -> eixo (-1,1,0)   -> plano diagonal, inclinado à ESQUERDA
    //   - Fogo  -> eixo ( 1,1,0)   -> plano diagonal, inclinado à DIREITA
    // Além de orbitar, cada bloco também gira um pouco em torno de si
    // mesmo (own-spin, bem mais lento que a órbita) só pra dar textura --
    // o movimento dominante é sempre a órbita.

    /** Público agora -- precisa ser referenciado pelas 4 packets de toggle individual. */
    public enum RingElement { FIRE, WATER, EARTH, AIR }

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

    // Raios dos anéis -- controla o tamanho de cada anel ao redor do
    // corpo (o PLANO de cada anel agora vem do eixo, não de um "tilt").
    private static final double FIRE_RADIUS = 4.4;
    private static final double WATER_RADIUS = 5.8;
    private static final double EARTH_RADIUS = 5.1;
    private static final double AIR_RADIUS = 7.0;

    // Eixos de órbita (e também de own-spin) de cada elemento -- ver
    // explicação no topo da seção. Já normalizados.
    private static final Vector3f AIR_AXIS = new Vector3f(1f, 0f, 0f);
    private static final Vector3f WATER_AXIS = new Vector3f(0f, 1f, 0f);
    private static final Vector3f EARTH_AXIS = new Vector3f(-1f, 1f, 0f).normalize();
    private static final Vector3f FIRE_AXIS = new Vector3f(1f, 1f, 0f).normalize();

    /**
     * Duas direções unitárias perpendiculares entre si e perpendiculares
     * a {@code axis} -- formam a base do plano em que o anel vive. Um
     * ponto do anel a um ângulo {@code θ} fica em
     * {@code center + radius*(cos θ * u + sin θ * v)}; fazer {@code θ}
     * avançar com o tempo é o que faz o anel ORBITAR em torno de
     * {@code axis} (em vez de só ficar girando no próprio centro).
     */
    private static Vector3f[] perpendicularBasis(Vector3f axis) {
        Vector3f a = new Vector3f(axis).normalize();
        Vector3f arbitrary = Math.abs(a.y) < 0.99f ? new Vector3f(0f, 1f, 0f) : new Vector3f(1f, 0f, 0f);
        Vector3f u = new Vector3f();
        a.cross(arbitrary, u);
        u.normalize();
        Vector3f v = new Vector3f();
        a.cross(u, v);
        v.normalize();
        return new Vector3f[]{u, v};
    }

    /**
     * Configuração fixa de cada anel de bloco. {@code axis} é o eixo de
     * órbita do anel inteiro (ver {@link #perpendicularBasis}); {@code
     * orbitDegPerTick} é a velocidade da órbita (bem alta = "muito
     * rápido"); {@code ownSpinDegPerTick} é um giro adicional, bem mais
     * lento, do bloco em torno de si mesmo (só textura, não é o
     * movimento principal). {@code jitterRadius}/{@code jitterHeight}
     * dão variação fixa por índice (só a Terra usa).
     */
    private record RingConfig(double radius, int count, float scale,
                              Vector3f axis, double orbitDegPerTick, double ownSpinDegPerTick,
                              IntFunction<BlockState> blockAt,
                              double jitterRadius, double jitterHeight) {
    }

    // Só Água e Terra usam blocos de verdade agora.
    private static final List<RingElement> BLOCK_ELEMENTS = List.of(RingElement.WATER, RingElement.EARTH);

    private static final Map<RingElement, RingConfig> CONFIGS = new EnumMap<>(RingElement.class);
    private static final Map<RingElement, Vector3f[]> BLOCK_BASIS = new EnumMap<>(RingElement.class);
    static {
        CONFIGS.put(RingElement.WATER, new RingConfig(
                WATER_RADIUS, 26, 0.40f,
                WATER_AXIS, 34, 8,
                i -> WATER_BLOCK,
                0.0, 0.0));
        CONFIGS.put(RingElement.EARTH, new RingConfig(
                EARTH_RADIUS, 32, 0.45f,
                EARTH_AXIS, 30, 8,
                i -> EARTH_BLOCKS[i % EARTH_BLOCKS.length],
                0.4, 0.25));
        for (Map.Entry<RingElement, RingConfig> entry : CONFIGS.entrySet()) {
            BLOCK_BASIS.put(entry.getKey(), perpendicularBasis(entry.getValue().axis()));
        }
    }

    /**
     * Configuração dos anéis 100% partícula (Fogo e Ar) -- sem entidade
     * nenhuma, só {@code level.sendParticles} todo tick. A posição
     * também orbita em torno de {@code axis}, igual aos anéis de bloco.
     * {@code particlesPerPoint} é o que dá o "rate alto" pedido.
     */
    private record ParticleRingConfig(double radius, int count, int particlesPerPoint,
                                      Vector3f axis, double orbitDegPerTick) {
    }

    private static final Map<RingElement, ParticleRingConfig> PARTICLE_CONFIGS = new EnumMap<>(RingElement.class);
    private static final Map<RingElement, Vector3f[]> PARTICLE_BASIS = new EnumMap<>(RingElement.class);
    static {
        PARTICLE_CONFIGS.put(RingElement.FIRE, new ParticleRingConfig(FIRE_RADIUS, 70, 3, FIRE_AXIS, 38));
        PARTICLE_CONFIGS.put(RingElement.AIR, new ParticleRingConfig(AIR_RADIUS, 90, 4, AIR_AXIS, 44));
        for (Map.Entry<RingElement, ParticleRingConfig> entry : PARTICLE_CONFIGS.entrySet()) {
            PARTICLE_BASIS.put(entry.getKey(), perpendicularBasis(entry.getValue().axis()));
        }
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
            if (isRingEnabled(player, element)) {
                updateRing(element, player, baseY);
            } else {
                removeRing(element, player); // idempotente -- não faz nada se já não tinha
            }
        }
        if (isRingEnabled(player, RingElement.FIRE)) {
            drawParticleRing(RingElement.FIRE, player, baseY);
        }
        if (isRingEnabled(player, RingElement.AIR)) {
            drawParticleRing(RingElement.AIR, player, baseY);
        }
    }

    // ==================== Efeitos de combate dos anéis ====================
    //
    // Cada anel LIGADO protege o Avatar de quem chegar perto (pedido do
    // usuário, uma "individualidade" por elemento):
    //   - Fogo  -> queima (fire ticks) + dano que aumenta quanto mais perto
    //     do centro do anel o alvo estiver;
    //   - Água  -> só dispara pra quem chega bem perto o suficiente pra
    //     "tocar" o Avatar (não o anel inteiro): sufocamento rápido
    //     (dano de afogamento em intervalo curto) + congelamento
    //     (trava com lentidão pesada, igual iceMastery);
    //   - Terra -> dano contínuo pra qualquer um dentro do raio do anel;
    //   - Ar    -> nunca deixa ninguém chegar perto: empurrão radial pra
    //     fora todo tick + dano cortante periódico.

    private static final int RING_DAMAGE_INTERVAL_TICKS = 10; // a cada 0.5s

    private static final float FIRE_RING_BASE_DAMAGE = 1.0f;
    private static final float FIRE_RING_MAX_BONUS_DAMAGE = 3.0f; // no centro do anel, dano = base + bonus
    private static final int FIRE_RING_BURN_TICKS = 60; // 3s de fogo, reforçado a cada intervalo

    private static final double WATER_RING_TOUCH_RANGE = 2.0; // "tocar" o Avatar -- não é o raio inteiro do anel
    private static final float WATER_RING_SUFFOCATE_DAMAGE = 1.5f;
    private static final int WATER_RING_SUFFOCATE_INTERVAL_TICKS = 4; // "muito rápido" -- a cada 0.2s

    private static final float EARTH_RING_DAMAGE = 1.5f;

    private static final float AIR_RING_SLASH_DAMAGE = 2.0f;
    private static final double AIR_RING_PUSH = 0.9;

    private static void applyRingCombatEffects(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean fireOn = isRingEnabled(player, RingElement.FIRE);
        boolean waterOn = isRingEnabled(player, RingElement.WATER);
        boolean earthOn = isRingEnabled(player, RingElement.EARTH);
        boolean airOn = isRingEnabled(player, RingElement.AIR);
        if (!fireOn && !waterOn && !earthOn && !airOn) {
            return;
        }

        double maxRadius = Math.max(Math.max(FIRE_RADIUS, WATER_RADIUS), Math.max(EARTH_RADIUS, AIR_RADIUS));
        AABB area = player.getBoundingBox().inflate(maxRadius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive());
        if (nearby.isEmpty()) {
            return;
        }

        boolean damageTick = player.tickCount % RING_DAMAGE_INTERVAL_TICKS == 0;
        Vec3 center = player.position();

        for (LivingEntity target : nearby) {
            double dist = target.position().distanceTo(center);

            if (fireOn && dist <= FIRE_RADIUS) {
                applyFireRingEffect(level, target, dist, damageTick);
            }
            if (waterOn && dist <= WATER_RING_TOUCH_RANGE) {
                applyWaterRingTouchEffect(level, player, target);
            }
            if (earthOn && damageTick && dist <= EARTH_RADIUS) {
                target.hurt(level.damageSources().indirectMagic(player, player), EARTH_RING_DAMAGE);
            }
            if (airOn && dist <= AIR_RADIUS) {
                applyAirRingRepulsionEffect(level, player, target, damageTick);
            }
        }
    }

    /** Fogo: mantém o alvo pegando fogo e aplica dano que cresce conforme ele chega mais perto do centro do anel. */
    private static void applyFireRingEffect(ServerLevel level, LivingEntity target, double dist, boolean damageTick) {
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), FIRE_RING_BURN_TICKS));
        if (!damageTick) {
            return;
        }
        double closeness = 1.0 - Math.min(1.0, dist / FIRE_RADIUS); // 0 na borda do anel, 1 no centro
        float damage = FIRE_RING_BASE_DAMAGE + (float) (FIRE_RING_MAX_BONUS_DAMAGE * closeness);
        target.hurt(level.damageSources().onFire(), damage);
    }

    /** Água: só quem chega perto o bastante pra "tocar" o Avatar sofre -- sufocamento rápido + congelamento. */
    private static void applyWaterRingTouchEffect(ServerLevel level, ServerPlayer player, LivingEntity target) {
        // Sufocamento "muito rápido": esvazia o ar quase na hora e aplica
        // dano de afogamento num intervalo bem mais curto que o afogar
        // normal do jogo.
        target.setAirSupply(Math.max(-20, target.getAirSupply() - 40));
        if (player.tickCount % WATER_RING_SUFFOCATE_INTERVAL_TICKS == 0) {
            target.hurt(level.damageSources().drown(), WATER_RING_SUFFOCATE_DAMAGE);
        }
        // Congelamento: trava o alvo com lentidão quase total (igual
        // FrostNovaAbility) e ativa a textura/estado congelado do
        // vanilla enquanto ele insistir em ficar colado no Avatar.
        target.setTicksFrozen(target.getTicksRequiredToFreeze());
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 6, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 3, false, false, true));
    }

    /** Ar: empurra pra fora todo tick (nunca deixa chegar perto) e corta com dano periódico. */
    private static void applyAirRingRepulsionEffect(ServerLevel level, ServerPlayer player, LivingEntity target, boolean damageTick) {
        Vec3 away = target.position().subtract(player.position());
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5);
        }
        Vec3 push = away.normalize().scale(AIR_RING_PUSH).add(0, 0.2, 0);
        target.push(push.x, push.y, push.z);
        target.hurtMarked = true;
        if (damageTick) {
            target.hurt(level.damageSources().playerAttack(player), AIR_RING_SLASH_DAMAGE);
        }
    }

    /**
     * Anel 100% partícula (Fogo/Ar) -- cada ponto ORBITA em torno do
     * eixo do elemento (ver {@link #perpendicularBasis}), rápido. Fogo =
     * só FLAME/SMALL_FLAME (nunca LAVA). Ar = só WHITE_SMOKE.
     */
    private static void drawParticleRing(RingElement element, ServerPlayer player, double baseY) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ParticleRingConfig config = PARTICLE_CONFIGS.get(element);
        Vector3f[] basis = PARTICLE_BASIS.get(element);
        Vector3f u = basis[0];
        Vector3f v = basis[1];
        int count = config.count();
        double t = player.tickCount;
        double orbit = Math.toRadians(t * config.orbitDegPerTick());

        for (int i = 0; i < count; i++) {
            // Ângulo base do ponto (posição dele dentro do anel) + órbita
            // (avança com o tempo) -- é a soma dos dois que faz o ponto
            // percorrer o círculo inteiro em volta do eixo, rápido.
            double angle = (2 * Math.PI * i) / count + orbit;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double x = player.getX() + config.radius() * (cos * u.x + sin * v.x);
            double y = baseY + config.radius() * (cos * u.y + sin * v.y);
            double z = player.getZ() + config.radius() * (cos * u.z + sin * v.z);

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
     * Reposiciona cada bloco ORBITANDO em torno do eixo do elemento
     * (posição = ângulo base do bloco + ângulo de órbita que avança com
     * o tempo -- é isso que faz o anel percorrer o círculo em vez de
     * ficar parado) e aplica um own-spin leve (bem mais lento que a
     * órbita) só de textura.
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
        Vector3f[] basis = BLOCK_BASIS.get(element);
        Vector3f u = basis[0];
        Vector3f v = basis[1];
        double t = player.tickCount;
        int count = chunks.size();
        double orbit = Math.toRadians(t * config.orbitDegPerTick());

        for (int i = 0; i < count; i++) {
            Display.BlockDisplay display = chunks.get(i);
            if (display.isRemoved()) {
                // Chunk sumiu (chunk do mundo descarregou, etc.) -- recria
                // no lugar pra nunca ficar faltando um pedaço do anel.
                display = newRingChunkEntity(level, player, config.blockAt().apply(i));
                chunks.set(i, display);
            }

            // ---- Posição: ângulo base do bloco (índice) + órbita (t) --
            // é a soma dos dois que faz o bloco PERCORRER o anel em volta
            // do eixo, em vez de ficar parado numa posição fixa.
            double angle = (2 * Math.PI * i) / count + orbit;
            double jitterR = config.jitterRadius() != 0.0
                    ? config.jitterRadius() * Math.sin(i * 2.399963) : 0.0;
            double jitterH = config.jitterHeight() != 0.0
                    ? config.jitterHeight() * Math.cos(i * 1.618034) : 0.0;
            double radius = config.radius() + jitterR;

            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double worldX = player.getX() + radius * (cos * u.x + sin * v.x);
            double worldY = baseY + jitterH + radius * (cos * u.y + sin * v.y);
            double worldZ = player.getZ() + radius * (cos * u.z + sin * v.z);
            display.setPos(worldX, worldY, worldZ);
            spawnRingAccent(element, level, i, worldX, worldY, worldZ);

            // ---- Own-spin: giro leve do bloco em torno de si mesmo,
            // bem mais lento que a órbita -- só textura, o movimento
            // dominante é a órbita acima.
            float phase = (float) Math.toRadians(t * config.ownSpinDegPerTick() + i * 17);
            Vector3f axis = config.axis();
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
            DISABLED_RINGS.remove(sp.getUUID());
            MANUAL_RINGS.remove(sp.getUUID());
            RING_AUTO_EXPIRE_TICK.remove(sp.getUUID());
            // Cooldown e nível/timer de duração de propósito NÃO são limpos aqui --
            // são o estado de progressão do jogador, precisam sobreviver a um
            // logout/login (o cooldown de quem sai no meio da espera continua
            // contando; ver onPlayerLoggedIn, que retoma ACTIVATION_START_TICK
            // implicitamente ao reativar os anéis/buffs sem resetar duração).
            removeAllRings(sp);
        }
    }

    /**
     * Quem desconecta com o Avatar State ligado volta com {@code
     * PlayerAvatarData#isAvatarState()} ainda {@code true} (é persistido) mas
     * sem entrar em {@link #ACTIVE} de novo sozinho -- sem isso o jogador
     * ficaria preso só com Avatar+Energy, sem anéis/buffs, até apertar a
     * tecla de novo (o que aliás reativaria certo, já que {@link #activate}
     * só chama {@link #lockCurrentBendingsAndGrantAvatar} quando ainda NÃO
     * está marcado como avatarState). Aqui só retomamos a parte visual/buff
     * automaticamente pra não deixar essa pegadinha pro jogador.
     */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerAvatarData avatarData = player.getData(ModAttachments.AVATAR);
        if (avatarData.isAvatarState()) {
            ACTIVE.add(player.getUUID());
            MANUAL_RINGS.remove(player.getUUID());
            RING_AUTO_EXPIRE_TICK.put(player.getUUID(), player.tickCount + (long) RING_AUTO_DURATION_TICKS);
            applyBuffs(player);
            grantAvatarFlight(player);
            spawnAllRings(player);
            broadcastSync(player, true);
        }
    }
}