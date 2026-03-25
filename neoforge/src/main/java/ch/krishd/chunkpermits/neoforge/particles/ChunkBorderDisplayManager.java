package ch.krishd.chunkpermits.neoforge.particles;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkBorderDisplayManager {
    private static final Map<UUID, ActiveBorderDisplay> ACTIVE = new ConcurrentHashMap<>();

    private ChunkBorderDisplayManager() {
    }

    public static void startDisplay(ServerPlayer player) {
        long durationMillis = ChunkPermitsServices.CONFIG.borderRules().durationSeconds() * 1000L;
        ACTIVE.put(player.getUUID(), new ActiveBorderDisplay(player.getUUID(), System.currentTimeMillis() + durationMillis));
    }

    public static void tick(ServerPlayer player) {
        ActiveBorderDisplay display = ACTIVE.get(player.getUUID());
        if (display == null) {
            return;
        }

        if (System.currentTimeMillis() >= display.expiresAtMillis()) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        var visibleClaims = ChunkBorderClaimResolver.findVisibleClaims(player);

        for (VisibleClaim visibleClaim : visibleClaims) {
            ChunkPos chunkPos = new ChunkPos(
                    visibleClaim.claim().key().chunkX(),
                    visibleClaim.claim().key().chunkZ()
            );

            if (visibleClaim.type() == VisibleClaimType.OWN) {
                ChunkBorderParticleHelper.showChunkBorder(
                        player,
                        chunkPos,
                        ChunkBorderParticleResolver.resolveOwnClaimParticle()
                );
            } else {
                ChunkBorderParticleHelper.showChunkBorder(
                        player,
                        chunkPos,
                        ChunkBorderParticleResolver.resolveTrustedClaimParticle()
                );
            }
        }
    }

    public static void cleanupOffline(java.util.function.Predicate<UUID> onlineChecker) {
        Iterator<Map.Entry<UUID, ActiveBorderDisplay>> iterator = ACTIVE.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveBorderDisplay> entry = iterator.next();
            if (!onlineChecker.test(entry.getKey())) {
                iterator.remove();
            }
        }
    }
}