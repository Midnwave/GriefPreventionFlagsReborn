package com.blockforge.griefpreventionflagsreborn.flags.schedule;

import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * A parsed representation of a single scheduled flag change.
 * <p>
 * Instances are created via {@link #parse(ScheduleEntry)} which converts the raw
 * {@code cronExpression} string stored in the database into typed fields that
 * {@code ScheduleManager} can evaluate each minute without string-parsing overhead.
 *
 * <h3>Supported cron expression formats</h3>
 * <table>
 *   <tr><th>Expression</th><th>Resulting type</th><th>Behaviour</th></tr>
 *   <tr><td>{@code SUNRISE}</td><td>{@link ScheduleType#SUNRISE}</td><td>Active for a brief window at sunrise (~tick 0)</td></tr>
 *   <tr><td>{@code SUNSET}</td><td>{@link ScheduleType#SUNSET}</td><td>Active for a brief window at sunset (~tick 12 000)</td></tr>
 *   <tr><td>{@code DAY}</td><td>{@link ScheduleType#DAY}</td><td>Active from sunrise through sunset (ticks 0–12 000)</td></tr>
 *   <tr><td>{@code NIGHT}</td><td>{@link ScheduleType#NIGHT}</td><td>Active from sunset through sunrise (ticks 12 000–24 000)</td></tr>
 *   <tr><td>{@code HH:MM}</td><td>{@link ScheduleType#DAILY_TIME}</td><td>Active for exactly one minute at that real-world time</td></tr>
 *   <tr><td>{@code HH:MM-HH:MM}</td><td>{@link ScheduleType#DAILY_TIME}</td><td>Active between the two real-world times (supports midnight wrap)</td></tr>
 * </table>
 */
public final class FlagSchedule {

    // ------------------------------------------------------------------
    //  ScheduleType enum
    // ------------------------------------------------------------------

    /**
     * The type of trigger used by a {@link FlagSchedule}.
     */
    public enum ScheduleType {
        /** Activates at a specific real-world clock time (optionally ranged). */
        DAILY_TIME,
        /** Active during the brief Minecraft sunrise window (ticks ~0–1 000). */
        SUNRISE,
        /** Active during the brief Minecraft sunset window (ticks ~12 000–14 000). */
        SUNSET,
        /** Active during the Minecraft night period (ticks ~14 000–24 000 / 0). */
        NIGHT,
        /** Active during the Minecraft day period (ticks ~1 000–12 000). */
        DAY
    }

    // ------------------------------------------------------------------
    //  Fields
    // ------------------------------------------------------------------

    /** Database row ID. */
    private final int id;
    /** The unique identifier of the flag this schedule targets. */
    private final String flagId;
    /** The scope at which the flag value will be applied. */
    private final FlagScope scope;
    /** The scope identifier (world name, claim ID, "server", etc.). */
    private final String scopeId;
    /** The raw expression string as stored in the database. */
    private final String cronExpression;
    /** The flag value string to apply when the schedule becomes active. */
    private final String value;
    /** Whether this schedule is enabled in the database. */
    private final boolean enabled;
    /** The parsed trigger type. */
    private final ScheduleType type;
    /**
     * For {@link ScheduleType#DAILY_TIME}: the hour component of the start time (0–23).
     * Unused for other types.
     */
    private final int startHour;
    /**
     * For {@link ScheduleType#DAILY_TIME}: the minute component of the start time (0–59).
     * Unused for other types.
     */
    private final int startMinute;
    /**
     * For ranged {@link ScheduleType#DAILY_TIME}: the hour component of the end time (0–23).
     * {@code -1} when the schedule is a point-in-time (non-ranged) trigger.
     */
    private final int endHour;
    /**
     * For ranged {@link ScheduleType#DAILY_TIME}: the minute component of the end time (0–59).
     * {@code -1} when the schedule is a point-in-time (non-ranged) trigger.
     */
    private final int endMinute;

    // ------------------------------------------------------------------
    //  Constructor (private – use parse())
    // ------------------------------------------------------------------

    private FlagSchedule(int id, @NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId,
                         @NotNull String cronExpression, @NotNull String value, boolean enabled,
                         @NotNull ScheduleType type,
                         int startHour, int startMinute, int endHour, int endMinute) {
        this.id = id;
        this.flagId = flagId;
        this.scope = scope;
        this.scopeId = scopeId;
        this.cronExpression = cronExpression;
        this.value = value;
        this.enabled = enabled;
        this.type = type;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
    }

    // ------------------------------------------------------------------
    //  Static factory
    // ------------------------------------------------------------------

    /**
     * Parses a {@link ScheduleEntry} (from the database) into a {@link FlagSchedule}.
     *
     * <p>If the {@code cronExpression} cannot be parsed, this method logs a warning
     * and returns {@code null} so the caller can skip the malformed entry gracefully.
     *
     * @param entry  the raw schedule entry from the database
     * @return a fully parsed {@code FlagSchedule}, or {@code null} if parsing fails
     */
    @Nullable
    public static FlagSchedule parse(@NotNull ScheduleEntry entry) {
        return parse(entry, null);
    }

    /**
     * Parses a {@link ScheduleEntry} into a {@link FlagSchedule}, writing warnings
     * to the supplied logger when the expression is malformed.
     *
     * @param entry  the raw schedule entry from the database
     * @param logger an optional logger for diagnostic output (may be {@code null})
     * @return a fully parsed {@code FlagSchedule}, or {@code null} if parsing fails
     */
    @Nullable
    public static FlagSchedule parse(@NotNull ScheduleEntry entry, @Nullable Logger logger) {
        String expr = entry.cronExpression().trim().toUpperCase();

        // --- Named symbolic types ---
        switch (expr) {
            case "SUNRISE" -> {
                return new FlagSchedule(entry.id(), entry.flagId(), entry.scope(), entry.scopeId(),
                        entry.cronExpression(), entry.value(), entry.enabled(),
                        ScheduleType.SUNRISE, -1, -1, -1, -1);
            }
            case "SUNSET" -> {
                return new FlagSchedule(entry.id(), entry.flagId(), entry.scope(), entry.scopeId(),
                        entry.cronExpression(), entry.value(), entry.enabled(),
                        ScheduleType.SUNSET, -1, -1, -1, -1);
            }
            case "NIGHT" -> {
                return new FlagSchedule(entry.id(), entry.flagId(), entry.scope(), entry.scopeId(),
                        entry.cronExpression(), entry.value(), entry.enabled(),
                        ScheduleType.NIGHT, -1, -1, -1, -1);
            }
            case "DAY" -> {
                return new FlagSchedule(entry.id(), entry.flagId(), entry.scope(), entry.scopeId(),
                        entry.cronExpression(), entry.value(), entry.enabled(),
                        ScheduleType.DAY, -1, -1, -1, -1);
            }
        }

        // --- Ranged time: "HH:MM-HH:MM" ---
        if (expr.contains("-")) {
            String[] parts = expr.split("-", 2);
            int[] start = parseTime(parts[0].trim());
            int[] end   = parseTime(parts[1].trim());
            if (start == null || end == null) {
                warn(logger, entry, expr);
                return null;
            }
            return new FlagSchedule(entry.id(), entry.flagId(), entry.scope(), entry.scopeId(),
                    entry.cronExpression(), entry.value(), entry.enabled(),
                    ScheduleType.DAILY_TIME,
                    start[0], start[1], end[0], end[1]);
        }

        // --- Point-in-time: "HH:MM" ---
        int[] time = parseTime(expr);
        if (time != null) {
            return new FlagSchedule(entry.id(), entry.flagId(), entry.scope(), entry.scopeId(),
                    entry.cronExpression(), entry.value(), entry.enabled(),
                    ScheduleType.DAILY_TIME,
                    time[0], time[1], -1, -1);
        }

        warn(logger, entry, expr);
        return null;
    }

    // ------------------------------------------------------------------
    //  Private helpers
    // ------------------------------------------------------------------

    /**
     * Parses a {@code "HH:MM"} string into a two-element int array {@code [hour, minute]}.
     *
     * @param token the string to parse (already upper-cased and trimmed)
     * @return {@code int[]{hour, minute}} or {@code null} if the format is invalid
     */
    @Nullable
    private static int[] parseTime(@NotNull String token) {
        if (!token.matches("\\d{1,2}:\\d{2}")) {
            return null;
        }
        String[] parts = token.split(":", 2);
        try {
            int hour   = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return new int[]{hour, minute};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Logs a warning about an unparseable cron expression.
     */
    private static void warn(@Nullable Logger logger, @NotNull ScheduleEntry entry, @NotNull String expr) {
        if (logger != null) {
            logger.warning("ScheduleManager: skipping schedule #" + entry.id()
                    + " for flag '" + entry.flagId()
                    + "' — unrecognised cron expression: \"" + expr + "\"");
        }
    }

    // ------------------------------------------------------------------
    //  Accessors
    // ------------------------------------------------------------------

    /** Returns the database row ID of this schedule. */
    public int getId() { return id; }

    /** Returns the unique identifier of the flag this schedule targets. */
    @NotNull
    public String getFlagId() { return flagId; }

    /** Returns the scope at which the flag value will be applied or removed. */
    @NotNull
    public FlagScope getScope() { return scope; }

    /** Returns the scope identifier (e.g. world name, claim ID, {@code "server"}). */
    @NotNull
    public String getScopeId() { return scopeId; }

    /** Returns the raw cron expression string as stored in the database. */
    @NotNull
    public String getCronExpression() { return cronExpression; }

    /** Returns the flag value string to apply when the schedule is active. */
    @NotNull
    public String getValue() { return value; }

    /** Returns {@code true} if this schedule is enabled in the database. */
    public boolean isEnabled() { return enabled; }

    /** Returns the parsed schedule trigger type. */
    @NotNull
    public ScheduleType getType() { return type; }

    /**
     * Returns the hour component of the start time for {@link ScheduleType#DAILY_TIME} schedules,
     * or {@code -1} for symbolic types (SUNRISE, SUNSET, DAY, NIGHT).
     */
    public int getStartHour() { return startHour; }

    /**
     * Returns the minute component of the start time for {@link ScheduleType#DAILY_TIME} schedules,
     * or {@code -1} for symbolic types.
     */
    public int getStartMinute() { return startMinute; }

    /**
     * Returns the hour component of the end time for ranged {@link ScheduleType#DAILY_TIME} schedules,
     * or {@code -1} if this is a non-ranged (point-in-time) trigger or a symbolic type.
     */
    public int getEndHour() { return endHour; }

    /**
     * Returns the minute component of the end time for ranged {@link ScheduleType#DAILY_TIME} schedules,
     * or {@code -1} if this is a non-ranged trigger or a symbolic type.
     */
    public int getEndMinute() { return endMinute; }

    /**
     * Returns {@code true} if this is a ranged {@link ScheduleType#DAILY_TIME} schedule
     * (i.e. both {@link #getEndHour()} and {@link #getEndMinute()} are non-negative).
     */
    public boolean isRanged() { return endHour >= 0 && endMinute >= 0; }

    @Override
    public String toString() {
        return "FlagSchedule{id=" + id
                + ", flagId='" + flagId + '\''
                + ", scope=" + scope
                + ", scopeId='" + scopeId + '\''
                + ", type=" + type
                + ", expr='" + cronExpression + '\''
                + ", enabled=" + enabled + '}';
    }
}
