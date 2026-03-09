package ch.krishd.chunkpermits.neoforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ChunkPermitsNeoForgeCommandEvents {
    private ChunkPermitsNeoForgeCommandEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ChunkPermitsNeoForgeCommands.register(event.getDispatcher());
    }
}