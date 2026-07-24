package com.elementals.morebendings.registry;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.data.PlayerSubbendingData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registro do Data Attachment que guarda, por jogador, quais sub-bendings
 * ele já tem. Precisa ser registrado no mod event bus — isso é feito em
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
}