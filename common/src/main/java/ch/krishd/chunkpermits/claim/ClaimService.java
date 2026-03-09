package ch.krishd.chunkpermits.claim;

import ch.krishd.chunkpermits.protection.AccessResult;
import ch.krishd.chunkpermits.storage.ClaimRepository;

import java.util.Optional;
import java.util.UUID;

public final class ClaimService {
    private final ClaimRepository claimRepository;

    public ClaimService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public AccessResult claim(UUID playerId, String playerName, ClaimKey key) {
        if (claimRepository.isClaimed(key)) {
            return AccessResult.deny("§cThis chunk is already claimed.");
        }

        claimRepository.save(new Claim(key, playerId, playerName));
        return AccessResult.allow();
    }

    public AccessResult unclaim(UUID playerId, ClaimKey key) {
        Optional<Claim> claim = claimRepository.findByKey(key);

        if (claim.isEmpty()) {
            return AccessResult.deny("This chunk is not claimed.");
        }

        if (!claim.get().owner().equals(playerId)) {
            return AccessResult.deny("§cYou do not own this claim.");
        }

        claimRepository.delete(key);
        return AccessResult.allow();
    }

    public Optional<Claim> getClaim(ClaimKey key) {
        return claimRepository.findByKey(key);
    }
}