package ch.krishd.chunkpermits.config;

public record BorderRules(
        int durationSeconds,
        int radiusChunks,
        String ownClaimParticle,
        String trustedClaimParticle
) {
}