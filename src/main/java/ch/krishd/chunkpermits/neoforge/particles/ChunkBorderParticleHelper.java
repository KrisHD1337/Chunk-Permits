//? if neoforge {
package ch.krishd.chunkpermits.neoforge.particles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class ChunkBorderParticleHelper {
    private ChunkBorderParticleHelper() {
    }

    public static void showChunkBorder(ServerPlayer player, ChunkPos chunkPos, ParticleOptions particle) {
        ServerLevel level = player.serverLevel();

        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();

        int baseY = player.blockPosition().getY();
        int maxY = baseY + 3;

        for (int y = baseY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                level.sendParticles(player, particle, true, x + 0.5, y + 0.1, minZ + 0.5, 1, 0, 0, 0, 0);
                level.sendParticles(player, particle, true, x + 0.5, y + 0.1, maxZ + 0.5, 1, 0, 0, 0, 0);
            }

            for (int z = minZ; z <= maxZ; z++) {
                level.sendParticles(player, particle, true, minX + 0.5, y + 0.1, z + 0.5, 1, 0, 0, 0, 0);
                level.sendParticles(player, particle, true, maxX + 0.5, y + 0.1, z + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }
}//?}
