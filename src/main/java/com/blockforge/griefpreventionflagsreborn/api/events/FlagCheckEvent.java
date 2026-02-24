package com.blockforge.griefpreventionflagsreborn.api.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a flag value is being resolved/checked at a specific location.
 * <p>
 * This event is NOT cancellable. However, listeners can override the resolved value
 * via {@link #setOverrideValue(Object)} to allow other plugins to dynamically alter
 * flag behavior.
 */
public class FlagCheckEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final String flagId;
    private final Location location;
    private final Object resolvedValue;
    @Nullable
    private Object overrideValue;

    /**
     * Constructs a new FlagCheckEvent.
     *
     * @param flagId        the flag being checked
     * @param location      the location at which the flag is being resolved
     * @param resolvedValue the value resolved through normal scope resolution
     */
    public FlagCheckEvent(@NotNull String flagId,
                          @NotNull Location location,
                          @Nullable Object resolvedValue) {
        super(false);
        this.flagId = flagId;
        this.location = location;
        this.resolvedValue = resolvedValue;
        this.overrideValue = null;
    }

    /**
     * Returns the ID of the flag being checked.
     *
     * @return the flag ID
     */
    @NotNull
    public String getFlagId() {
        return flagId;
    }

    /**
     * Returns the location at which the flag is being resolved.
     *
     * @return the location
     */
    @NotNull
    public Location getLocation() {
        return location;
    }

    /**
     * Returns the value resolved through normal scope resolution (before any override).
     *
     * @return the originally resolved value
     */
    @Nullable
    public Object getResolvedValue() {
        return resolvedValue;
    }

    /**
     * Returns the override value if one has been set by a listener, or null if
     * no override has been applied.
     *
     * @return the override value, or null
     */
    @Nullable
    public Object getOverrideValue() {
        return overrideValue;
    }

    /**
     * Sets an override value that will be used instead of the normally resolved value.
     * This allows other plugins to dynamically modify flag behavior based on custom logic.
     *
     * @param overrideValue the value to use instead of the resolved value
     */
    public void setOverrideValue(@Nullable Object overrideValue) {
        this.overrideValue = overrideValue;
    }

    /**
     * Returns whether an override value has been set by a listener.
     *
     * @return true if an override value has been set
     */
    public boolean hasOverride() {
        return overrideValue != null;
    }

    /**
     * Returns the effective value, taking into account any override.
     * If an override has been set, it is returned; otherwise, the originally resolved value is used.
     *
     * @return the effective flag value
     */
    @Nullable
    public Object getEffectiveValue() {
        return overrideValue != null ? overrideValue : resolvedValue;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
