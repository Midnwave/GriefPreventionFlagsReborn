package com.blockforge.griefpreventionflagsreborn.schedule;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.flags.schedule.FlagSchedule;
import com.blockforge.griefpreventionflagsreborn.flags.schedule.FlagSchedule.ScheduleType;
import com.blockforge.griefpreventionflagsreborn.flags.schedule.ScheduleEntry;
import com.blockforge.griefpreventionflagsreborn.storage.ScheduleStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages time-based flag schedules for GriefPreventionFlagsReborn.
 * <p>
 * Schedules are loaded from the database via {@link ScheduleStorageManager} and
 * evaluated once per minute on the main Bukkit thread. When a schedule transitions
 * from inactive to active its configured flag value is applied; when it transitions
 * back to inactive the flag is unset so the normal scope-inheritance chain resumes.
 *
 * <h3>Tick approximations used for Minecraft world-time schedules</h3>
 * <pre>
 *   SUNRISE  ticks    0 – 1 000   (dawn)
 *   DAY      ticks 1 000 – 12 000 (daytime)
 *   SUNSET   ticks 12 000 – 14 000 (dusk)
 *   NIGHT    ticks 14 000 – 24 000 (nighttime)
 * </pre>
 */
public final class ScheduleManager {

    // ------------------------------------------------------------------
    //  Minecraft world-time tick boundaries
    // ------------------------------------------------------------------

    /** Start of the sunrise window (inclusive). */
    private static final long TICK_SUNRISE_START = 0L;
    /** End of the sunrise window (exclusive). */
    private static final long TICK_SUNRISE_END   = 1_000L;
    /** End of the daytime period (exclusive) / start of sunset window. */
    private static final long TICK_SUNSET_START  = 12_000L;
    /** End of the sunset window (exclusive) / start of night. */
    private static final long TICK_SUNSET_END    = 14_000L;
    /** Total ticks in a Minecraft day. */
    private static final long TICKS_PER_DAY      = 24_000L;

    /**
     * Scope ID used when applying a scheduled flag override. The flag is written at
     * the same scope and scopeId as defined in the schedule; this constant is used
     * only in log messages to identify system-originated changes.
     */
    private static final String SCHEDULED_SOURCE = "SCHEDULED";

    // ------------------------------------------------------------------
    //  Fields
    // ------------------------------------------------------------------

    private final GriefPreventionFlagsPlugin plugin;
    private final Logger logger;

    /**
     * The currently loaded, parsed schedules. Replaced atomically on {@link #reload()}.
     * Access is confined to the main Bukkit thread inside the tick task, so a plain
     * {@link ArrayList} is sufficient; the reference itself is {@code volatile} so
     * that {@link #reload()} (also called on the main thread) is safely published.
     */
    private volatile List<FlagSchedule> activeSchedules = Collections.emptyList();

    /**
     * Tracks the "is currently active" state for each schedule ID so the manager
     * can detect rising (inactive→active) and falling (active→inactive) edges and
     * apply or revert the flag exactly once per transition.
     * <p>
     * Keys are schedule IDs; values are {@code true} when the schedule was active
     * during the most-recent tick evaluation.
     */
    private final Map<Integer, Boolean> scheduleActiveState = new ConcurrentHashMap<>();

    /** The repeating Bukkit task; null when the manager is stopped. */
    @Nullable
    private BukkitTask tickTask;

    // ------------------------------------------------------------------
    //  Constructor
    // ------------------------------------------------------------------

    /**
     * Creates a new ScheduleManager.
     *
     * @param plugin the owning plugin instance (must not be null)
     */
    public ScheduleManager(@NotNull GriefPreventionFlagsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin must not be null");
        this.logger = plugin.getLogger();
    }

    // ------------------------------------------------------------------
    //  Lifecycle
    // ------------------------------------------------------------------

    /**
     * Loads all enabled schedules from the database and starts the 60-second
     * evaluation tick task.
     * <p>
     * Calling this method when the manager is already running has no effect beyond
     * a warning log entry; call {@link #reload()} to restart cleanly.
     */
    public void start() {
        if (tickTask != null) {
            logger.warning("ScheduleManager.start() called while already running — use reload() to restart.");
            return;
        }

        loadSchedules();
        startTickTask();
        logger.info("ScheduleManager started with " + activeSchedules.size() + " active schedule(s).");
    }

