package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.FlagAlreadyRegisteredException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe implementation of {@link FlagRegistry} backed by a {@link ConcurrentHashMap}.
 * <p>
 * All flag registrations are keyed by the flag's unique ID (from its {@link com.blockforge.griefpreventionflagsreborn.api.FlagDefinition}).
 * Duplicate registration attempts throw {@link FlagAlreadyRegisteredException}.
 */
public final class FlagRegistryImpl implements FlagRegistry {

    private final ConcurrentHashMap<String, AbstractFlag<?>> flags = new ConcurrentHashMap<>();
    private final Logger logger;

    /**
     * Creates a new FlagRegistryImpl.
     *
     * @param logger the logger for diagnostic output
     */
    public FlagRegistryImpl(@NotNull Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * @throws FlagAlreadyRegisteredException if a flag with the same ID is already registered
     * @throws NullPointerException           if flag is null
     */
    @Override
    public void registerFlag(@NotNull AbstractFlag<?> flag) {
        Objects.requireNonNull(flag, "flag must not be null");

        String flagId = flag.getId();
        AbstractFlag<?> existing = flags.putIfAbsent(flagId, flag);

        if (existing != null) {
            throw new FlagAlreadyRegisteredException(flagId);
        }

        logger.log(Level.FINE, "Registered flag: {0} (category: {1}, type: {2})",
                new Object[]{flagId, flag.getCategory().name(), flag.getType().name()});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterFlag(@NotNull String flagId) {
        Objects.requireNonNull(flagId, "flagId must not be null");

        AbstractFlag<?> removed = flags.remove(flagId);

        if (removed != null) {
            logger.log(Level.FINE, "Unregistered flag: {0}", flagId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public AbstractFlag<?> getFlag(@NotNull String flagId) {
        Objects.requireNonNull(flagId, "flagId must not be null");
        return flags.get(flagId);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Collection<AbstractFlag<?>> getAllFlags() {
        return Collections.unmodifiableCollection(flags.values());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns flags matching the given category, sorted alphabetically by flag ID.
     */
    @NotNull
    @Override
    public List<AbstractFlag<?>> getFlagsByCategory(@NotNull FlagCategory category) {
        Objects.requireNonNull(category, "category must not be null");

        return flags.values().stream()
                .filter(flag -> flag.getCategory() == category)
                .sorted(Comparator.comparing(AbstractFlag::getId))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasFlag(@NotNull String flagId) {
        Objects.requireNonNull(flagId, "flagId must not be null");
        return flags.containsKey(flagId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlagCount() {
        return flags.size();
    }

    @Override
    public String toString() {
        return "FlagRegistryImpl{flagCount=" + flags.size() + "}";
    }
}
