package com.elementals.morebendings.bending.earthsubbendings.bone;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dono de todos os fantoches de {@code bonePuppeteer} ativos no servidor --
 * um por vítima possuída (chaveado pelo UUID do {@link Mob}, não do
 * caster, já que vários casters podem possuir mortos-vivos diferentes ao
 * mesmo tempo -- mesma ideia de {@link
 * com.elementals.morebendings.bending.watersubbendings.spirit.CurseMinionManager}).
 * Dirigido por {@link ServerTickEvent.Post}, registrado em {@link
 * com.elementals.morebendings.ElementalsMoreBendingsMod}.
 *
 * COMPORTAMENTO: enquanto a possessão durar ({@link #DURATION_TICKS}), a
 * vítima:
 *
 *  - Perde a IA própria ({@code setNoAi(true)}) e para de perseguir
 *    qualquer alvo que já tivesse.
 *  - Anda continuamente na direção horizontal que o CASTER estiver
 *    olhando (yaw do caster), a {@link #WALK_SPEED} -- é o caster quem
 *    "dirige" o fantoche olhando pra algum lado, não precisa apertar
 *    tecla de movimento nenhuma (mesma ideia de {@code BoneControlAbility}
 *    empurrando a farpa, só que aqui é a orientação do caster, não o
 *    ponto mirado, que define a direção).
 *  - A física normal (gravidade, colisão com blocos) continua rolando --
 *    só a IA/pathfinding próprios ficam desligados, então o fantoche
 *    ainda cai, esbarra em paredes, etc.
 *
 * FIM DA POSSESSÃO -- por qualquer um destes motivos a IA original volta
 * (se ela existia antes, ver {@link Puppet#hadAi}) e o fantoche solta:
 *
 *  - {@link #DURATION_TICKS} se esgota.
 *  - O caster desconecta ou morre.
 *  - A própria vítima morre (nesse caso não há nada pra restaurar).
 *
 * Reativar {@code bonePuppeteer} na mesma vítima enquanto ela ainda está
 * possuída (por qualquer caster) apenas reinicia a duração e transfere o
 * controle pro caster novo, em vez de empilhar uma segunda possessão.
 */
public final class BonePuppeteerManager {

    static final int DURATION_TICKS = 100; // 5s
    private static final double WALK_SPEED = 0.18;

    private static final Map<UUID, Puppet> ACTIVE = new HashMap<>();

    private BonePuppeteerManager() {
    }

    public static void possess(ServerLevel level, ServerPlayer caster, Mob victim) {
        UUID id = victim.getUUID();
        Puppet existing = ACTIVE.get(id);
        if (existing != null) {
            // Já possuída (por esse ou outro caster) -- só reinicia a duração e
            // transfere o controle, sem empilhar uma segunda entrada.
            existing.remainingTicks = DURATION_TICKS;
            existing.casterId = caster.getUUID();
            return;
        }

        boolean hadAi = !victim.isNoAi();
        victim.setTarget(null);
        victim.getNavigation().stop();
        victim.setNoAi(true);

        ACTIVE.put(id, new Puppet(level, caster.getUUID(), hadAi));

        level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                victim.getZ(), 16, 0.3, 0.4, 0.3, 0.04);
        level.playSound(null, victim.blockPosition(), SoundEvents.BONE_BLOCK_PLACE,
                SoundSource.HOSTILE, 0.8f, 0.5f);
    }

    /** Registrado via NeoForge.EVENT_BUS.addListener em ElementalsMoreBendingsMod. */
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Puppet>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Puppet> entry = it.next();
            Puppet puppet = entry.getValue();

            Mob victim = findMob(puppet.level, entry.getKey());
            if (victim == null || !victim.isAlive()) {
                it.remove();
                continue; // já morreu/sumiu -- não há IA pra restaurar
            }

            ServerPlayer caster = puppet.level.getServer().getPlayerList().getPlayer(puppet.casterId);
            if (caster == null || !caster.isAlive()) {
                release(victim, puppet);
                it.remove();
                continue; // caster desconectou ou morreu -- solta o fantoche na hora
            }

            puppet.remainingTicks--;
            if (puppet.remainingTicks <= 0) {
                release(victim, puppet);
                it.remove();
                continue;
            }

            walkTowardsCasterLook(victim, caster);

            if (victim.tickCount % 8 == 0) {
                puppet.level.sendParticles(ParticleTypes.SOUL, victim.getX(),
                        victim.getY() + 0.1, victim.getZ(), 2, 0.2, 0.0, 0.2, 0.0);
            }
        }
    }

    /** Empurra a vítima na direção horizontal que o caster está olhando (yaw). */
    private static void walkTowardsCasterLook(Mob victim, ServerPlayer caster) {
        float yaw = caster.getYRot();
        double radians = Math.toRadians(yaw);
        double dx = -Math.sin(radians) * WALK_SPEED;
        double dz = Math.cos(radians) * WALK_SPEED;

        double currentY = victim.getDeltaMovement().y; // preserva queda/pulo -- só controla o horizontal
        victim.setDeltaMovement(dx, currentY, dz);
        victim.hasImpulse = true;

        victim.setYRot(yaw);
        victim.yBodyRot = yaw;
        victim.setYHeadRot(yaw);
    }

    /** Devolve a IA original (se ela existia antes de {@link #possess}) e avisa com efeito. */
    private static void release(Mob victim, Puppet puppet) {
        if (puppet.hadAi) {
            victim.setNoAi(false);
        }
        puppet.level.sendParticles(ParticleTypes.POOF, victim.getX(),
                victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
        puppet.level.playSound(null, victim.blockPosition(), SoundEvents.BONE_BLOCK_BREAK,
                SoundSource.HOSTILE, 0.6f, 0.9f);
    }

    private static Mob findMob(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof Mob mob ? mob : null;
    }

    private static final class Puppet {
        final ServerLevel level;
        UUID casterId;
        final boolean hadAi;
        int remainingTicks = DURATION_TICKS;

        Puppet(ServerLevel level, UUID casterId, boolean hadAi) {
            this.level = level;
            this.casterId = casterId;
            this.hadAi = hadAi;
        }
    }
}