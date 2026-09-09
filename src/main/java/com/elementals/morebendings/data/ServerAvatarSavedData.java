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

    /**
     * Se {@code true}, o Avatar atual foi atribuído via {@code /morebending
     * avatar set <player>} (em vez de {@code /morebending serveravatar
     * start|set}) -- nesse caso ele NÃO perde o título nem os elementos ao
     * morrer, e o título não passa pra mais ninguém (ver {@code
     * ServerAvatarManager#onAvatarDeath}). Sempre volta a {@code false}
     * quando o título muda de mãos por qualquer outro caminho.
     */
    private boolean permanentAvatar = false;

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
        state.permanentAvatar = tag.getBoolean("permanentAvatar");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        if (currentAvatar != null) {
            tag.putUUID("currentAvatar", currentAvatar);
        }
        tag.putBoolean("systemStarted", systemStarted);
        tag.putBoolean("permanentAvatar", permanentAvatar);
        return tag;
    }

    public UUID getCurrentAvatar() {
        return currentAvatar;
    }

    /** Equivalente a {@code setCurrentAvatar(uuid, false)} -- usado por todo caminho que NÃO deve ser permanente. */
    public void setCurrentAvatar(UUID uuid) {
        setCurrentAvatar(uuid, false);
    }

    /** @param permanent ver {@link #permanentAvatar}. Sempre reavaliado a cada troca de título, nunca herdado do titular anterior. */
    public void setCurrentAvatar(UUID uuid, boolean permanent) {
        this.currentAvatar = uuid;
        this.permanentAvatar = permanent;
        this.setDirty();
    }

    /** @return se o Avatar atual foi atribuído como permanente (ver {@link #permanentAvatar}). */
    public boolean isPermanentAvatar() {
        return permanentAvatar;
    }

    public boolean isSystemStarted() {
        return systemStarted;
    }

    public void markSystemStarted() {
        this.systemStarted = true;
        this.setDirty();
    }
}