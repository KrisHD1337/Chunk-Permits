package ch.krishd.chunkpermits.raid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RaidRepository {
    Optional<RaidAccess> findActive(UUID attacker, UUID victim, long now);

    List<RaidAccess> findActiveByAttacker(UUID attacker, long now);

    List<RaidAccess> findActiveByVictim(UUID victim, long now);

    void save(RaidAccess raidAccess);

    void delete(UUID attacker, UUID victim);

    void deleteExpired(long now);
}
