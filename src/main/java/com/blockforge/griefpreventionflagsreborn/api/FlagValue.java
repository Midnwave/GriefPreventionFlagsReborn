package com.blockforge.griefpreventionflagsreborn.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A typed wrapper holding a resolved flag value along with metadata about
 * where it was set and by whom.
 *
 * @param <T> the value type corresponding to the flag's {@link FlagType}
 */
public final class FlagValue<T> {

    private final T value;
    private final FlagScope scope;
    private final String scopeId;
    @Nullable
    private final UUID setBy;

    /**
     * Constructs a new FlagValue.
     *
     * @param value   the flag value
     * @param scope   the scope at which this value was set
     * @param scopeId the identifier for the scope (e.g. world name, claim ID)
     * @param setBy   the UUID of the player who set the value, or null if set by the system
     */
    public FlagValue(@NotNull T value, @NotNull FlagScope scope, @NotNull String scopeId, @Nullable UUID setBy) {
        this.value = Objects.requireNonNull(value, "value must not be null");
        this.scope = Objects.requireNonNull(scope, "scope must not be null");
        this.scopeId = Objects.requireNonNull(scopeId, "scopeId must not be null");
        this.setBy = setBy;
    }

    /**
     * Returns the flag value.
     *
     * @return the value
     */
    @NotNull
    public T getValue() {
        return value;
    }

    /**
     * Returns the scope at which this value was set.
     *
     * @return the flag scope
     */
    @NotNull
    public FlagScope getScope() {
        return scope;
    }

    /**
     * Returns the identifier for the scope (e.g. world name, claim ID, "server").
     *
     * @return the scope identifier
     */
    @NotNull
    public String getScopeId() {
        return scopeId;
    }

    /**
     * Returns the UUID of the player who set this value, or null if it was set
     * by the system or via configuration.
     *
     * @return the setter's UUID, or null
     */
    @Nullable
    public UUID getSetBy() {
        return setBy;
    }

    /**
     * Determines whether this value is inherited relative to the given scope.
     * A value is considered inherited if it was set at a lower-priority scope
     * than the scope being queried.
     * <p>
     * For example, if a value was set at {@link FlagScope#WORLD} and the
     * current scope is {@link FlagScope#CLAIM}, the value is inherited.
     *
     * @param currentScope the scope being queried
     * @return true if this value was set at a lower-priority scope than currentScope
     */
    public boolean isInherited(@NotNull FlagScope currentScope) {
        return this.scope.getPriority() < currentScope.getPriority();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlagValue<?> that)) return false;
        return value.equals(that.value)
                && scope == that.scope
                && scopeId.equals(that.scopeId)
                && Objects.equals(setBy, that.setBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, scope, scopeId, setBy);
    }

    @Override
    public String toString() {
        return "FlagValue{value=" + value + ", scope=" + scope + ", scopeId='" + scopeId + "'" +
                (setBy != null ? ", setBy=" + setBy : "") + "}";
    }
}
