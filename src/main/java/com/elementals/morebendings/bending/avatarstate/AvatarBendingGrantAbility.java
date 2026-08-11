package com.elementals.morebendings.bending.avatarstate;

import com.elementals.morebendings.bending.avatarstate.fx.AvatarFxScheduler;
import com.elementals.morebendings.commands.MoreBendingCommand;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.elements.Ability;
import dev.saperate.elementals.elements.Element;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * "cede" um dos 4 elementos-base pro jogador mirado (ver {@code
 * CastAvatarBendingGrantPacket}, tecla dedicada) -- qual elemento é
 * decidido por {@link AvatarBendingSelection#current}, trocado com {@code
 * CycleAvatarBendingPacket}. Só utilizável enquanto o caster está no
 * Avatar State (ver {@link AvatarStateManager#isActive}, checado no
 * pacote antes de chamar esta ability) -- é a contraparte de {@link
 * AvatarBendingRemoveAbility}.
 * <p>
 * A concessão em si continua instantânea (o alvo já recebe o elemento no
 * momento do cast, como antes) -- só a "casca" visual ganhou um feixe de
 * energia dourado viajando do caster até o alvo ao longo de alguns ticks,
 * em vez de só um burst de partículas em cima do alvo -- ver {@link
 * AvatarFxScheduler}.
 */
public class AvatarBendingGrantAbility implements Ability {

    private static final float CHI_COST = 20.0f;
    private static final DustParticleOptions BEAM_DUST =
            new DustParticleOptions(Vec3.fromRGB24(0xFFD700).toVector3f(), 1.3f);
    private static final int BEAM_STEPS = 8;

    @Override
    public void onCall(Bender bender, long heldTimeMs) {
        Player player = bender.player;
        bender.setCurrAbility(null); // instantânea, nunca canaliza

        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        ServerPlayer target = AvatarBendingTargeting.raycastPlayerTarget(caster, level);
        if (target == null) {
            caster.displayClientMessage(Component.literal("§7Nenhum jogador à vista."), true);
            return;
        }
        if (target == caster) {
            caster.displayClientMessage(Component.literal("§7Você não pode usar isso em si mesmo."), true);
            return;
        }

        Element element = AvatarBendingSelection.current(caster);
        String name = AvatarBendingSelection.displayName(element);
        Bender targetBender = Bender.getBender(target);
        if (targetBender.hasElement(element)) {
            caster.displayClientMessage(Component.literal(
                    "§7" + target.getName().getString() + " já domina " + name + "."), true);
            return;
        }

        if (!bender.reduceChi(CHI_COST)) {
            caster.displayClientMessage(Component.literal("§7Chi insuficiente."), true);
            return;
        }

        targetBender.addElement(element, true);
        MoreBendingCommand.syncAndPersist(targetBender, target);

        caster.displayClientMessage(Component.literal(
                "§bVocê concedeu " + name + " a " + target.getName().getString() + "."), true);
        target.displayClientMessage(Component.literal(
                "§bO Avatar concedeu a você a dobra de " + name + "!"), true);

        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f);

        Vec3 from = caster.position().add(0, caster.getBbHeight() * 0.6, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        for (int step = 0; step <= BEAM_STEPS; step++) {
            final double t = (double) step / BEAM_STEPS;
            AvatarFxScheduler.schedule(step, () -> {
                Vec3 point = from.lerp(to, t);
                level.sendParticles(BEAM_DUST, point.x, point.y, point.z, 6, 0.15, 0.15, 0.15, 0.0);
                if (t >= 1.0) {
                    // feixe chegou -- estouro de luz no alvo, igual antes
                    level.sendParticles(BEAM_DUST, point.x, point.y, point.z, 40, 0.4, 0.6, 0.4, 0.02);
                    level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
                }
            });
        }
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}