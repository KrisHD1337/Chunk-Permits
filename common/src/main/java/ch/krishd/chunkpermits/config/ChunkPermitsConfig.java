package ch.krishd.chunkpermits.config;

public record ChunkPermitsConfig(
        ClaimRules claimRules,
        RaidRules raidRules,
        BorderRules borderRules
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
                ),
                new BorderRules(
                        15,
                        8,
                        "minecraft:end_rod",
                        "minecraft:happy_villager"
                )
        );
    }
}