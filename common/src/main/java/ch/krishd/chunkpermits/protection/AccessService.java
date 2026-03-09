package ch.krishd.chunkpermits.protection;

import ch.krishd.chunkpermits.claim.ClaimKey;
import ch.krishd.chunkpermits.storage.ClaimRepository;

import java.util.UUID;

public final class AccessService {
    private final ClaimRepository claimRepository;

    public AccessService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public AccessResult canBreak(UUID actor, ClaimKey key) {
        if (!claimRepository.isClaimed(key)) {
            return AccessResult.allow();
        }

        if (claimRepository.isOwner(key, actor)) {
            return AccessResult.allow();
        }

        return AccessResult.deny("§cYou can't break in this claimed chunk.");
    }

    public AccessResult canPlace(UUID actor, ClaimKey key) {
        if (!claimRepository.isClaimed(key)) {
            return AccessResult.allow();
        }

        if (claimRepository.isOwner(key, actor)) {
            return AccessResult.allow();
        }

        return AccessResult.deny("§cYou can't build in this claimed chunk.");
    }

    public AccessResult canInteract(UUID actor, ClaimKey key) {
        if (!claimRepository.isClaimed(key)) {
            return AccessResult.allow();
        }

        if (claimRepository.isOwner(key, actor)) {
            return AccessResult.allow();
        }

        return AccessResult.deny("§cYou can't interact in this claimed chunk.");
    }

    public AccessResult canOpenContainer(UUID actor, ClaimKey key) {
        if (!claimRepository.isClaimed(key)) {
            return AccessResult.allow();
        }

        if (claimRepository.isOwner(key, actor)) {
            return AccessResult.allow();
        }

        return AccessResult.deny("§cYou can't open containers in this claimed chunk.");
    }
}