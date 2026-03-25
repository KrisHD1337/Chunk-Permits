package ch.krishd.chunkpermits.storage;

import ch.krishd.chunkpermits.raid.RaidAccess;
import ch.krishd.chunkpermits.raid.RaidRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class SqliteRaidRepository implements RaidRepository {
    private final Connection connection;

    public SqliteRaidRepository(Connection connection) {
        this.connection = connection;
        initDatabase();
    }

    private void initDatabase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS raids (
                    attacker_uuid TEXT NOT NULL,
                    victim_uuid TEXT NOT NULL,
                    expires_at INTEGER NOT NULL,
                    PRIMARY KEY (attacker_uuid, victim_uuid)
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize raids database", e);
        }
    }

    @Override
    public Optional<RaidAccess> findActive(UUID attacker, UUID victim, long now) {
        String sql = """
                SELECT expires_at
                FROM raids
                WHERE attacker_uuid = ? AND victim_uuid = ? AND expires_at > ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, attacker.toString());
            statement.setString(2, victim.toString());
            statement.setLong(3, now);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                long expiresAt = resultSet.getLong("expires_at");
                return Optional.of(new RaidAccess(attacker, victim, expiresAt));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active raid", e);
        }
    }

    @Override
    public void save(RaidAccess raidAccess) {
        String sql = """
                INSERT INTO raids (attacker_uuid, victim_uuid, expires_at)
                VALUES (?, ?, ?)
                ON CONFLICT(attacker_uuid, victim_uuid)
                DO UPDATE SET expires_at = excluded.expires_at
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, raidAccess.attacker().toString());
            statement.setString(2, raidAccess.victim().toString());
            statement.setLong(3, raidAccess.expiresAtEpochMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save raid access", e);
        }
    }

    @Override
    public void delete(UUID attacker, UUID victim) {
        String sql = """
                DELETE FROM raids
                WHERE attacker_uuid = ? AND victim_uuid = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, attacker.toString());
            statement.setString(2, victim.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete raid access", e);
        }
    }

    @Override
    public void deleteExpired(long now) {
        String sql = """
                DELETE FROM raids
                WHERE expires_at <= ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete expired raid access", e);
        }
    }
}
