package ch.krishd.chunkpermits.neoforge;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import ch.krishd.chunkpermits.Chunkpermits;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Path;

@Mod(Chunkpermits.MOD_ID)
public final class ChunkPermitsNeoForge {
    public ChunkPermitsNeoForge(IEventBus modEventBus) {
        Path configDir = FMLPaths.GAMEDIR.get().resolve("config").resolve("chunkpermits");
        // Run our common setup.
        ChunkPermitsServices.init(configDir);
        Chunkpermits.init();

        NeoForge.EVENT_BUS.register(ChunkPermitsNeoForgeEvents.class);
        NeoForge.EVENT_BUS.register(ChunkPermitsNeoForgeCommandEvents.class);
    }
}
