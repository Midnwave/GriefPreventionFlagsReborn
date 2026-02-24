package com.blockforge.griefpreventionflagsreborn.storage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the SQLite database connection and schema for GriefPreventionFlagsReborn.
 * <p>
 * Uses WAL journal mode for improved concurrent read performance and foreign keys
 * for referential integrity. Schema versioning is tracked via a dedicated
 * {@code schema_version} table, allowing incremental migrations.
 */
public final class DatabaseManager {

    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final File dataFolder;
    private final String databaseFileName;
    private final Logger logger;
    private Connection connection;

    /**
     * Creates a new DatabaseManager.
     *
     * @param dataFolder       the plugin's data folder (typically {@code JavaPlugin#getDataFolder()})
     * @param databaseFileName the SQLite database file name (e.g. "data.db")
     * @param logger           the logger used for diagnostic output
     */
    public DatabaseManager(File dataFolder, String databaseFileName, Logger logger) {
        this.dataFolder = dataFolder;
        this.databaseFileName = databaseFileName;
        this.logger = logger;
    }

    /**
     * Opens the database connection, enables WAL mode and foreign keys,
     * creates the schema version table, and applies any pending migrations.
     *
     * @throws SQLException if the connection cannot be established or schema setup fails
     */
    public void open() throws SQLException {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new SQLException("Failed to create data folder: " + dataFolder.getAbsolutePath());
        }

        File dbFile = new File(dataFolder, databaseFileName);
        if (!dbFile.exists()) {
            try {
                if (!dbFile.createNewFile()) {
                    throw new SQLException("Failed to create database file: " + dbFile.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new SQLException("Failed to create database file: " + dbFile.getAbsolutePath(), e);
            }
        }

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);

        // Enable WAL journal mode for better concurrent read performance
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        }

        // Enable foreign key enforcement
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=ON");
        }

        // Create the schema version tracking table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS schema_version (" +
                "    version INTEGER NOT NULL," +
                "    applied_at INTEGER NOT NULL" +
                ")"
            );
        }

        // Apply migrations
        applyMigrations();

        logger.info("Database connection established: " + dbFile.getAbsolutePath());
    }

    /**
     * Returns the active database connection.
     *
     * @return the {@link Connection} instance
     * @throws SQLException if the connection is null or closed
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not open. Call open() first.");
        }
        return connection;
    }

    /**
     * Closes the database connection gracefully.
     */
    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.info("Database connection closed.");
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing database connection", e);
            }
            connection = null;
        }
    }

    /**
     * Returns the current schema version recorded in the database.
     *
     * @return the schema version number, or 0 if no version has been recorded
     * @throws SQLException if a database error occurs
     */
    private int getCurrentSchemaVersion() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (rs.next()) {
                int version = rs.getInt(1);
                if (!rs.wasNull()) {
                    return version;
                }
            }
        }
        return 0;
    }

    /**
     * Records a schema version in the schema_version table.
     *
     * @param version the version number to record
     * @throws SQLException if a database error occurs
     */
    private void recordSchemaVersion(int version) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO schema_version (version, applied_at) VALUES (?, ?)")) {
            ps.setInt(1, version);
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    /**
     * Applies all pending schema migrations sequentially from the current version
     * up to {@link #CURRENT_SCHEMA_VERSION}.
     *
     * @throws SQLException if any migration fails
     */
    private void applyMigrations() throws SQLException {
        int currentVersion = getCurrentSchemaVersion();

        if (currentVersion < CURRENT_SCHEMA_VERSION) {
            logger.info("Database schema version " + currentVersion + " -> " + CURRENT_SCHEMA_VERSION + ". Applying migrations...");
        }

        if (currentVersion < 1) {
            applyMigrationV1();
            recordSchemaVersion(1);
            logger.info("Applied database migration v1.");
        }

        // Future migrations go here:
        // if (currentVersion < 2) { applyMigrationV2(); recordSchemaVersion(2); }
    }

    /**
     * Migration v1: Creates the core flag_values and schedules tables with indices.
     *
     * @throws SQLException if a database error occurs
     */
    private void applyMigrationV1() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Flag values table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS gpfr_flag_values (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    flag_id TEXT NOT NULL," +
                "    scope TEXT NOT NULL," +
                "    scope_id TEXT NOT NULL," +
                "    value TEXT NOT NULL," +
                "    set_by TEXT," +
                "    set_at INTEGER NOT NULL," +
                "    UNIQUE(flag_id, scope, scope_id)" +
                ")"
            );

            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_flag_scope ON gpfr_flag_values(scope, scope_id)"
            );

            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_flag_id ON gpfr_flag_values(flag_id)"
            );

            // Schedules table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS gpfr_schedules (" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    flag_id TEXT NOT NULL," +
                "    scope TEXT NOT NULL," +
                "    scope_id TEXT NOT NULL," +
                "    cron_expression TEXT NOT NULL," +
                "    value TEXT NOT NULL," +
                "    enabled INTEGER NOT NULL DEFAULT 1," +
                "    created_by TEXT," +
                "    created_at INTEGER NOT NULL" +
                ")"
            );

            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_schedule_flag ON gpfr_schedules(flag_id, scope, scope_id)"
            );
        }
    }

    /**
     * Applies a custom SQL migration string. Useful for plugins extending the schema.
     *
     * @param migrationName a descriptive name for logging
     * @param sql           the SQL statement(s) to execute
     * @throws SQLException if the migration fails
     */
    public void applyMigration(String migrationName, String sql) throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
            logger.info("Applied custom migration: " + migrationName);
        }
    }

    /**
     * Applies a custom migration with a specific version number. If the version
     * has already been applied, this method does nothing.
     *
     * @param version       the version number for the migration
     * @param migrationName a descriptive name for logging
     * @param sql           the SQL statement(s) to execute
     * @throws SQLException if the migration fails
     */
    public void applyMigration(int version, String migrationName, String sql) throws SQLException {
        if (getCurrentSchemaVersion() >= version) {
            return;
        }
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
            recordSchemaVersion(version);
            logger.info("Applied migration v" + version + ": " + migrationName);
        }
    }
}
