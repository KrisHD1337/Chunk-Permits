package ch.krishd.chunkpermits.raid;

import ch.krishd.chunkpermits.config.RaidRules;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class RaidService {
    private final RaidRepository raidRepository;
    private final RaidRules raidRules;

    public RaidService(RaidRepository raidRepository, RaidRules raidRules) {
        this.raidRepository = raidRepository;
        this.raidRules = raidRules;
    }

    public void startRaid(UUID attacker, UUID victim, long now) {
        if (!raidRules.enabled()) {
            return;
        }

        long expiresAt = now + (raidRules.durationSeconds() * 1000L);
        raidRepository.save(new RaidAccess(attacker, victim, expiresAt));
    }

    public boolean hasRaidAccess(UUID attacker, UUID victim, long now, Predicate<UUID> onlineChecker) {
        if (!raidRules.enabled()) {
            return false;
        }

        raidRepository.deleteExpired(now);

        if (raidRules.victimMustBeOnline() && !onlineChecker.test(victim)) {
            return false;
        }

        return raidRepository.findActive(attacker, victim, now).isPresent();
    }

    public RaidRules getRaidRules() {
        return raidRules;
    }
}