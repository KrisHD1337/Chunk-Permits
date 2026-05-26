//? if fabric {
package ch.krishd.chunkpermits.fabric.particles;

import ch.krishd.chunkpermits.claim.Claim;

public record VisibleClaim(
        Claim claim,
        VisibleClaimType type
) {
}
//?}
