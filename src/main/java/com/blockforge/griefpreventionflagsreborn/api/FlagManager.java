package com.blockforge.griefpreventionflagsreborn.api;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Primary interface for reading and writing flag values across different scopes.
 * <p>
 * Flag resolution follows priority order: SUBCLAIM > CLAIM > WORLD > SERVER.
 * The highest-priority scope that has a value set will be used.
 */
public interface FlagManager {

    /**
     * Resolves the effective flag value at the given location by checking all scopes
     * from highest to lowest priority.
     *
     * @param flagId   the unique flag identifier
     * @param location the location to resolve for (used to determine world, claim, subclaim)
     * @param <T>      the expected value type
     * @return the resolved value, or the flag's default if no value is set at any scope
     */
    <T> T getFlagValue(@NotNull String flagId, @NotNull Location location);

    /**
     * Resolves the effective flag value for a specific claim by checking the claim scope,
     * then falling back through world and server scopes.
     *
     * @param flagId  the unique flag identifier
     * @param claimId the claim identifier
     * @param <T>     the expected value type
     * @return the resolved value, or the flag's default if no value is set at any scope
     */
    <T> T getFlagValue(@NotNull String flagId, @NotNull String claimId);

    /**
     * Convenience method that resolves a boolean flag at the given location.
     * Equivalent to calling {@code getFlagValue(flagId, location)} for boolean flags.
     *
     * @param flagId   the unique flag identifier
     * @param location the location to check
     * @return true if the flag is enabled at the location
     */
    boolean isFlagEnabled(@NotNull String flagId, @NotNull Location location);

    /**
     * Sets a flag value at the specified scope.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the scope to set the value at
     * @param scopeId the scope identifier (e.g. "server", world name, claim ID)
     * @param value   the string representation of the value to set (will be parsed)
     * @param setBy   the UUID of the player setting the value, or null for system changes
     * @throws com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException
     *         if the value cannot be parsed or is invalid for the flag
     */
    void setFlag(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId,
                 @NotNull String value, @Nullable UUID setBy);

    /**
     * Removes a flag value at the specified scope, allowing it to fall through to
     * a lower-priority scope or the default value.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the scope to unset at
     * @param scopeId the scope identifier
     */
    void unsetFlag(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId);

    /**
     * Returns the raw value set at a specific scope, without fallback resolution.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the scope to check
     * @param scopeId the scope identifier
     * @param <T>     the expected value type
     * @return the value set at the exact scope, or null if no value is set there
     */
    @Nullable
    <T> T getRawFlagValue(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId);

    /**
     * Returns all flag values set at the specified scope and scope ID.
     *
     * @param scope   the scope to query
     * @param scopeId the scope identifier
     * @return an unmodifiable map of flag IDs to their values at that scope
     */
    @NotNull
    Map<String, Object> getAllFlagsForScope(@NotNull FlagScope scope, @NotNull String scopeId);

    /**
     * Clears the entire flag value cache. Use sparingly; prefer
     * {@link #invalidateCache(String)} for targeted invalidation.
     */
    void clearCache();

    /**
     * Invalidates cached values for a specific scope ID (e.g. a claim that was modified).
     *
     * @param scopeId the scope identifier to invalidate
     */
    void invalidateCache(@NotNull String scopeId);
}
