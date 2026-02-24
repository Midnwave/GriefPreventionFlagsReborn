package com.blockforge.griefpreventionflagsreborn.api;

import org.jetbrains.annotations.NotNull;

/**
 * Static singleton accessor for the GriefPreventionFlagsReborn API.
 * <p>
 * This class is initialized by the plugin during startup via {@link #init(FlagRegistry, FlagManager)}.
 * Third-party plugins should access the API through {@link #getRegistry()} and {@link #getManager()}.
 * <p>
 * Example usage:
 * <pre>{@code
 * if (GPFRApi.isAvailable()) {
 *     FlagRegistry registry = GPFRApi.getRegistry();
 *     FlagManager manager = GPFRApi.getManager();
 *     // ...
 * }
 * }</pre>
 */
public final class GPFRApi {

    private static FlagRegistry registry;
    private static FlagManager manager;

    private GPFRApi() {
        // Prevent instantiation
    }

    /**
     * Initializes the API with the given implementations. This method is called
     * by the GriefPreventionFlagsReborn plugin during startup and should NOT be
     * called by external plugins.
     *
     * @param registry the flag registry implementation
     * @param manager  the flag manager implementation
     * @throws IllegalStateException if the API has already been initialized
     */
    public static void init(@NotNull FlagRegistry registry, @NotNull FlagManager manager) {
        if (GPFRApi.registry != null || GPFRApi.manager != null) {
            throw new IllegalStateException("GPFRApi has already been initialized");
        }
        GPFRApi.registry = registry;
        GPFRApi.manager = manager;
    }

    /**
     * Returns the flag registry for registering, unregistering, and querying flag definitions.
     *
     * @return the flag registry
     * @throws IllegalStateException if the API has not been initialized yet
     */
    @NotNull
    public static FlagRegistry getRegistry() {
        if (registry == null) {
            throw new IllegalStateException(
                    "GPFRApi is not available. Ensure GriefPreventionFlagsReborn is loaded and enabled.");
        }
        return registry;
    }

    /**
     * Returns the flag manager for reading and writing flag values.
     *
     * @return the flag manager
     * @throws IllegalStateException if the API has not been initialized yet
     */
    @NotNull
    public static FlagManager getManager() {
        if (manager == null) {
            throw new IllegalStateException(
                    "GPFRApi is not available. Ensure GriefPreventionFlagsReborn is loaded and enabled.");
        }
        return manager;
    }

    /**
     * Checks whether the API has been initialized and is ready for use.
     *
     * @return true if both the registry and manager are available
     */
    public static boolean isAvailable() {
        return registry != null && manager != null;
    }

    /**
     * Shuts down the API. Called by the plugin during disable.
     * This method is internal and should NOT be called by external plugins.
     */
    public static void shutdown() {
        registry = null;
        manager = null;
    }
}