    /**
     * Cancels the tick task and reverts any schedule-applied flag values so the
     * plugin shuts down in a clean state.
     * <p>
     * Safe to call even if the manager was never started.
     */
    public void stop() {
        cancelTickTask();
        revertAllActive();
        activeSchedules = Collections.emptyList();
        scheduleActiveState.clear();
        logger.info("ScheduleManager stopped.");
    }

    /**
     * Stops the manager, re-reads all schedules from the database, and restarts
     * evaluation. Active overrides are reverted before the new schedule list is
     * applied so no stale flag values remain.
     */
    public void reload() {
        cancelTickTask();
        revertAllActive();
        scheduleActiveState.clear();

        loadSchedules();
        startTickTask();
        logger.info("ScheduleManager reloaded — " + activeSchedules.size() + " active schedule(s).");
    }

    // ------------------------------------------------------------------
    //  Public schedule mutation API
    // ------------------------------------------------------------------

    /**
     * Persists a new schedule to the database and reloads the active schedule list.
     *
     * @param flagId         the unique flag identifier
     * @param scope          the scope at which the flag value should be applied
     * @param scopeId        the scope identifier (world name, claim ID, etc.)
     * @param cronExpression the schedule expression (e.g. {@code "NIGHT"}, {@code "20:00-06:00"})
     * @param value          the flag value to apply when the schedule is active
     * @param createdBy      UUID of the player creating the schedule, or null for system-created
     * @return the auto-generated database row ID, or {@code -1} on failure
     */
    public int addSchedule(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId,
                           @NotNull String cronExpression, @NotNull String value, @Nullable UUID createdBy) {
        int id = plugin.getScheduleStorageManager()
                .addSchedule(flagId, scope, scopeId, cronExpression, value, createdBy);
        reload();
        return id;
    }

    /**
     * Removes a schedule from the database by its row ID and reloads the active
     * schedule list. If the schedule was currently active its flag override is
     * reverted during reload.
     *
     * @param id the database row ID of the schedule to remove
     */
    public void removeSchedule(int id) {
        plugin.getScheduleStorageManager().removeSchedule(id);
        reload();
    }

    // ------------------------------------------------------------------
    //  Activity evaluation
    // ------------------------------------------------------------------

    /**
     * Evaluates whether the given schedule should be active at the current moment.
     * <p>
     * For {@link ScheduleType#DAILY_TIME} schedules the server's wall-clock time
     * (via {@link LocalTime#now()}) is compared against the schedule's configured
     * time window. For world-time types (SUNRISE, SUNSET, DAY, NIGHT) the method
     * uses the time of the first non-null world returned by
     * {@link Bukkit#getWorlds()} as a representative value.
     *
     * @param schedule the schedule to test
     * @return {@code true} if the schedule should be active right now
     */
    public boolean isActive(@NotNull FlagSchedule schedule) {
        return switch (schedule.getType()) {
            case DAILY_TIME -> evaluateDailyTime(schedule);
            case SUNRISE    -> evaluateWorldTick(TICK_SUNRISE_START, TICK_SUNRISE_END);
            case SUNSET     -> evaluateWorldTick(TICK_SUNSET_START,  TICK_SUNSET_END);
            case DAY        -> evaluateWorldTick(TICK_SUNRISE_END,   TICK_SUNSET_START);
            case NIGHT      -> evaluateNight();
        };
    }

    // ------------------------------------------------------------------
    //  Private helpers — schedule loading
    // ------------------------------------------------------------------

    /**
     * Fetches all enabled schedule rows from the DB, converts each
     * {@link ScheduleStorageManager.ScheduleEntry} to a local {@link ScheduleEntry}
     * wrapper, then parses each one into a {@link FlagSchedule}.
     */
    private void loadSchedules() {
        List<ScheduleStorageManager.ScheduleEntry> dbEntries =
                plugin.getScheduleStorageManager().getActiveSchedules();

        List<FlagSchedule> parsed = new ArrayList<>(dbEntries.size());
        for (ScheduleStorageManager.ScheduleEntry dbEntry : dbEntries) {
            // Bridge the ScheduleStorageManager's inner record to the local ScheduleEntry type
            ScheduleEntry entry = new ScheduleEntry(
                    dbEntry.id(),
                    dbEntry.flagId(),
                    dbEntry.scope(),
                    dbEntry.scopeId(),
                    dbEntry.cronExpression(),
                    dbEntry.value(),
                    dbEntry.enabled()
            );

            FlagSchedule schedule = FlagSchedule.parse(entry, logger);
            if (schedule != null) {
                parsed.add(schedule);
            }
        }

        activeSchedules = Collections.unmodifiableList(parsed);
    }

