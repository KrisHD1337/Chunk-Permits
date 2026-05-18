package ch.krishd.chunkpermits.claim;

import java.util.UUID;

public record Claim(ClaimKey key, UUID owner, String ownerName) {
}
