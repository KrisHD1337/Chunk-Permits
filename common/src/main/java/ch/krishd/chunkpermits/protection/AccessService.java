package ch.krishd.chunkpermits.protection;

import ch.krishd.chunkpermits.claim.Claim;
import ch.krishd.chunkpermits.claim.ClaimKey;
import ch.krishd.chunkpermits.raid.RaidService;
import ch.krishd.chunkpermits.storage.ClaimRepository;
import ch.krishd.chunkpermits.trust.TrustService;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class AccessService {
    private final ClaimRepository claimRepository;
    private final RaidService raidService;
    private final TrustService trustService;

    public AccessService(ClaimRepository claimRepository, RaidService raidService, TrustService trustService) {
        this.claimRepository = claimRepository;
        this.raidService = raidService;
        this.trustService = trustService;
    }

    public AccessResult canBreak(UUID actor, ClaimKey key, Predicate<UUID> onlineChecker, long now) {
        return canAccessClaim(actor, key, onlineChecker, now, "§cYou can't break in this claimed chunk.");
    }

    public AccessResult canPlace(UUID actor, ClaimKey key, Predicate<UUID> onlineChecker, long now) {
        return canAccessClaim(actor, key, onlineChecker, now, "§cYou can't build in this claimed chunk.");
    }

    public AccessResult canInteract(UUID actor, ClaimKey key, Predicate<UUID> onlineChecker, long now) {
        return canAccessClaim(actor, key, onlineChecker, now, "§cYou can't interact in this claimed chunk.");
    }

    public AccessResult canOpenContainer(UUID actor, ClaimKey key, Predicate<UUID> onlineChecker, long now) {
        return canAccessClaim(actor, key, onlineChecker, now, "§cYou can't open containers in this claimed chunk.");
    }

    private AccessResult canAccessClaim(UUID actor, ClaimKey key, Predicate<UUID> onlineChecker, long now, String denyMessage) {
        Optional<Claim> optionalClaim = claimRepository.findByKey(key);
        if (optionalClaim.isEmpty()) {
            return AccessResult.allow();
        }

        Claim claim = optionalClaim.get();
        UUID owner = claim.owner();

        if (claim.owner().equals(actor)) {
            return AccessResult.allow();
        }

        if (trustService.isTrusted(owner, actor)) {
            return AccessResult.allow();
        }

        if (raidService.hasRaidAccess(actor, claim.owner(), now, onlineChecker)) {
            return AccessResult.allow();
        }

        for (var trust : trustService.getTrustedPlayers(owner)) {
            if (raidService.hasRaidAccess(actor, trust.trustedPlayer(), now, onlineChecker)) {
                return AccessResult.allow();
            }
        }

        return AccessResult.deny(denyMessage);
    }
}
