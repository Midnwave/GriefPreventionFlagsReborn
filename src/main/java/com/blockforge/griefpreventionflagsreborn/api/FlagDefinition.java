package com.blockforge.griefpreventionflagsreborn.api;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable definition of a flag, describing its metadata, type constraints,
 * default value, allowed scopes, and GUI representation.
 * <p>
 * Use {@link #builder()} to construct instances via the fluent {@link Builder} API.
 */
public final class FlagDefinition {

    private final String id;
    private final String displayName;
    private final String description;
    private final FlagType type;
    private final FlagCategory category;
    private final Object defaultValue;
    private final Set<FlagScope> allowedScopes;
    @Nullable
    private final Set<String> allowedEnumValues;
    private final Material guiIcon;
    private final boolean adminOnly;

    private FlagDefinition(String id,
                           String displayName,
                           String description,
                           FlagType type,
                           FlagCategory category,
                           Object defaultValue,
                           Set<FlagScope> allowedScopes,
                           @Nullable Set<String> allowedEnumValues,
                           Material guiIcon,
                           boolean adminOnly) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.defaultValue = defaultValue;
        this.allowedScopes = Collections.unmodifiableSet(EnumSet.copyOf(
                Objects.requireNonNull(allowedScopes, "allowedScopes must not be null")));
        this.allowedEnumValues = allowedEnumValues != null
                ? Collections.unmodifiableSet(new LinkedHashSet<>(allowedEnumValues))
                : null;
        this.guiIcon = Objects.requireNonNull(guiIcon, "guiIcon must not be null");
        this.adminOnly = adminOnly;
    }

    /**
     * Returns a new {@link Builder} for constructing a FlagDefinition.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public FlagType getType() {
        return type;
    }

    @NotNull
    public FlagCategory getCategory() {
        return category;
    }

    @Nullable
    public Object getDefaultValue() {
        return defaultValue;
    }

    @NotNull
    public Set<FlagScope> getAllowedScopes() {
        return allowedScopes;
    }

    /**
     * Returns the set of allowed enum values, or null if the flag is not constrained
     * to a fixed set of string values.
     *
     * @return the allowed enum values, or null
     */
    @Nullable
    public Set<String> getAllowedEnumValues() {
        return allowedEnumValues;
    }

    @NotNull
    public Material getGuiIcon() {
        return guiIcon;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    /**
     * Returns whether the given scope is permitted for this flag.
     *
     * @param scope the scope to check
     * @return true if the scope is allowed
     */
    public boolean isScopeAllowed(@NotNull FlagScope scope) {
        return allowedScopes.contains(scope);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlagDefinition that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "FlagDefinition{id='" + id + "', type=" + type + ", category=" + category + "}";
    }

    /**
     * Fluent builder for constructing {@link FlagDefinition} instances.
     */
    public static final class Builder {

        private String id;
        private String displayName;
        private String description = "";
        private FlagType type;
        private FlagCategory category;
        private Object defaultValue;
        private Set<FlagScope> allowedScopes = EnumSet.allOf(FlagScope.class);
        @Nullable
        private Set<String> allowedEnumValues;
        private Material guiIcon = Material.PAPER;
        private boolean adminOnly = false;

        private Builder() {
        }

        @NotNull
        public Builder id(@NotNull String id) {
            this.id = id;
            return this;
        }

        @NotNull
        public Builder displayName(@NotNull String displayName) {
            this.displayName = displayName;
            return this;
        }

        @NotNull
        public Builder description(@NotNull String description) {
            this.description = description;
            return this;
        }

        @NotNull
        public Builder type(@NotNull FlagType type) {
            this.type = type;
            return this;
        }

        @NotNull
        public Builder category(@NotNull FlagCategory category) {
            this.category = category;
            return this;
        }

        @NotNull
        public Builder defaultValue(@Nullable Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @NotNull
        public Builder allowedScopes(@NotNull Set<FlagScope> allowedScopes) {
            this.allowedScopes = allowedScopes;
            return this;
        }

        @NotNull
        public Builder allowedScopes(@NotNull FlagScope... scopes) {
            this.allowedScopes = scopes.length > 0
                    ? EnumSet.copyOf(Set.of(scopes))
                    : EnumSet.noneOf(FlagScope.class);
            return this;
        }

        @NotNull
        public Builder allowedEnumValues(@Nullable Set<String> allowedEnumValues) {
            this.allowedEnumValues = allowedEnumValues;
            return this;
        }

        @NotNull
        public Builder guiIcon(@NotNull Material guiIcon) {
            this.guiIcon = guiIcon;
            return this;
        }

        @NotNull
        public Builder adminOnly(boolean adminOnly) {
            this.adminOnly = adminOnly;
            return this;
        }

        /**
         * Builds and returns the immutable {@link FlagDefinition}.
         *
         * @return the constructed FlagDefinition
         * @throws NullPointerException if any required field is null
         * @throws IllegalStateException if allowedScopes is empty
         */
        @NotNull
        public FlagDefinition build() {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(displayName, "displayName is required");
            Objects.requireNonNull(type, "type is required");
            Objects.requireNonNull(category, "category is required");
            if (allowedScopes == null || allowedScopes.isEmpty()) {
                throw new IllegalStateException("allowedScopes must contain at least one scope");
            }
            return new FlagDefinition(id, displayName, description, type, category,
                    defaultValue, allowedScopes, allowedEnumValues, guiIcon, adminOnly);
        }
    }
}
