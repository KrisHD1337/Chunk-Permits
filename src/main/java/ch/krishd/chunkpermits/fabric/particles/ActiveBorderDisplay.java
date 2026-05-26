//? if fabric {
package ch.krishd.chunkpermits.fabric.particles;

import java.util.UUID;

public record ActiveBorderDisplay(
        UUID playerId,
        long expiresAtMillis
) {
}
//?}
