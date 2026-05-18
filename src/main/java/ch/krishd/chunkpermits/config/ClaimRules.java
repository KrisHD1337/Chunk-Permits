package ch.krishd.chunkpermits.config;

public record ClaimRules(
        ClaimCost claimCost,
        int maxClaimsPerPlayer) {
}
