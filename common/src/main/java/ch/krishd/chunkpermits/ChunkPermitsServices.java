package ch.krishd.chunkpermits;

import ch.krishd.chunkpermits.claim.ClaimService;
import ch.krishd.chunkpermits.config.ChunkPermitsConfig;
import ch.krishd.chunkpermits.config.ChunkPermitsConfigLoader;
import ch.krishd.chunkpermits.protection.AccessService;
import ch.krishd.chunkpermits.storage.ClaimRepository;
import ch.krishd.chunkpermits.storage.SqliteClaimRepository;

import java.nio.file.Path;

public final class ChunkPermitsServices {
    public static ClaimRepository CLAIM_REPOSITORY;
    public static AccessService ACCESS_SERVICE;
    public static ClaimService CLAIM_SERVICE;
    public static ChunkPermitsConfig CONFIG;

    private ChunkPermitsServices() {
    }

    public static void init(Path configDirectory, Path databasePath) {
        CONFIG = ChunkPermitsConfigLoader.load(configDirectory);
        CLAIM_REPOSITORY = new SqliteClaimRepository(databasePath);
        ACCESS_SERVICE = new AccessService(CLAIM_REPOSITORY);
        CLAIM_SERVICE = new ClaimService(CLAIM_REPOSITORY, CONFIG.claimRules());
    }
}