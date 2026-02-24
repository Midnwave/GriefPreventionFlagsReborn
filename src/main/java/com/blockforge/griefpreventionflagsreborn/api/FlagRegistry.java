package com.blockforge.griefpreventionflagsreborn.api;

import com.blockforge.griefpreventionflagsreborn.flags.AbstractFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Registry for managing flag definitions and their associated {@link AbstractFlag} implementations.
 * <p>
 * Flags must be registered before they can be used by the plugin or queried via the API.
 * Third-party plugins can register custom flags through this interface.
 */
public interface FlagRegistry {

    /**
     * Registers a flag implementation. The flag's definition ID must be unique.
     *
     * @param flag the flag to register
     * @throws com.blockforge.griefpreventionflagsreborn.api.exceptions.FlagAlreadyRegisteredException
     *         if a flag with the same ID is already registered
     */
    void registerFlag(@NotNull AbstractFlag<?> flag);

    /**
     * Unregisters a flag by its ID. Does nothing if no flag with the given ID exists.
     *
     * @param flagId the unique flag identifier
     */
    void unregisterFlag(@NotNull String flagId);

    /**
     * Retrieves a registered flag by its ID.
     *
     * @param flagId the unique flag identifier
     * @return the flag, or null if not registered
     */
    @Nullable
    AbstractFlag<?> getFlag(@NotNull String flagId);

    /**
     * Returns all currently registered flags.
     *
     * @return an unmodifiable collection of all registered flags
     */
    @NotNull
    Collection<AbstractFlag<?>> getAllFlags();

    /**
     * Returns all registered flags belonging to the specified category.
     *
     * @param category the category to filter by
     * @return a list of flags in the given category
     */
    @NotNull
    List<AbstractFlag<?>> getFlagsByCategory(@NotNull FlagCategory category);

    /**
     * Checks whether a flag with the given ID is registered.
     *
     * @param flagId the unique flag identifier
     * @return true if the flag is registered
     */
    boolean hasFlag(@NotNull String flagId);

    /**
     * Returns the total number of registered flags.
     *
     * @return the flag count
     */
    int getFlagCount();
}
