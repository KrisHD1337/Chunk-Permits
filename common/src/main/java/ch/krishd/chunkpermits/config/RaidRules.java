package ch.krishd.chunkpermits.config;

public record RaidRules(
        boolean enabled,
        int durationSeconds,
        boolean victimMustBeOnline
) {
}
