package com.blockforge.griefpreventionflagsreborn.api.events;

import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired when a flag value is about to be changed or unset.
 * <p>
 * This event is {@link Cancellable}. If cancelled, the flag change will not take effect.
 */
public class FlagChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final String flagId;
    private final FlagScope scope;
    private final String scopeId;
    @Nullable
    private final Object oldValue;
    @Nullable
    private final Object newValue;
    @Nullable
    private final UUID changedBy;

    private boolean cancelled = false;

    /**
     * Constructs a new FlagChangeEvent.
     *
     * @param flagId    the flag being changed
     * @param scope     the scope at which the change is occurring
     * @param scopeId   the scope identifier
     * @param oldValue  the previous value, or null if the flag was not set
     * @param newValue  the new value, or null if the flag is being unset
     * @param changedBy the UUID of the player making the change, or null for system changes
     */
    public FlagChangeEvent(@NotNull String flagId,
                           @NotNull FlagScope scope,
                           @NotNull String scopeId,
                           @Nullable Object oldValue,
                           @Nullable Object newValue,
                           @Nullable UUID changedBy) {
        super(false);
        this.flagId = flagId;
        this.scope = scope;
        this.scopeId = scopeId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
    }

    /**
     * Returns the ID of the flag being changed.
     *
     * @return the flag ID
     */
    @NotNull
    public String getFlagId() {
        return flagId;
    }

    /**
     * Returns the scope at which the change is occurring.
     *
     * @return the flag scope
     */
    @NotNull
    public FlagScope getScope() {
        return scope;
    }

    /**
     * Returns the scope identifier (e.g. world name, claim ID).
     *
     * @return the scope ID
     */
    @NotNull
    public String getScopeId() {
        return scopeId;
    }

    /**
     * Returns the previous value of the flag, or null if it was not previously set.
     *
     * @return the old value, or null
     */
    @Nullable
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Returns the new value being set, or null if the flag is being unset.
     *
     * @return the new value, or null
     */
    @Nullable
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Returns the UUID of the player who initiated the change, or null for system changes.
     *
     * @return the changer's UUID, or null
     */
    @Nullable
    public UUID getChangedBy() {
        return changedBy;
    }

    /**
     * Returns whether this is an unset operation (new value is null).
     *
     * @return true if the flag is being unset
     */
    public boolean isUnset() {
        return newValue == null;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
