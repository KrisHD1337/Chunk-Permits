package ch.krishd.chunkpermits;

import ch.krishd.chunkpermits.claim.ClaimService;
import ch.krishd.chunkpermits.config.ChunkPermitsConfig;
import ch.krishd.chunkpermits.config.ChunkPermitsConfigLoader;
import ch.krishd.chunkpermits.protection.AccessService;
import ch.krishd.chunkpermits.raid.RaidRepository;
import ch.krishd.chunkpermits.raid.RaidService;
import ch.krishd.chunkpermits.storage.ClaimRepository;
import ch.krishd.chunkpermits.storage.SqliteClaimRepository;
import ch.krishd.chunkpermits.storage.SqliteDatabase;
import ch.krishd.chunkpermits.storage.SqliteRaidRepository;

import java.nio.file.Path;

public final class ChunkPermitsServices {
    public static SqliteDatabase DATABASE;
    public static RaidRepository RAID_REPOSITORY;
    public static RaidService RAID_SERVICE;
    public static ClaimRepository CLAIM_REPOSITORY;
    public static AccessService ACCESS_SERVICE;
    public static ClaimService CLAIM_SERVICE;
    public static ChunkPermitsConfig CONFIG;

    private ChunkPermitsServices() {
    }

    public static void init(Path configDirectory, Path databasePath) {
        CONFIG = ChunkPermitsConfigLoader.load(configDirectory);
        DATABASE = new SqliteDatabase(databasePath);
        CLAIM_REPOSITORY = new SqliteClaimRepository(DATABASE.connection());
        RAID_REPOSITORY = new SqliteRaidRepository(DATABASE.connection());
        CLAIM_SERVICE = new ClaimService(CLAIM_REPOSITORY, CONFIG.claimRules());
        RAID_SERVICE = new RaidService(RAID_REPOSITORY, CONFIG.raidRules());
        ACCESS_SERVICE = new AccessService(CLAIM_REPOSITORY, RAID_SERVICE);
    }
}