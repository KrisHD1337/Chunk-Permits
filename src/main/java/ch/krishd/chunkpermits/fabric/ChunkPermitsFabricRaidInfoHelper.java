//? if fabric {
package ch.krishd.chunkpermits.fabric;

public final class ChunkPermitsFabricRaidInfoHelper {
    private ChunkPermitsFabricRaidInfoHelper() {
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
}
//?}
