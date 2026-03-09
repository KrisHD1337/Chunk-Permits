package ch.krishd.chunkpermits;

import ch.krishd.chunkpermits.claim.ClaimService;
import ch.krishd.chunkpermits.protection.AccessService;
import ch.krishd.chunkpermits.storage.ClaimRepository;
import ch.krishd.chunkpermits.storage.SqliteClaimRepository;

import java.nio.file.Path;

public final class ChunkPermitsServices {
    public static ClaimRepository CLAIM_REPOSITORY;
    public static AccessService ACCESS_SERVICE;
    public static ClaimService CLAIM_SERVICE;

    private ChunkPermitsServices() {
    }

    public static void init(Path dataDirectory) {
        CLAIM_REPOSITORY = new SqliteClaimRepository(dataDirectory.resolve("chunkpermits.db"));
        ACCESS_SERVICE = new AccessService(CLAIM_REPOSITORY);
        CLAIM_SERVICE = new ClaimService(CLAIM_REPOSITORY);
    }
}