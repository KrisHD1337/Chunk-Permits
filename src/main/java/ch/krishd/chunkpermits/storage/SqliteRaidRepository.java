package ch.krishd.chunkpermits.storage;

import ch.krishd.chunkpermits.raid.RaidAccess;
import ch.krishd.chunkpermits.raid.RaidRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
                    attacker_name TEXT NOT NULL,
                    victim_uuid TEXT NOT NULL,
                    victim_name TEXT NOT NULL,
                    expires_at INTEGER NOT NULL,
                    PRIMARY KEY (attacker_uuid, victim_uuid)
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize raids database", e);
        }

        ensureColumnExists("attacker_name", "TEXT NOT NULL DEFAULT ''");
        ensureColumnExists("victim_name", "TEXT NOT NULL DEFAULT ''");
    }

    private void ensureColumnExists(String columnName, String definition) {
        try (PreparedStatement statement = connection.prepareStatement(
                "ALTER TABLE raids ADD COLUMN " + columnName + " " + definition
        )) {
            statement.execute();
        } catch (SQLException ignored) {
            // Column probably already exists already.
        }
    }

    @Override
    public Optional<RaidAccess> findActive(UUID attacker, UUID victim, long now) {
        String sql = """
                SELECT attacker_name, victim_name, expires_at
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

                String attackerName = resultSet.getString("attacker_name");
                String victimName = resultSet.getString("victim_name");
                long expiresAt = resultSet.getLong("expires_at");

                return Optional.of(new RaidAccess(attacker, attackerName, victim, victimName, expiresAt));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active raid", e);
        }
    }

    @Override
    public List<RaidAccess> findActiveByAttacker(UUID attacker, long now) {
        String sql = """
                SELECT attacker_name, victim_uuid, victim_name, expires_at
                FROM raids
                WHERE attacker_uuid = ? AND expires_at > ?
                ORDER BY expires_at ASC
                """;

        List<RaidAccess> raids = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, attacker.toString());
            statement.setLong(2, now);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String attackerName = resultSet.getString("attacker_name");
                    UUID victim = UUID.fromString(resultSet.getString("victim_uuid"));
                    String victimName = resultSet.getString("victim_name");
                    long expiresAt = resultSet.getLong("expires_at");

                    raids.add(new RaidAccess(attacker, attackerName, victim, victimName, expiresAt));
                }
            }

            return raids;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active raids by attacker: " + attacker, e);
        }
    }

    @Override
    public List<RaidAccess> findActiveByVictim(UUID victim, long now) {
        String sql = """
                SELECT attacker_uuid, attacker_name, victim_name, expires_at
                FROM raids
                WHERE victim_uuid = ? AND expires_at > ?
                ORDER BY expires_at ASC
                """;

        List<RaidAccess> raids = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, victim.toString());
            statement.setLong(2, now);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID attacker = UUID.fromString(resultSet.getString("attacker_uuid"));
                    String attackerName = resultSet.getString("attacker_name");
                    String victimName = resultSet.getString("victim_name");
                    long expiresAt = resultSet.getLong("expires_at");

                    raids.add(new RaidAccess(attacker, attackerName, victim, victimName, expiresAt));
                }
            }

            return raids;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active raids by victim: " + victim, e);
        }
    }

    @Override
    public void save(RaidAccess raidAccess) {
        String sql = """
                INSERT INTO raids (attacker_uuid, attacker_name, victim_uuid, victim_name, expires_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(attacker_uuid, victim_uuid)
                DO UPDATE SET
                    attacker_name = excluded.attacker_name,
                    victim_name = excluded.victim_name,
                    expires_at = excluded.expires_at
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, raidAccess.attacker().toString());
            statement.setString(2, raidAccess.attackerName());
            statement.setString(3, raidAccess.victim().toString());
            statement.setString(4, raidAccess.victimName());
            statement.setLong(5, raidAccess.expiresAtEpochMillis());
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
    @Override
    public void deleteByAttackerAndVictim(UUID attacker, UUID victim) {
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
}
