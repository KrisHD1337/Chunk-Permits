package ch.krishd.chunkpermits.claim;

import ch.krishd.chunkpermits.config.ClaimRules;
import ch.krishd.chunkpermits.protection.AccessResult;
import ch.krishd.chunkpermits.storage.ClaimRepository;

import java.util.Optional;
import java.util.UUID;

public final class ClaimService {
    private final ClaimRepository claimRepository;
    private final ClaimRules rules;

    public ClaimService(ClaimRepository claimRepository, ClaimRules rules) {
        this.claimRepository = claimRepository;
        this.rules = rules;
    }

    public AccessResult claim(UUID playerId, String playerName, ClaimKey key, ClaimAttemptContext context) {
        int currentClaims = claimRepository.countByOwner(playerId);
        if (currentClaims >= rules.maxClaimsPerPlayer()) {
            return AccessResult.deny("§cYou have reached the maximum number of claims.");
        }
        if (claimRepository.isClaimed(key)) {
            return AccessResult.deny("§cThis chunk is already claimed.");
        }

        int cost = rules.claimCost().amount();
        if (context.availableCostItems() < cost) {
            return AccessResult.deny("§cYou need " + cost + " " + rules.claimCost().itemId() + " to claim this chunk.");
        }

        claimRepository.save(new Claim(key, playerId, playerName));
        return AccessResult.allow();
    }

    public AccessResult unclaim(UUID playerId, ClaimKey key) {
        Optional<Claim> claim = claimRepository.findByKey(key);

        if (claim.isEmpty()) {
            return AccessResult.deny("§cThis chunk is not claimed.");
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

    public int countClaims(UUID playerId) {
        return claimRepository.countByOwner(playerId);
    }

    public ClaimRules getClaimRules() {
        return rules;
    }
}