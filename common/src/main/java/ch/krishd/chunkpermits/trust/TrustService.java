package ch.krishd.chunkpermits.trust;

import ch.krishd.chunkpermits.protection.AccessResult;
import ch.krishd.chunkpermits.raid.RaidRepository;

import java.util.List;
import java.util.UUID;

public final class TrustService {
    private final TrustRepository trustRepository;
    private final RaidRepository raidRepository;

    public TrustService(TrustRepository trustRepository, RaidRepository raidRepository) {
        this.trustRepository = trustRepository;
        this.raidRepository = raidRepository;
    }

    public AccessResult addTrust(UUID owner, String ownerName, UUID trustedPlayer, String trustedPlayerName) {
        if (owner.equals(trustedPlayer)) {
            return AccessResult.deny("You cannot trust yourself.");
        }

        trustRepository.save(new ClaimTrust(owner, ownerName, trustedPlayer, trustedPlayerName));
        raidRepository.delete(trustedPlayer, owner);
        raidRepository.delete(owner, trustedPlayer);
        return AccessResult.allow();
    }

    public AccessResult removeTrust(UUID owner, UUID trustedPlayer) {
        trustRepository.delete(owner, trustedPlayer);
        return AccessResult.allow();
    }

    public boolean isTrusted(UUID owner, UUID trustedPlayer) {
        return trustRepository.isTrusted(owner, trustedPlayer);
    }

    public List<ClaimTrust> getTrustedPlayers(UUID owner) {
        return trustRepository.findByOwner(owner);
    }

    public List<ClaimTrust> getOwnersForTrustedPlayer(UUID trustedPlayer) {
        return trustRepository.findByTrustedPlayer(trustedPlayer);
    }
}