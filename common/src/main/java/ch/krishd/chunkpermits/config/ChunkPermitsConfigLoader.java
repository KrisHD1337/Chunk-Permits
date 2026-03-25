package ch.krishd.chunkpermits.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ChunkPermitsConfigLoader {
    private ChunkPermitsConfigLoader() {
    }

    public static ChunkPermitsConfig load(Path configDirectory) {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }

        Path configFile = configDirectory.resolve("chunkpermits.properties");

        if (Files.notExists(configFile)) {
            writeDefaults(configFile);
        }

        Properties properties = new Properties();

        try (InputStream inputStream = Files.newInputStream(configFile)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + configFile, e);
        }

        String costItemId = properties.getProperty("claim.cost.item", "minecraft:diamond");
        int costAmount = parseInt(properties.getProperty("claim.cost.amount"), 16, "claim.cost.amount");
        int maxClaims = parseInt(properties.getProperty("claim.max-claims-per-player"), 16, "claim.max-claims-per-player");

        boolean raidEnabled = Boolean.parseBoolean(properties.getProperty("raid.enabled", "true"));
        int raidDurationSeconds = parseInt(properties.getProperty("raid.duration-seconds"), 1800, "raid.duration-seconds");
        boolean victimMustBeOnline = Boolean.parseBoolean(properties.getProperty("raid.victim-must-be-online", "true"));

        return new ChunkPermitsConfig(
                new ClaimRules(
                        new ClaimCost(costItemId, costAmount),
                        maxClaims
                ),
                new RaidRules(
                        raidEnabled,
                        raidDurationSeconds,
                        victimMustBeOnline
                )
        );
    }

    private static void writeDefaults(Path configFile) {
        Properties defaults = new Properties();
        defaults.setProperty("claim.cost.item", "minecraft:diamond");
        defaults.setProperty("claim.cost.amount", "16");
        defaults.setProperty("claim.max-claims-per-player", "16");
        defaults.setProperty("raid.enabled", "true");
        defaults.setProperty("raid.duration-seconds", "1800");
        defaults.setProperty("raid.victim-must-be-online", "true");

        try (OutputStream outputStream = Files.newOutputStream(configFile)) {
            defaults.store(outputStream, "Chunk Permits configuration");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write default config file: " + configFile, e);
        }
    }

    private static int parseInt(String value, int fallback, String key) {
        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value for config key '" + key + "': " + value, e);
        }
    }
}
