package ch.krishd.chunkpermits.storage;

import ch.krishd.chunkpermits.claim.Claim;
import ch.krishd.chunkpermits.claim.ClaimKey;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public final class SqliteClaimRepository implements ClaimRepository {
    private final String jdbcUrl;
    private final Driver sqliteDriver;

    public SqliteClaimRepository(Path databasePath) {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new RuntimeException("§cFailed to create database directories", e);
        }

        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
        this.sqliteDriver = createSqliteDriver();
        initDatabase();
    }

    private static Driver createSqliteDriver() {
        ClassLoader[] candidates = new ClassLoader[] {
                SqliteClaimRepository.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader()
        };

        for (ClassLoader loader : candidates) {
            if (loader == null) {
                continue;
            }

            try {
                Class<?> rawClass = Class.forName("org.sqlite.JDBC", true, loader);
                Object instance = rawClass.getDeclaredConstructor().newInstance();
                if (instance instanceof Driver driver) {
                    return driver;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try next classloader.
            }
        }

        throw new RuntimeException("SQLite JDBC driver not found on runtime classpath");
    }

    private Connection openConnection() throws SQLException {
        Connection connection = sqliteDriver.connect(jdbcUrl, new Properties());
        if (connection == null) {
            throw new SQLException("SQLite driver rejected URL: " + jdbcUrl);
        }
        return connection;
    }

    private void initDatabase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS claims (
                    level_key TEXT NOT NULL,
                    chunk_x INTEGER NOT NULL,
                    chunk_z INTEGER NOT NULL,
                    owner_uuid TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    PRIMARY KEY (level_key, chunk_x, chunk_z)
                )
                """;

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("§cFailed to initialize claims database", e);
        }
    }

    @Override
    public Optional<Claim> findByKey(ClaimKey key) {
        String sql = """
                SELECT owner_uuid, owner_name
                FROM claims
                WHERE level_key = ? AND chunk_x = ? AND chunk_z = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, key.levelKey());
            statement.setInt(2, key.chunkX());
            statement.setInt(3, key.chunkZ());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                UUID owner = UUID.fromString(resultSet.getString("owner_uuid"));
                String ownerName = resultSet.getString("owner_name");

                return Optional.of(new Claim(key, owner, ownerName));
            }
        } catch (SQLException e) {
            throw new RuntimeException("§cFailed to find claim for key: " + key, e);
        }
    }

    @Override
    public boolean isClaimed(ClaimKey key) {
        String sql = """
                SELECT 1
                FROM claims
                WHERE level_key = ? AND chunk_x = ? AND chunk_z = ?
                LIMIT 1
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, key.levelKey());
            statement.setInt(2, key.chunkX());
            statement.setInt(3, key.chunkZ());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("§cFailed to check if chunk is claimed: " + key, e);
        }
    }

    @Override
    public void save(Claim claim) {
        String sql = """
                INSERT INTO claims (level_key, chunk_x, chunk_z, owner_uuid, owner_name)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(level_key, chunk_x, chunk_z)
                DO UPDATE SET
                    owner_uuid = excluded.owner_uuid,
                    owner_name = excluded.owner_name
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, claim.key().levelKey());
            statement.setInt(2, claim.key().chunkX());
            statement.setInt(3, claim.key().chunkZ());
            statement.setString(4, claim.owner().toString());
            statement.setString(5, claim.ownerName());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("§cFailed to save claim: " + claim, e);
        }
    }

    @Override
    public void delete(ClaimKey key) {
        String sql = """
                DELETE FROM claims
                WHERE level_key = ? AND chunk_x = ? AND chunk_z = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, key.levelKey());
            statement.setInt(2, key.chunkX());
            statement.setInt(3, key.chunkZ());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("§cFailed to delete claim: " + key, e);
        }
    }

    @Override
    public boolean isOwner(ClaimKey key, UUID playerId) {
        String sql = """
                SELECT owner_uuid
                FROM claims
                WHERE level_key = ? AND chunk_x = ? AND chunk_z = ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, key.levelKey());
            statement.setInt(2, key.chunkX());
            statement.setInt(3, key.chunkZ());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }

                UUID owner = UUID.fromString(resultSet.getString("owner_uuid"));
                return owner.equals(playerId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("§cFailed to check owner for claim: " + key, e);
        }
    }
}
