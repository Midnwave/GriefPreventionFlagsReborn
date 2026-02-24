package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.events.FlagChangeEvent;
import com.blockforge.griefpreventionflagsreborn.api.events.FlagCheckEvent;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import com.blockforge.griefpreventionflagsreborn.storage.FlagStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core implementation of {@link FlagManager} that resolves flag values using the
 * 4-level inheritance chain: SUBCLAIM > CLAIM > WORLD > SERVER > default.
 * <p>
 * Flag values are cached in a thread-safe {@link ConcurrentHashMap} with a configurable
 * time-to-live (TTL). Storage operations are delegated to {@link FlagStorageManager},
 * and claim/subclaim resolution is handled via {@link GriefPreventionHook}.
 * <p>
 * All mutating operations fire Bukkit events ({@link FlagChangeEvent}) that can be
 * cancelled by other plugins. Read operations fire {@link FlagCheckEvent} to allow
 * dynamic value overrides.
 */
public final class FlagManagerImpl implements FlagManager {

    private static final String SERVER_SCOPE_ID = "server";
    private static final long DEFAULT_CACHE_TTL_MS = 30_000L;

    private final FlagRegistryImpl registry;
    private final FlagStorageManager storageManager;
    private final GriefPreventionHook gpHook;
    private final Logger logger;

    private final ConcurrentHashMap<String, CachedEntry> cache = new ConcurrentHashMap<>();
    private long cacheTtlMs;

