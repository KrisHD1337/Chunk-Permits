package ch.krishd.chunkpermits.raid;

import java.util.UUID;

public record RaidAccess(
        UUID attacker,
        UUID victim,
        long expiresAtEpochMillis
) {
}
