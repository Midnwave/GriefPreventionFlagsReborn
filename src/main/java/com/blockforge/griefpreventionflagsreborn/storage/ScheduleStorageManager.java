package com.blockforge.griefpreventionflagsreborn.storage;

import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides CRUD operations for scheduled flag changes in the SQLite database.
 * <p>
 * Schedules allow flag values to be applied or changed on a cron-based schedule,
 * enabling time-based flag behavior (e.g., PvP enabled only at night).
 */
public final class ScheduleStorageManager {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    /**
     * Creates a new ScheduleStorageManager.
     *
     * @param databaseManager the database manager providing connections
     * @param logger          the logger for diagnostic output
     */
    public ScheduleStorageManager(@NotNull DatabaseManager databaseManager, @NotNull Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * Adds a new schedule entry for a flag.
     *
     * @param flagId    the unique flag identifier
     * @param scope     the flag scope
     * @param scopeId   the scope identifier
     * @param cronExpr  the cron expression defining when the schedule triggers
     * @param value     the value to set when the schedule triggers
     * @param createdBy the UUID of the player who created the schedule, or null if system-created
     * @return the auto-generated row ID of the new schedule, or -1 on failure
     */
    public int addSchedule(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId,
                           @NotNull String cronExpr, @NotNull String value, @Nullable UUID createdBy) {
        String sql = "INSERT INTO gpfr_schedules (flag_id, scope, scope_id, cron_expression, value, enabled, created_by, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, 1, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, flagId);
            ps.setString(2, scope.name());
            ps.setString(3, scopeId);
            ps.setString(4, cronExpr);
            ps.setString(5, value);
            ps.setString(6, createdBy != null ? createdBy.toString() : null);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    logger.info("Created schedule #" + id + " for flag " + flagId + " [" + scope + ":" + scopeId + "]");
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to add schedule for flag: " + flagId, e);
        }
        return -1;
    }

    /**
     * Removes a schedule entry by its ID.
     *
     * @param id the schedule row ID
     */
    public void removeSchedule(int id) {
        String sql = "DELETE FROM gpfr_schedules WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logger.info("Removed schedule #" + id);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to remove schedule #" + id, e);
        }
    }

    /**
     * Removes all schedule entries for a specific flag within a scope.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     */
    public void removeSchedulesForFlag(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        String sql = "DELETE FROM gpfr_schedules WHERE flag_id = ? AND scope = ? AND scope_id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, flagId);
            ps.setString(2, scope.name());
            ps.setString(3, scopeId);
            int deleted = ps.executeUpdate();
            logger.info("Removed " + deleted + " schedule(s) for flag " + flagId + " [" + scope + ":" + scopeId + "]");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to remove schedules for flag: " + flagId + " [" + scope + ":" + scopeId + "]", e);
        }
    }

    /**
     * Retrieves all active (enabled) schedule entries from the database.
     *
     * @return a list of active {@link ScheduleEntry} records, never null
     */
    @NotNull
    public List<ScheduleEntry> getActiveSchedules() {
        String sql = "SELECT id, flag_id, scope, scope_id, cron_expression, value, enabled " +
                     "FROM gpfr_schedules WHERE enabled = 1";
        List<ScheduleEntry> result = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new ScheduleEntry(
                    rs.getInt("id"),
                    rs.getString("flag_id"),
                    FlagScope.valueOf(rs.getString("scope")),
                    rs.getString("scope_id"),
                    rs.getString("cron_expression"),
                    rs.getString("value"),
                    rs.getInt("enabled") == 1
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get active schedules", e);
        }
        return result;
    }

    /**
     * Represents a schedule entry from the database.
     *
     * @param id             the auto-generated row ID
     * @param flagId         the unique flag identifier
     * @param scope          the flag scope
     * @param scopeId        the scope identifier
     * @param cronExpression the cron expression defining the trigger schedule
     * @param value          the value to apply when triggered
     * @param enabled        whether the schedule is currently active
     */
    public record ScheduleEntry(
        int id,
        @NotNull String flagId,
        @NotNull FlagScope scope,
        @NotNull String scopeId,
        @NotNull String cronExpression,
        @NotNull String value,
        boolean enabled
    ) {
    }
}
