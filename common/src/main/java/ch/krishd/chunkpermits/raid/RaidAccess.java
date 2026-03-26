package ch.krishd.chunkpermits.raid;

import java.util.UUID;

public record RaidAccess(
        UUID attacker,
        String attackerName,
        UUID victim,
        String victimName,
        long expiresAtEpochMillis
) {
}
