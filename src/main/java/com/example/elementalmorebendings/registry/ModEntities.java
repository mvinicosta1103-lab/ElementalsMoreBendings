package com.example.elementalmorebendings.registry;

import com.example.elementalmorebendings.mud.abilities.MudBallEntity;
import com.example.elementalmorebendings.mud.abilities.MudBallEntityRenderer;
import dev.saperate.elementals.platform.Services;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

public class ModEntities {

    public static final Supplier<EntityType<MudBallEntity>> MUD_BALL =
            Services.REGISTRY.registerEntity("mud_ball", MudBallEntity::new, 0.5f, 0.5f);

    public static void init() {
        // apenas força o carregamento estático da classe (e portanto o registro acima)
    }

    public static void registerClientRenderer() {
        Services.REGISTRY.registerClientEntityRenderer(MUD_BALL, MudBallEntityRenderer::new);
    }
}