    // ------------------------------------------------------------------
    //  Private helpers — tick task
    // ------------------------------------------------------------------

    /** Creates and registers the repeating BukkitRunnable (20 ticks = 1 second, 1200 = 1 minute). */
    private void startTickTask() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 1_200L); // delay=0, period=1200 ticks (60 s)
    }

    /** Cancels the repeating task if it is running. */
    private void cancelTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    // ------------------------------------------------------------------
    //  Private helpers — tick evaluation
    // ------------------------------------------------------------------

    /**
     * Called once per minute on the main thread. Iterates over all loaded schedules,
     * evaluates their current active state, and fires apply or revert operations
     * for any schedules whose state has changed since the previous tick.
     */
    private void tick() {
        List<FlagSchedule> schedules = activeSchedules; // read volatile reference once
        FlagManager flagManager = plugin.getFlagManager();

        for (FlagSchedule schedule : schedules) {
            boolean nowActive   = isActive(schedule);
            boolean wasActive   = scheduleActiveState.getOrDefault(schedule.getId(), false);

            if (nowActive == wasActive) {
                // No state change — nothing to do.
                continue;
            }

            scheduleActiveState.put(schedule.getId(), nowActive);

            if (nowActive) {
                // Rising edge: apply the schedule's flag value.
                applySchedule(flagManager, schedule);
            } else {
                // Falling edge: remove the scheduled override so normal resolution resumes.
                revertSchedule(flagManager, schedule);
            }
        }
    }

    /**
     * Applies the scheduled flag value at the schedule's scope and scopeId.
     *
     * @param flagManager the flag manager used to write the value
     * @param schedule    the schedule becoming active
     */
    private void applySchedule(@NotNull FlagManager flagManager, @NotNull FlagSchedule schedule) {
        try {
            flagManager.setFlag(schedule.getFlagId(), schedule.getScope(), schedule.getScopeId(),
                    schedule.getValue(), null);
            logger.log(Level.FINE,
                    "Schedule #{0} ({1}) activated — set {2} [{3}:{4}] = {5}",
                    new Object[]{
                            schedule.getId(), SCHEDULED_SOURCE,
                            schedule.getFlagId(), schedule.getScope(), schedule.getScopeId(),
                            schedule.getValue()
                    });
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "ScheduleManager: failed to apply schedule #" + schedule.getId()
                    + " for flag '" + schedule.getFlagId() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Removes the scheduled flag override so the normal scope-inheritance chain
     * takes effect again (claim/world/server defaults resume).
     *
     * @param flagManager the flag manager used to remove the value
     * @param schedule    the schedule becoming inactive
     */
    private void revertSchedule(@NotNull FlagManager flagManager, @NotNull FlagSchedule schedule) {
        try {
            flagManager.unsetFlag(schedule.getFlagId(), schedule.getScope(), schedule.getScopeId());
            logger.log(Level.FINE,
                    "Schedule #{0} ({1}) deactivated — unset {2} [{3}:{4}]",
                    new Object[]{
                            schedule.getId(), SCHEDULED_SOURCE,
                            schedule.getFlagId(), schedule.getScope(), schedule.getScopeId()
                    });
        } catch (Exception e) {
            logger.log(Level.WARNING,
                    "ScheduleManager: failed to revert schedule #" + schedule.getId()
                    + " for flag '" + schedule.getFlagId() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Reverts all schedules that are currently marked as active. Called on
     * {@link #stop()} and at the beginning of {@link #reload()} so that no
     * stale flag overrides remain after a shutdown or config change.
     */
    private void revertAllActive() {
        FlagManager flagManager = plugin.getFlagManager();
        if (flagManager == null) {
            return;
        }

        List<FlagSchedule> schedules = activeSchedules;
        for (FlagSchedule schedule : schedules) {
            if (Boolean.TRUE.equals(scheduleActiveState.get(schedule.getId()))) {
                revertSchedule(flagManager, schedule);
            }
        }
    }

    // ------------------------------------------------------------------
    //  Private helpers — activity evaluation
    // ------------------------------------------------------------------

    /**
     * Evaluates a {@link ScheduleType#DAILY_TIME} schedule against the current
     * wall-clock time.
     *
     * <ul>
     *   <li>Non-ranged: the schedule is active for the single minute whose
     *       hour and minute match {@code startHour:startMinute}.</li>
     *   <li>Ranged: the schedule is active while the current time falls within
     *       {@code [start, end)}. Midnight wrap-around is supported — if the end
     *       time is earlier than the start time (e.g. 22:00–06:00) the schedule
     *       is considered active when the current time is {@code >= start} OR
     *       {@code < end}.</li>
     * </ul>
     *
     * @param schedule the schedule to evaluate
     * @return {@code true} if the schedule is currently active
     */
    private boolean evaluateDailyTime(@NotNull FlagSchedule schedule) {
        LocalTime now = LocalTime.now();

        if (!schedule.isRanged()) {
            // Point-in-time: active for the entire matching minute
            return now.getHour() == schedule.getStartHour()
                    && now.getMinute() == schedule.getStartMinute();
        }

        // Ranged: compare as total-minutes-since-midnight for simplicity
        int nowMinutes   = now.getHour() * 60 + now.getMinute();
        int startMinutes = schedule.getStartHour() * 60 + schedule.getStartMinute();
        int endMinutes   = schedule.getEndHour()   * 60 + schedule.getEndMinute();

        if (startMinutes <= endMinutes) {
            // Normal range (no midnight wrap): e.g. 08:00–20:00
            return nowMinutes >= startMinutes && nowMinutes < endMinutes;
        } else {
            // Midnight-wrapping range: e.g. 22:00–06:00
            return nowMinutes >= startMinutes || nowMinutes < endMinutes;
        }
    }

    /**
     * Checks whether the first available Bukkit world's game-time falls within the
     * given tick range {@code [startTick, endTick)}.
     *
     * @param startTick inclusive lower bound (Minecraft ticks)
     * @param endTick   exclusive upper bound (Minecraft ticks)
     * @return {@code true} if the world time is inside the range, {@code false}
     *         if no worlds are loaded or the time is outside the range
     */
    private boolean evaluateWorldTick(long startTick, long endTick) {
        long worldTime = getRepresentativeWorldTime();
        if (worldTime < 0) {
            return false;
        }
        return worldTime >= startTick && worldTime < endTick;
    }

    /**
     * Night spans from the end of sunset through the end of the Minecraft day
     * and wraps back to before sunrise — i.e. ticks [14 000, 24 000) ∪ [0, 1 000).
     *
     * @return {@code true} if the current world time is within the night window
     */
    private boolean evaluateNight() {
        long worldTime = getRepresentativeWorldTime();
        if (worldTime < 0) {
            return false;
        }
        // Night = after sunset end OR before sunrise end
        return worldTime >= TICK_SUNSET_END || worldTime < TICK_SUNRISE_END;
    }

    /**
     * Returns the full-day-normalised tick value (0–23 999) of the first non-null
     * world returned by {@link Bukkit#getWorlds()}, or {@code -1} if no worlds
     * are available.
     *
     * @return a tick in the range [0, 24 000), or {@code -1}
     */
    private long getRepresentativeWorldTime() {
        for (World world : Bukkit.getWorlds()) {
            if (world != null) {
                // getFullTime() can exceed 24 000; normalise to a single day
                return world.getTime() % TICKS_PER_DAY;
            }
        }
        return -1L;
    }

    // ------------------------------------------------------------------
    //  Accessors (for testing / introspection)
    // ------------------------------------------------------------------

    /**
     * Returns an unmodifiable view of the currently loaded parsed schedules.
     *
     * @return the active schedule list
     */
    @NotNull
    public List<FlagSchedule> getActiveSchedules() {
        return activeSchedules;
    }

    /**
     * Returns a snapshot of the current active-state map (schedule ID to boolean).
     * Intended for diagnostics and testing.
     *
     * @return an unmodifiable copy of the active-state map
     */
    @NotNull
    public Map<Integer, Boolean> getScheduleActiveState() {
        return Collections.unmodifiableMap(new HashMap<>(scheduleActiveState));
    }
}
