package ch.krishd.chunkpermits.config;

public record ChunkPermitsConfig(
        ClaimRules claimRules,
        RaidRules raidRules
) {
    public static ChunkPermitsConfig defaults() {
        return new ChunkPermitsConfig(
                new ClaimRules(
                        new ClaimCost("minecraft:diamond", 16),
                        16
                ),
                new RaidRules(
                        true,
                        1800,
                        true
                )
        );
    }
}