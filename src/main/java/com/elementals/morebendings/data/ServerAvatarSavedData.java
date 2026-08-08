package com.elementals.morebendings.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.UUID;

/**
 * Estado GLOBAL do server (não por jogador, diferente de {@link
 * PlayerAvatarData}) pro sistema de "Avatar-título": só existe UM Avatar
 * por vez no server inteiro, ele é escolhido dentre os jogadores online
 * (ver {@code com.elementals.morebendings.bending.avatarstate.ServerAvatarManager}),
 * e continua sendo o Avatar até morrer -- não até desconectar.
 * <p>
 * Segue o mesmo padrão de {@code dev.saperate.elementals.data.StateDataSaverAndLoader}
 * do mod base (SavedData preso ao Overworld via DimensionDataStorage).
 */
public class ServerAvatarSavedData extends SavedData {

    private static final String ID = "elementalsmorebendings_server_avatar";

    private static final SavedData.Factory<ServerAvatarSavedData> FACTORY =
            new SavedData.Factory<>(ServerAvatarSavedData::new, ServerAvatarSavedData::load, null);

    /** {@code null} = sistema nunca foi iniciado, ou ninguém é o Avatar no momento (ver comentário em ServerAvatarManager#onPlayerLoggedIn). */
    private UUID currentAvatar;

    /** Uma vez {@code true} (por {@code /morebending serveravatar start}), fica assim pra sempre -- controla o fallback de auto-atribuição em login. */
    private boolean systemStarted = false;

    public static ServerAvatarSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        assert overworld != null;
        DimensionDataStorage storage = overworld.getDataStorage();
        ServerAvatarSavedData state = storage.computeIfAbsent(FACTORY, ID);
        state.setDirty();
        return state;
    }

    public static ServerAvatarSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ServerAvatarSavedData state = new ServerAvatarSavedData();
        if (tag.hasUUID("currentAvatar")) {
            state.currentAvatar = tag.getUUID("currentAvatar");
        }
        state.systemStarted = tag.getBoolean("systemStarted");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        if (currentAvatar != null) {
            tag.putUUID("currentAvatar", currentAvatar);
        }
        tag.putBoolean("systemStarted", systemStarted);
        return tag;
    }

    public UUID getCurrentAvatar() {
        return currentAvatar;
    }

    public void setCurrentAvatar(UUID uuid) {
        this.currentAvatar = uuid;
        this.setDirty();
    }

    public boolean isSystemStarted() {
        return systemStarted;
    }

    public void markSystemStarted() {
        this.systemStarted = true;
        this.setDirty();
    }
}