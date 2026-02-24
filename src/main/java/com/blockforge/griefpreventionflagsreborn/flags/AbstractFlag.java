package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Abstract base class for all flag implementations. Each flag holds an immutable
 * {@link FlagDefinition} and provides type-safe parsing, serialization, validation,
 * and display logic for its value type.
 * <p>
 * Subclasses must implement the abstract methods for their specific value type.
 * Optionally, subclasses can override {@link #onEnable(FlagScope, String)} and
 * {@link #onDisable(FlagScope, String)} to react to flag value changes.
 *
 * @param <T> the Java type of the flag's value
 */
public abstract class AbstractFlag<T> {

    private final FlagDefinition definition;

    /**
     * Constructs a new flag with the given definition.
     *
     * @param definition the immutable flag definition
     */
    protected AbstractFlag(@NotNull FlagDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
    }

    /**
     * Parses a string representation into the flag's typed value.
     *
     * @param input the raw string input (e.g. from a command or config)
     * @return the parsed value
     * @throws com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException
     *         if the input cannot be parsed or is invalid
     */
    @NotNull
    public abstract T parseValue(@NotNull String input);

    /**
     * Serializes the typed value back to a string for storage.
     *
     * @param value the typed value
     * @return the string representation suitable for persistence
     */
    @NotNull
    public abstract String serializeValue(@NotNull T value);

    /**
     * Validates whether the given value is acceptable for this flag.
     *
     * @param value the value to validate
     * @return true if the value is valid
     */
    public abstract boolean isValid(@NotNull T value);

    /**
     * Returns a human-readable display representation of the value,
     * suitable for showing in chat messages or GUIs.
     *
     * @param value the value to display
     * @return a formatted display string
     */
    @NotNull
    public abstract String getDisplayValue(@NotNull T value);

    /**
     * Returns the unique identifier for this flag.
     *
     * @return the flag ID
     */
    @NotNull
    public String getId() {
        return definition.getId();
    }

    /**
     * Returns the immutable definition for this flag.
     *
     * @return the flag definition
     */
    @NotNull
    public FlagDefinition getDefinition() {
        return definition;
    }

    /**
     * Returns the category this flag belongs to.
     *
     * @return the flag category
     */
    @NotNull
    public FlagCategory getCategory() {
        return definition.getCategory();
    }

    /**
     * Returns the data type of this flag.
     *
     * @return the flag type
     */
    @NotNull
    public FlagType getType() {
        return definition.getType();
    }

    /**
     * Called when this flag is enabled (set to a truthy/active value) at a given scope.
     * Override to perform side effects when the flag is activated.
     *
     * @param scope   the scope where the flag was enabled
     * @param scopeId the scope identifier
     */
    public void onEnable(@NotNull FlagScope scope, @NotNull String scopeId) {
        // Default no-op; override in subclasses as needed
    }

    /**
     * Called when this flag is disabled (unset or set to a falsy/inactive value) at a given scope.
     * Override to perform side effects when the flag is deactivated.
     *
     * @param scope   the scope where the flag was disabled
     * @param scopeId the scope identifier
     */
    public void onDisable(@NotNull FlagScope scope, @NotNull String scopeId) {
        // Default no-op; override in subclasses as needed
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractFlag<?> that)) return false;
        return definition.getId().equals(that.definition.getId());
    }

    @Override
    public int hashCode() {
        return definition.getId().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + getId() + "'}";
    }
}