    /**
     * Creates a new FlagManagerImpl.
     *
     * @param registry       the flag registry for looking up flag definitions
     * @param storageManager the storage manager for persisted flag values
     * @param gpHook         the GriefPrevention hook for claim/subclaim resolution
     * @param logger         the logger for diagnostic output
     */
    public FlagManagerImpl(@NotNull FlagRegistryImpl registry,
                           @NotNull FlagStorageManager storageManager,
                           @NotNull GriefPreventionHook gpHook,
                           @NotNull Logger logger) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager must not be null");
        this.gpHook = Objects.requireNonNull(gpHook, "gpHook must not be null");
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
        this.cacheTtlMs = DEFAULT_CACHE_TTL_MS;
    }

    /**
     * Sets the cache time-to-live in milliseconds. Entries older than this value
     * will be treated as expired and re-fetched from storage.
     *
     * @param ttlMs the TTL in milliseconds (must be positive)
     */
    public void setCacheTtlMs(long ttlMs) {
        if (ttlMs <= 0) {
            throw new IllegalArgumentException("Cache TTL must be positive, got: " + ttlMs);
        }
        this.cacheTtlMs = ttlMs;
    }

    /**
     * Returns the current cache TTL in milliseconds.
     *
     * @return the cache TTL
     */
    public long getCacheTtlMs() {
        return cacheTtlMs;
    }

    // ---------------------------------------------------------------
    //  Flag value resolution (location-based)
    // ---------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * Resolves the flag value using the 4-level inheritance chain:
     * <ol>
     *   <li>SUBCLAIM (if location is in a subclaim)</li>
     *   <li>CLAIM (if location is in a claim)</li>
     *   <li>WORLD</li>
     *   <li>SERVER</li>
     *   <li>Flag default value</li>
     * </ol>
     * A {@link FlagCheckEvent} is fired before resolution to allow external overrides.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getFlagValue(@NotNull String flagId, @NotNull Location location) {
        Objects.requireNonNull(flagId, "flagId must not be null");
        Objects.requireNonNull(location, "location must not be null");

        AbstractFlag<?> flag = registry.getFlag(flagId);
        if (flag == null) {
            logger.log(Level.FINE, "getFlagValue: flag not found in registry: {0}", flagId);
            return null;
        }

        // Fire FlagCheckEvent to allow external overrides before doing any resolution
        FlagCheckEvent checkEvent = new FlagCheckEvent(flagId, location, null);
        Bukkit.getPluginManager().callEvent(checkEvent);
        if (checkEvent.hasOverride()) {
            logger.log(Level.FINE, "getFlagValue: override applied for flag {0} at {1}", new Object[]{flagId, location});
            return (T) checkEvent.getOverrideValue();
        }

        // 1. SUBCLAIM scope
        if (gpHook.isInSubclaim(location)) {
            String subclaimId = gpHook.getSubclaimId(location);
            if (subclaimId != null) {
                Object value = getCachedOrResolve(flagId, FlagScope.SUBCLAIM, subclaimId);
                if (value != null) {
                    logger.log(Level.FINE, "getFlagValue: resolved {0} from SUBCLAIM {1}", new Object[]{flagId, subclaimId});
                    return (T) value;
                }
            }
        }

        // 2. CLAIM scope
        if (gpHook.isInClaim(location)) {
            String claimId = gpHook.getClaimId(location);
            if (claimId != null) {
                Object value = getCachedOrResolve(flagId, FlagScope.CLAIM, claimId);
                if (value != null) {
                    logger.log(Level.FINE, "getFlagValue: resolved {0} from CLAIM {1}", new Object[]{flagId, claimId});
                    return (T) value;
                }
            }
        }

        // 3. WORLD scope
        World world = location.getWorld();
        if (world != null) {
            String worldName = world.getName();
            Object value = getCachedOrResolve(flagId, FlagScope.WORLD, worldName);
            if (value != null) {
                logger.log(Level.FINE, "getFlagValue: resolved {0} from WORLD {1}", new Object[]{flagId, worldName});
                return (T) value;
            }
        }

        // 4. SERVER scope
        Object serverValue = getCachedOrResolve(flagId, FlagScope.SERVER, SERVER_SCOPE_ID);
        if (serverValue != null) {
            logger.log(Level.FINE, "getFlagValue: resolved {0} from SERVER", flagId);
            return (T) serverValue;
        }

        // 5. Flag default value
        logger.log(Level.FINE, "getFlagValue: returning default for flag {0}", flagId);
        return (T) flag.getDefinition().getDefaultValue();
    }

    // ---------------------------------------------------------------
    //  Flag value resolution (claim-based)
    // ---------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * Resolves the flag value starting from the CLAIM scope, then falling through
     * WORLD and SERVER scopes. Subclaim scope is skipped because only a claim ID
     * is provided.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getFlagValue(@NotNull String flagId, @NotNull String claimId) {
        Objects.requireNonNull(flagId, "flagId must not be null");
        Objects.requireNonNull(claimId, "claimId must not be null");

        AbstractFlag<?> flag = registry.getFlag(flagId);
        if (flag == null) {
            logger.log(Level.FINE, "getFlagValue(claim): flag not found in registry: {0}", flagId);
            return null;
        }

        // 1. CLAIM scope
        Object claimValue = getCachedOrResolve(flagId, FlagScope.CLAIM, claimId);
        if (claimValue != null) {
            logger.log(Level.FINE, "getFlagValue(claim): resolved {0} from CLAIM {1}", new Object[]{flagId, claimId});
            return (T) claimValue;
        }

        // 2. WORLD scope - skipped because we cannot determine the world from a claim ID alone
        //    (GriefPrevention API does not provide a direct claim-ID-to-world lookup without a Location)

        // 3. SERVER scope
        Object serverValue = getCachedOrResolve(flagId, FlagScope.SERVER, SERVER_SCOPE_ID);
        if (serverValue != null) {
            logger.log(Level.FINE, "getFlagValue(claim): resolved {0} from SERVER", flagId);
            return (T) serverValue;
        }

        // 4. Flag default value
        logger.log(Level.FINE, "getFlagValue(claim): returning default for flag {0}", flagId);
        return (T) flag.getDefinition().getDefaultValue();
    }

    // ---------------------------------------------------------------
    //  Boolean convenience method
    // ---------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * If the resolved value is a {@link Boolean}, returns it directly.
     * If the resolved value is non-null (any type), returns {@code true}.
     * If the resolved value is {@code null}, returns {@code false}.
     */
    @Override
    public boolean isFlagEnabled(@NotNull String flagId, @NotNull Location location) {
        Object value = getFlagValue(flagId, location);

        if (value instanceof Boolean boolValue) {
            return boolValue;
        }

        return value != null;
    }

    // ---------------------------------------------------------------
    //  Set / Unset operations
    // ---------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @throws InvalidFlagValueException if the flag is not registered, the value cannot
     *                                   be parsed, or the parsed value fails validation
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setFlag(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId,
                        @NotNull String value, @Nullable UUID setBy) {
        Objects.requireNonNull(flagId, "flagId must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(value, "value must not be null");

        // 1. Get the flag from the registry
        AbstractFlag flag = registry.getFlag(flagId);
        if (flag == null) {
            throw new InvalidFlagValueException(flagId, "Flag is not registered.");
        }

        // 2. Parse the value using the flag's parser
        Object parsedValue;
        try {
            parsedValue = flag.parseValue(value);
        } catch (Exception e) {
            throw new InvalidFlagValueException(flagId, "Failed to parse value '" + value + "': " + e.getMessage());
        }

        // 3. Validate the parsed value
        if (!flag.isValid(parsedValue)) {
            throw new InvalidFlagValueException(flagId, "Value '" + value + "' is not valid for this flag.");
        }

        // 4. Get the old value from storage for the event
        Object oldValue = resolveFromStorage(flagId, scope, scopeId);

        // 5. Fire FlagChangeEvent (cancellable)
        FlagChangeEvent changeEvent = new FlagChangeEvent(flagId, scope, scopeId, oldValue, parsedValue, setBy);
        Bukkit.getPluginManager().callEvent(changeEvent);

        if (changeEvent.isCancelled()) {
            logger.log(Level.FINE, "setFlag: FlagChangeEvent cancelled for {0} [{1}:{2}]",
                    new Object[]{flagId, scope, scopeId});
            return;
        }

        // 6. Store via storageManager using serialized value
        String serialized = flag.serializeValue(parsedValue);
        storageManager.setValue(flagId, scope, scopeId, serialized, setBy);

        // 7. Invalidate cache for this scopeId
        invalidateCache(scopeId);

        // 8. Call onEnable callback
        try {
            flag.onEnable(scope, scopeId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception in onEnable callback for flag " + flagId, e);
        }

        logger.log(Level.FINE, "setFlag: set {0} [{1}:{2}] = {3} (by {4})",
                new Object[]{flagId, scope, scopeId, value, setBy});
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void unsetFlag(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        Objects.requireNonNull(flagId, "flagId must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");

        AbstractFlag flag = registry.getFlag(flagId);

        // 1. Get the old value from storage for the event
        Object oldValue = resolveFromStorage(flagId, scope, scopeId);

        // 2. Fire FlagChangeEvent with newValue=null (unset)
        FlagChangeEvent changeEvent = new FlagChangeEvent(flagId, scope, scopeId, oldValue, null, null);
        Bukkit.getPluginManager().callEvent(changeEvent);

        if (changeEvent.isCancelled()) {
            logger.log(Level.FINE, "unsetFlag: FlagChangeEvent cancelled for {0} [{1}:{2}]",
                    new Object[]{flagId, scope, scopeId});
            return;
        }

        // 3. Remove via storageManager
        storageManager.removeValue(flagId, scope, scopeId);

        // 4. Invalidate cache for this scopeId
        invalidateCache(scopeId);

        // 5. Call onDisable callback if the flag is registered
        if (flag != null) {
            try {
                flag.onDisable(scope, scopeId);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in onDisable callback for flag " + flagId, e);
            }
        }

        logger.log(Level.FINE, "unsetFlag: removed {0} [{1}:{2}]",
                new Object[]{flagId, scope, scopeId});
    }

    // ---------------------------------------------------------------
    //  Raw / direct access (no inheritance chain)
    // ---------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * Performs a direct storage lookup without walking the inheritance chain.
     * The raw string value is parsed to a typed value using the flag's parseValue method.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getRawFlagValue(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        Objects.requireNonNull(flagId, "flagId must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");

        Object resolved = resolveFromStorage(flagId, scope, scopeId);
        return (T) resolved;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves all flag values stored at the given scope and parses each one
     * using the corresponding flag's parseValue method.
     */
    @NotNull
    @Override
    public Map<String, Object> getAllFlagsForScope(@NotNull FlagScope scope, @NotNull String scopeId) {
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");

        Map<String, String> rawValues = storageManager.getAllForScope(scope, scopeId);
        if (rawValues.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rawValues.entrySet()) {
            String flagId = entry.getKey();
            String rawValue = entry.getValue();

            AbstractFlag<?> flag = registry.getFlag(flagId);
            if (flag == null) {
                // Flag is no longer registered; include the raw string value
                logger.log(Level.FINE, "getAllFlagsForScope: flag {0} not registered, including raw value", flagId);
                result.put(flagId, rawValue);
                continue;
            }

            try {
                Object parsed = flag.parseValue(rawValue);
                result.put(flagId, parsed);
            } catch (Exception e) {
                logger.log(Level.WARNING, "getAllFlagsForScope: failed to parse value for flag " + flagId +
                        " at " + scope + ":" + scopeId + " - raw value: " + rawValue, e);
                result.put(flagId, rawValue);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    // ---------------------------------------------------------------
    //  Cache management
    // ---------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearCache() {
        cache.clear();
        logger.log(Level.FINE, "Flag value cache cleared");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes all cached entries whose key contains the given scopeId.
     * Cache keys use the format {@code "flagId:scope:scopeId"}, so this
     * performs a suffix-match on the scopeId portion.
     */
    @Override
    public void invalidateCache(@NotNull String scopeId) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");

        // Remove all entries that contain this scopeId as part of their key
        cache.entrySet().removeIf(entry -> entry.getKey().contains(":" + scopeId));

        logger.log(Level.FINE, "Cache invalidated for scopeId: {0}", scopeId);
    }

    // ---------------------------------------------------------------
    //  Private helper methods
    // ---------------------------------------------------------------

    /**
     * Checks the cache first; if not found or expired, resolves from storage and caches the result.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @return the resolved value, or null if not set at this scope
     */
    @Nullable
    private Object getCachedOrResolve(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        Object cached = getCachedValue(flagId, scope, scopeId);
        if (cached != null) {
            return cached;
        }

        Object resolved = resolveFromStorage(flagId, scope, scopeId);
        if (resolved != null) {
            cacheValue(flagId, scope, scopeId, resolved);
        }
        return resolved;
    }

    /**
     * Retrieves a value from the cache if present and not expired.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @return the cached value, or null if not cached or expired
     */
    @Nullable
    private Object getCachedValue(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        String cacheKey = buildCacheKey(flagId, scope, scopeId);
        CachedEntry entry = cache.get(cacheKey);

        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(cacheKey);
            return null;
        }

        return entry.value;
    }

    /**
     * Stores a value in the cache with the configured TTL.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @param value   the value to cache (must not be null)
     */
    private void cacheValue(@NotNull String flagId, @NotNull FlagScope scope,
                            @NotNull String scopeId, @NotNull Object value) {
        String cacheKey = buildCacheKey(flagId, scope, scopeId);
        long expiresAt = System.currentTimeMillis() + cacheTtlMs;
        cache.put(cacheKey, new CachedEntry(value, expiresAt));
    }

    /**
     * Queries the storage manager for a flag value and parses it using the flag's
     * parseValue method. Returns null if the flag is not registered, the value
     * is not set in storage, or parsing fails.
     *
     * @param flagId  the unique flag identifier
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @return the parsed value, or null
     */
    @Nullable
    private Object resolveFromStorage(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        AbstractFlag<?> flag = registry.getFlag(flagId);
        if (flag == null) {
            return null;
        }

        String rawValue = storageManager.getValue(flagId, scope, scopeId);
        if (rawValue == null) {
            return null;
        }

        try {
            return flag.parseValue(rawValue);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse stored value for flag " + flagId +
                    " at " + scope + ":" + scopeId + " - raw value: " + rawValue, e);
            return null;
        }
    }

    /**
     * Builds a cache key in the format {@code "flagId:scope:scopeId"}.
     *
     * @param flagId  the flag identifier
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @return the cache key string
     */
    @NotNull
    private static String buildCacheKey(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId) {
        return flagId + ":" + scope.name() + ":" + scopeId;
    }

    // ---------------------------------------------------------------
    //  CachedEntry inner class
    // ---------------------------------------------------------------

    /**
     * Holds a cached flag value along with its expiration timestamp.
     */
    private static final class CachedEntry {

        final Object value;
        final long expiresAt;

        /**
         * Creates a new cache entry.
         *
         * @param value     the cached value
         * @param expiresAt the timestamp (in millis) at which this entry expires
         */
        CachedEntry(@NotNull Object value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        /**
         * Checks whether this cache entry has expired.
         *
         * @return true if the current time is past the expiration timestamp
         */
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    @Override
    public String toString() {
        return "FlagManagerImpl{cacheSize=" + cache.size() + ", cacheTtlMs=" + cacheTtlMs + "}";
    }
}
