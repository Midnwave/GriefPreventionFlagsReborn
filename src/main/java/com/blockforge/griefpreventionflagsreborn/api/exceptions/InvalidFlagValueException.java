package com.blockforge.griefpreventionflagsreborn.api.exceptions;

/**
 * Thrown when a provided flag value is invalid, either because it cannot be parsed
 * into the expected type or because it fails validation constraints.
 */
public class InvalidFlagValueException extends RuntimeException {

    private final String flagId;

    /**
     * Constructs a new exception for the given flag ID and detail message.
     *
     * @param flagId  the ID of the flag that received an invalid value
     * @param message a description of why the value is invalid
     */
    public InvalidFlagValueException(String flagId, String message) {
        super("Invalid value for flag '" + flagId + "': " + message);
        this.flagId = flagId;
    }

    /**
     * Returns the flag ID that the invalid value was provided for.
     *
     * @return the flag ID
     */
    public String getFlagId() {
        return flagId;
    }
}
