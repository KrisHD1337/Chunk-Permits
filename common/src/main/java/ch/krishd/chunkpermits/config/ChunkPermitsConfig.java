package ch.krishd.chunkpermits.config;

public record ChunkPermitsConfig(
        ClaimRules claimRules
) {
    public static ChunkPermitsConfig defaults() {
        return new ChunkPermitsConfig(
                new ClaimRules(
                        new ClaimCost("minecraft:diamond", 16),
                        16
                )
        );
    }
}