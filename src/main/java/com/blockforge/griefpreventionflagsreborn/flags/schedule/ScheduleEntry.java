package com.blockforge.griefpreventionflagsreborn.flags.schedule;

import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import org.jetbrains.annotations.NotNull;

/**
 * A lightweight data record representing a single schedule row returned by
 * {@link com.blockforge.griefpreventionflagsreborn.storage.ScheduleStorageManager}.
 * <p>
 * This record mirrors {@code ScheduleStorageManager.ScheduleEntry} so that the
 * schedule sub-package can work with a local type without depending on the
 * inner record directly.
 *
 * @param id             the auto-generated database row ID
 * @param flagId         the unique flag identifier this schedule applies to
 * @param scope          the scope at which the flag value will be applied
 * @param scopeId        the scope identifier (e.g. world name, claim ID, "server")
 * @param cronExpression the raw schedule expression stored in the database
 *                       (e.g. "SUNRISE", "NIGHT", "08:00", "20:00-06:00")
 * @param value          the flag value string to apply when the schedule is active
 * @param enabled        whether the schedule is currently enabled
 */
public record ScheduleEntry(
        int id,
        @NotNull String flagId,
        @NotNull FlagScope scope,
        @NotNull String scopeId,
        @NotNull String cronExpression,
        @NotNull String value,
        boolean enabled
) {}
