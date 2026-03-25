package ch.krishd.chunkpermits.storage;

import ch.krishd.chunkpermits.claim.Claim;
import ch.krishd.chunkpermits.claim.ClaimKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository {
    Optional<Claim> findByKey(ClaimKey key);

    boolean isClaimed(ClaimKey key);

    void save(Claim claim);

    void delete(ClaimKey key);

    boolean isOwner(ClaimKey key, UUID playerId);

    int countByOwner(UUID playerId);

    List<Claim> findInChunkRange(String levelKey, int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ);
}
