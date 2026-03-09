package ch.krishd.chunkpermits.neoforge;

import ch.krishd.chunkpermits.Chunkpermits;
import net.neoforged.fml.common.Mod;

@Mod(Chunkpermits.MOD_ID)
public final class ChunkpermitsNeoForge {
    public ChunkpermitsNeoForge() {
        // Run our common setup.
        Chunkpermits.init();
    }
}
