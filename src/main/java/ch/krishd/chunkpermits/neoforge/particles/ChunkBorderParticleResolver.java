//? if neoforge {
package ch.krishd.chunkpermits.neoforge.particles;

import ch.krishd.chunkpermits.ChunkPermitsServices;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ChunkBorderParticleResolver {
    private ChunkBorderParticleResolver() {
    }

    public static ParticleOptions resolveOwnClaimParticle() {
        return resolveParticle(ChunkPermitsServices.CONFIG.borderRules().ownClaimParticle());
    }

    public static ParticleOptions resolveTrustedClaimParticle() {
        return resolveParticle(ChunkPermitsServices.CONFIG.borderRules().trustedClaimParticle());
    }

    private static ParticleOptions resolveParticle(String particleId) {
        var particleType = BuiltInRegistries.PARTICLE_TYPE.getOptional(ResourceLocation.tryParse(particleId))
                .orElseThrow(() -> new IllegalStateException("Unknown particle type: " + particleId));

        if (!(particleType instanceof ParticleOptions particleOptions)) {
            throw new IllegalStateException("Configured particle type is not a simple particle option: " + particleId);
        }

        return particleOptions;
    }
}//?}
