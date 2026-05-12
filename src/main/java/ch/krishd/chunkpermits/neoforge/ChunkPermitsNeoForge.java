//? if neoforge {
package ch.krishd.chunkpermits.neoforge;

import ch.krishd.chunkpermits.Chunkpermits;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Chunkpermits.MOD_ID)
public final class ChunkPermitsNeoForge {
    public ChunkPermitsNeoForge(IEventBus modEventBus) {
        // Run our common setup.
        Chunkpermits.init();

        NeoForge.EVENT_BUS.register(ChunkPermitsNeoForgeEvents.class);
        NeoForge.EVENT_BUS.register(ChunkPermitsNeoForgeCommandEvents.class);
    }
}//?}
