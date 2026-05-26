//? if fabric {
package ch.krishd.chunkpermits.fabric;

import net.minecraft.world.level.Level;

public final class FabricLevelKeys {
    private FabricLevelKeys() {
    }

    public static String levelKey(Level level) {
        Object dimensionKey = level.dimension();

        for (String methodName : new String[]{"location", "identifier"}) {
            try {
                Object value = dimensionKey.getClass().getMethod(methodName).invoke(dimensionKey);
                return value.toString();
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return dimensionKey.toString();
    }
}
//?}
