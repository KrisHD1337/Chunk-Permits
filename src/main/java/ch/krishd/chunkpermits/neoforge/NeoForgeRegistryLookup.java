//? if neoforge {
/*package ch.krishd.chunkpermits.neoforge;

import net.minecraft.core.Registry;

import java.lang.reflect.Method;
import java.util.Optional;

public final class NeoForgeRegistryLookup {
    private NeoForgeRegistryLookup() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getOptional(Registry<T> registry, String id) {
        Object parsedId = parseIdentifier(id);
        if (parsedId == null) {
            return Optional.empty();
        }

        try {
            Method getOptional = registry.getClass().getMethod("getOptional", parsedId.getClass());
            return (Optional<T>) getOptional.invoke(registry, parsedId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to query registry for id: " + id, exception);
        }
    }

    private static Object parseIdentifier(String id) {
        for (String className : new String[]{
                "net.minecraft.resources.ResourceLocation",
                "net.minecraft.resources.Identifier"
        }) {
            try {
                Class<?> idClass = Class.forName(className);
                return idClass.getMethod("tryParse", String.class).invoke(null, id);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }
}
*///?}
