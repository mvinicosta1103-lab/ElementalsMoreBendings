package com.elementals.morebendings.bending.avatarstate;

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
 * Instantânea (sem canal, sem {@code currAbility} setado) -- mesmo
 * espírito de {@code GasCloudAbility}. Só funciona em outro {@code
 * ServerPlayer} de verdade: dobra, no sistema base do mod, não existe em
 * cima de mobs (ver {@link AvatarBendingTargeting}), então mirar em algo
 * que não seja jogador simplesmente não acha alvo.
 */
public class AvatarBendingGrantAbility implements Ability {

    private static final float CHI_COST = 20.0f;

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

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
        level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(0xFFD700).toVector3f(), 1.6f),
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                40, 0.4, 0.6, 0.4, 0.02);
    }

    @Override
    public void onRemove(Bender bender) {
        bender.setCurrAbility(null);
    }
}