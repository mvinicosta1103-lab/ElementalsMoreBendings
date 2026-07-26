package com.elementals.morebendings.network;

import com.elementals.morebendings.Constants;
import com.elementals.morebendings.network.packets.ToggleFlyingPacket;
import commonnetwork.api.Network;
import commonnetwork.networking.data.Side;
import net.minecraft.resources.ResourceLocation;

public final class ModNetworking {

    public static final ResourceLocation TOGGLE_FLYING_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "toggle_flying");

    public static void register() {
        Network.registerPacket(ToggleFlyingPacket.type(), ToggleFlyingPacket.class,
                ToggleFlyingPacket.STREAM_CODEC, ToggleFlyingPacket::handle);
    }

    public static void expectSideOrThrow(Side current, Side expected) {
        if (!current.equals(expected)) {
            throw new RuntimeException("Pacote recebido no lado errado (esperava " + expected + ", era " + current + ")");
        }
    }

    private ModNetworking() {
    }
}