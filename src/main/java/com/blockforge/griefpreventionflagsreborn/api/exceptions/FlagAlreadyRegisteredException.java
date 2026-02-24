package com.blockforge.griefpreventionflagsreborn.api.exceptions;

/**
 * Thrown when attempting to register a flag with an ID that is already in use
 * by another registered flag.
 */
public class FlagAlreadyRegisteredException extends RuntimeException {

    private final String flagId;

    /**
     * Constructs a new exception for the given flag ID.
     *
     * @param flagId the ID of the flag that is already registered
     */
    public FlagAlreadyRegisteredException(String flagId) {
        super("A flag with ID '" + flagId + "' is already registered.");
        this.flagId = flagId;
    }

    /**
     * Returns the flag ID that caused the conflict.
     *
     * @return the conflicting flag ID
     */
    public String getFlagId() {
        return flagId;
    }
}
