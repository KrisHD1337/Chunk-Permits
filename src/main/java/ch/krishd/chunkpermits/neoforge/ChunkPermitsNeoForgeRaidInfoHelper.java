//? if neoforge {
/*package ch.krishd.chunkpermits.neoforge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class ChunkPermitsNeoForgeRaidInfoHelper {
    private ChunkPermitsNeoForgeRaidInfoHelper() {
    }

    public static String resolvePlayerName(MinecraftServer server, UUID uuid) {
        if (server == null) {
            return uuid.toString();
        }

        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }

        return uuid.toString();
    }

    public static String formatRemainingTime(long remainingMillis) {
        long totalSeconds = Math.max(0L, remainingMillis / 1000L);

        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }

        return seconds + "s";
    }
}*///?}
