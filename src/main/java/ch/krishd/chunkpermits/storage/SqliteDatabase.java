package ch.krishd.chunkpermits.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public final class SqliteDatabase {
    private final Connection connection;

    public SqliteDatabase(Path databasePath) {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create database directories", e);
        }

        Driver driver = createSqliteDriver();
        this.connection = createConnection(driver, databasePath);
    }

    public Connection connection() {
        return connection;
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close SQLite connection", e);
        }
    }

    private static Driver createSqliteDriver() {
        ClassLoader[] candidates = new ClassLoader[] {
                SqliteDatabase.class.getClassLoader(),
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
            }
        }

        throw new RuntimeException("SQLite JDBC driver not found on runtime classpath");
    }

    private static Connection createConnection(Driver sqliteDriver, Path databasePath) {
        String jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();

        try {
            Connection connection = sqliteDriver.connect(jdbcUrl, new Properties());
            if (connection == null) {
                throw new SQLException("SQLite driver rejected URL: " + jdbcUrl);
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open SQLite connection", e);
        }
    }
}