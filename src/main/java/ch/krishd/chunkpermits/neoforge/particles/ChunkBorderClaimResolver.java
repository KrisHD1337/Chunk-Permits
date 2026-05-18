//? if neoforge {
package ch.krishd.chunkpermits.neoforge.particles;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import ch.krishd.chunkpermits.claim.Claim;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public final class ChunkBorderClaimResolver {
    private ChunkBorderClaimResolver() {
    }

    public static List<VisibleClaim> findVisibleClaims(ServerPlayer player) {
        String levelKey = player.level().dimension().location().toString();
        ChunkPos center = player.chunkPosition();

        int radius = ChunkPermitsServices.CONFIG.borderRules().radiusChunks();

        int minChunkX = center.x - radius;
        int maxChunkX = center.x + radius;
        int minChunkZ = center.z - radius;
        int maxChunkZ = center.z + radius;

        List<Claim> nearbyClaims = ChunkPermitsServices.CLAIM_REPOSITORY.findInChunkRange(
                levelKey,
                minChunkX,
                maxChunkX,
                minChunkZ,
                maxChunkZ
        );

        List<VisibleClaim> visibleClaims = new ArrayList<>();

        for (Claim claim : nearbyClaims) {
            if (claim.owner().equals(player.getUUID())) {
                visibleClaims.add(new VisibleClaim(claim, VisibleClaimType.OWN));
                continue;
            }

            if (ChunkPermitsServices.TRUST_SERVICE.isTrusted(claim.owner(), player.getUUID())) {
                visibleClaims.add(new VisibleClaim(claim, VisibleClaimType.TRUSTED));
            }
        }

        return visibleClaims;
    }
}//?}
