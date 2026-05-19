//? if fabric {
package ch.krishd.chunkpermits.fabric;

import ch.krishd.chunkpermits.Chunkpermits;
import ch.krishd.chunkpermits.ChunkPermitsServices;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.nio.file.Path;

public final class ChunkpermitsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Chunkpermits.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ChunkPermitsFabricCommands.register(dispatcher)
        );
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path configDir = server.getRunDirectory().toPath().resolve("config").resolve("chunkpermits");
            Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            ChunkPermitsServices.init(configDir, worldDir.resolve("chunkpermits.db"));
        });
    }
}
//?}
