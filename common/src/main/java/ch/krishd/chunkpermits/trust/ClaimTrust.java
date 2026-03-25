package ch.krishd.chunkpermits.trust;

import java.util.UUID;

public record ClaimTrust(
        UUID owner,
        String ownerName,
        UUID trustedPlayer,
        String trustedPlayerName
) {
}