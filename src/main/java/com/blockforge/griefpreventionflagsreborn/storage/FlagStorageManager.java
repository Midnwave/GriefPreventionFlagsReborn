package com.blockforge.griefpreventionflagsreborn.storage;

import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides CRUD operations for persisted flag values in the SQLite database.
 * <p>
 * All methods use {@link PreparedStatement} for SQL injection safety and
 * try-with-resources for proper resource cleanup.
 */
public final class FlagStorageManager {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    /**
     * Creates a new FlagStorageManager.
     *
     * @param databaseManager the database manager providing connections
     * @param logger          the logger for diagnostic output
     */
    public FlagStorageManager(@NotNull DatabaseManager databaseManager, @NotNull Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Retrieves the value of a flag for a specific scope and scope identifier.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the flag scope (SERVER, WORLD, CLAIM, SUBCLAIM)
     * @param scopeId the scope identifier (e.g. world name, claim ID)
     * @return the flag value as a string, or null if not set
     */
    @Nullable
    public String getValue(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        String sql = "SELECT value FROM gpfr_flag_values WHERE flag_id = ? AND scope = ? AND scope_id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flagId);
            ps.setString(2, scope.name());
            ps.setString(3, scopeId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get flag value: " + flagId + " [" + scope + ":" + scopeId + "]", e);
        }
        return null;
    }

    /**
     * Sets or updates a flag value for a specific scope and scope identifier.
     * Uses SQLite's INSERT OR REPLACE (via UNIQUE constraint on flag_id, scope, scope_id)
     * to handle both insert and update operations.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @param value   the value to set
     * @param setBy   the UUID of the player who set the value, or null if set by the system
     */
    public void setValue(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId,
                         @NotNull String value, @Nullable UUID setBy) {
        String sql = "INSERT INTO gpfr_flag_values (flag_id, scope, scope_id, value, set_by, set_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(flag_id, scope, scope_id) DO UPDATE SET value = excluded.value, " +
                     "set_by = excluded.set_by, set_at = excluded.set_at";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flagId);
            ps.setString(2, scope.name());
            ps.setString(3, scopeId);
            ps.setString(4, value);
            ps.setString(5, setBy != null ? setBy.toString() : null);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to set flag value: " + flagId + " [" + scope + ":" + scopeId + "] = " + value, e);
        }
    }

    /**
     * Removes a flag value for a specific scope and scope identifier.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     */
    public void removeValue(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        String sql = "DELETE FROM gpfr_flag_values WHERE flag_id = ? AND scope = ? AND scope_id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flagId);
            ps.setString(2, scope.name());
            ps.setString(3, scopeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to remove flag value: " + flagId + " [" + scope + ":" + scopeId + "]", e);
        }
    }

    /**
     * Retrieves all flag values for a specific scope and scope identifier.
     *
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @return a map of flag ID to value string, never null
     */
    @NotNull
    public Map<String, String> getAllForScope(@NotNull FlagScope scope, @NotNull String scopeId) {
        String sql = "SELECT flag_id, value FROM gpfr_flag_values WHERE scope = ? AND scope_id = ?";
        Map<String, String> result = new LinkedHashMap<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scope.name());
            ps.setString(2, scopeId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("flag_id"), rs.getString("value"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get all flags for scope: " + scope + ":" + scopeId, e);
        }
        return result;
    }

    /**
     * Retrieves all scope entries for a specific flag across all scopes.
     *
     * @param flagId the unique flag identifier
     * @return a list of {@link FlagScopeEntry} records, never null
     */
    @NotNull
    public List<FlagScopeEntry> getAllForFlag(@NotNull String flagId) {
        String sql = "SELECT scope, scope_id, value FROM gpfr_flag_values WHERE flag_id = ? ORDER BY scope";
        List<FlagScopeEntry> result = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flagId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FlagScope scope = FlagScope.valueOf(rs.getString("scope"));
                    String scopeId = rs.getString("scope_id");
                    String value = rs.getString("value");
                    result.add(new FlagScopeEntry(scope, scopeId, value));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get all entries for flag: " + flagId, e);
        }
        return result;
    }

    /**
     * Removes all flag values for a specific scope and scope identifier.
     * Useful when a claim is deleted or a scope is reset.
     *
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     */
    public void clearScope(@NotNull FlagScope scope, @NotNull String scopeId) {
        String sql = "DELETE FROM gpfr_flag_values WHERE scope = ? AND scope_id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scope.name());
            ps.setString(2, scopeId);
            int deleted = ps.executeUpdate();
            logger.info("Cleared " + deleted + " flag value(s) for scope " + scope + ":" + scopeId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to clear scope: " + scope + ":" + scopeId, e);
        }
    }

    /**
     * Represents a flag value record associated with a scope.
     *
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @param value   the flag value as a string
     */
    public record FlagScopeEntry(@NotNull FlagScope scope, @NotNull String scopeId, @NotNull String value) {
    }
}
