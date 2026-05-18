package ch.krishd.chunkpermits.storage;

import ch.krishd.chunkpermits.trust.ClaimTrust;
import ch.krishd.chunkpermits.trust.TrustRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SqliteTrustRepository implements TrustRepository {
    private final Connection connection;

    public SqliteTrustRepository(Connection connection) {
        this.connection = connection;
        initDatabase();
    }

    private void initDatabase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS claim_trusts (
                    owner_uuid TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    trusted_player_uuid TEXT NOT NULL,
                    trusted_player_name TEXT NOT NULL,
                    PRIMARY KEY (owner_uuid, trusted_player_uuid)
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize trust database", e);
        }
    }

    @Override
    public void save(ClaimTrust trust) {
        String sql = """
                INSERT INTO claim_trusts (owner_uuid, owner_name, trusted_player_uuid, trusted_player_name)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(owner_uuid, trusted_player_uuid)
                DO UPDATE SET
                    owner_name = excluded.owner_name,
                    trusted_player_name = excluded.trusted_player_name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trust.owner().toString());
            statement.setString(2, trust.ownerName());
            statement.setString(3, trust.trustedPlayer().toString());
            statement.setString(4, trust.trustedPlayerName());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save trust", e);
        }
    }

    @Override
    public void delete(UUID owner, UUID trustedPlayer) {
        String sql = """
                DELETE FROM claim_trusts
                WHERE owner_uuid = ? AND trusted_player_uuid = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner.toString());
            statement.setString(2, trustedPlayer.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete trust", e);
        }
    }

    @Override
    public boolean isTrusted(UUID owner, UUID trustedPlayer) {
        String sql = """
                SELECT 1
                FROM claim_trusts
                WHERE owner_uuid = ? AND trusted_player_uuid = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner.toString());
            statement.setString(2, trustedPlayer.toString());

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check trust", e);
        }
    }

    @Override
    public List<ClaimTrust> findByOwner(UUID owner) {
        String sql = """
                SELECT owner_name, trusted_player_uuid, trusted_player_name
                FROM claim_trusts
                WHERE owner_uuid = ?
                ORDER BY trusted_player_name ASC
                """;

        List<ClaimTrust> trusts = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner.toString());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    trusts.add(new ClaimTrust(
                            owner,
                            rs.getString("owner_name"),
                            UUID.fromString(rs.getString("trusted_player_uuid")),
                            rs.getString("trusted_player_name")
                    ));
                }
            }

            return trusts;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find trusts by owner", e);
        }
    }

    @Override
    public List<ClaimTrust> findByTrustedPlayer(UUID trustedPlayer) {
        String sql = """
                SELECT owner_uuid, owner_name, trusted_player_name
                FROM claim_trusts
                WHERE trusted_player_uuid = ?
                ORDER BY owner_name ASC
                """;

        List<ClaimTrust> trusts = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trustedPlayer.toString());

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    trusts.add(new ClaimTrust(
                            UUID.fromString(rs.getString("owner_uuid")),
                            rs.getString("owner_name"),
                            trustedPlayer,
                            rs.getString("trusted_player_name")
                    ));
                }
            }

            return trusts;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find trusts by trusted player", e);
        }
    }
}