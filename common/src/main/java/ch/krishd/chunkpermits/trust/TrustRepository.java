package ch.krishd.chunkpermits.trust;

import java.util.List;
import java.util.UUID;

public interface TrustRepository {
    void save(ClaimTrust trust);

    void delete(UUID owner, UUID trustedPlayer);

    boolean isTrusted(UUID owner, UUID trustedPlayer);

    List<ClaimTrust> findByOwner(UUID owner);

    List<ClaimTrust> findByTrustedPlayer(UUID trustedPlayer);
}
