package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.data.PlayerAvatarData;
import com.elementals.morebendings.data.PlayerSubbendingData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registro dos Data Attachments deste addon — cada um guarda, por jogador,
 * um pedaço de estado próprio (não o Bender/PlayerData do mod base, que
 * tem seu próprio sistema de save via {@code StateDataSaverAndLoader}).
 * Precisa ser registrado no mod event bus — isso é feito em
 * ElementalsMoreBendingsMod.
 */
public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Constants.MOD_ID);

    public static final Supplier<AttachmentType<PlayerSubbendingData>> SUBBENDINGS =
            ATTACHMENT_TYPES.register("subbendings",
                    () -> AttachmentType.builder(PlayerSubbendingData::new)
                            .serialize(PlayerSubbendingData.CODEC)
                            .copyOnDeath()
                            .build());

    /** Estado de Avatar State por jogador — ver {@link PlayerAvatarData}. */
    public static final Supplier<AttachmentType<PlayerAvatarData>> AVATAR =
            ATTACHMENT_TYPES.register("avatar_state",
                    () -> AttachmentType.builder(PlayerAvatarData::new)
                            .serialize(PlayerAvatarData.CODEC)
                            .copyOnDeath()
                            .build());
}