//? if neoforge {
/*package ch.krishd.chunkpermits.neoforge;

import net.minecraft.world.level.Level;

public final class NeoForgeLevelKeys {
    private NeoForgeLevelKeys() {
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
*///?}
