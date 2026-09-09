package com.elementals.morebendings.bending.avatarstate;

import dev.saperate.elementals.effects.ElementalsStatusEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Congela totalmente (via {@code STUNNED}, o mesmo efeito real do mod base
 * usado por {@code IcePrisonState}/{@code CrystalPrisonAbility} -- sem se
 * mover, sem atacar, sem usar itens ou habilidades) o alvo mirado por um
 * Avatar enquanto ele decide, na GUI (ver {@code
 * AvatarBendingChoiceScreen}), o que conceder ou remover ({@link
 * AvatarBendingOptions}).
 * <p>
 * Uma escolha pendente POR CASTER (não por alvo) -- se dois Avatars
 * mirarem o mesmo alvo ao mesmo tempo, cada um mantém seu próprio freeze
 * independente, e o alvo só é liberado de verdade quando o último deles
 * resolver ou expirar, porque o {@code STUNNED} é reaplicado a cada tick
 * enquanto QUALQUER entrada deste manager apontar pra ele.
 */
public final class AvatarBendingFreezeManager {

    /** Trava de segurança -- se o Avatar nunca escolher nem cancelar (ex: desconexão), libera sozinho depois de 30s. */
    private static final int MAX_DURATION_TICKS = 20 * 30;
    /** Duração curta reaplicada a cada tick -- mesmo esquema de {@code IcePrisonState}, nunca "vence" sozinha enquanto reaplicada. */
    private static final int STUN_REFRESH_TICKS = 10;
    /** Bem mais alto que a prisão de gelo -- aqui o pedido explícito é imobilização total, não só um stun de combate. */
    private static final int STUN_AMPLIFIER = 4;

    private static final Map<UUID, Pending> ACTIVE = new HashMap<>();

    private AvatarBendingFreezeManager() {
    }

    private static final class Pending {
        final ServerPlayer caster;
        final ServerPlayer target;
        int ticksElapsed = 0;

        Pending(ServerPlayer caster, ServerPlayer target) {
            this.caster = caster;
            this.target = target;
        }
    }

    /** Congela {@code target} pra este {@code caster}; substitui qualquer escolha pendente anterior dele. */
    public static void begin(ServerPlayer caster, ServerPlayer target) {
        release(caster);
        ACTIVE.put(caster.getUUID(), new Pending(caster, target));
        freeze(target);
    }

    /** @return o alvo atualmente congelado por este caster, ou null se não houver escolha pendente. */
    public static ServerPlayer pendingTarget(ServerPlayer caster) {
        Pending pending = ACTIVE.get(caster.getUUID());
        return pending == null ? null : pending.target;
    }

    /** Libera a escolha pendente deste caster (escolha feita, cancelada ou invalidada). */
    public static void release(ServerPlayer caster) {
        Pending pending = ACTIVE.remove(caster.getUUID());
        if (pending != null) {
            unfreeze(pending.target);
        }
    }

    private static void freeze(ServerPlayer target) {
        target.addEffect(new MobEffectInstance(ElementalsStatusEffects.STUNNED.get(), STUN_REFRESH_TICKS, STUN_AMPLIFIER));
    }

    private static void unfreeze(ServerPlayer target) {
        if (target.isAlive() && !target.isRemoved()) {
            target.removeEffect(ElementalsStatusEffects.STUNNED.get());
        }
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Pending>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Pending pending = it.next().getValue();
            pending.ticksElapsed++;

            boolean casterGone = !pending.caster.isAlive() || pending.caster.isRemoved();
            boolean targetGone = !pending.target.isAlive() || pending.target.isRemoved();
            boolean timedOut = pending.ticksElapsed > MAX_DURATION_TICKS;

            if (casterGone || targetGone || timedOut) {
                unfreeze(pending.target);
                if (timedOut && !casterGone) {
                    pending.caster.displayClientMessage(
                            Component.literal("§7Energybend selection timed out."), true);
                }
                it.remove();
                continue;
            }

            freeze(pending.target);
        }
    }
}