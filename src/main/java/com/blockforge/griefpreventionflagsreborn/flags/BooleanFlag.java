package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A flag that holds a boolean value. Supports parsing of common truthy/falsy
 * representations from user input.
 */
public class BooleanFlag extends AbstractFlag<Boolean> {

    private static final Set<String> TRUE_VALUES = Set.of(
            "true", "yes", "on", "1", "enable", "enabled"
    );

    private static final Set<String> FALSE_VALUES = Set.of(
            "false", "no", "off", "0", "disable", "disabled"
    );

    /**
     * Constructs a new BooleanFlag with the given definition.
     *
     * @param definition the flag definition (should have type {@link com.blockforge.griefpreventionflagsreborn.api.FlagType#BOOLEAN})
     */
    public BooleanFlag(@NotNull FlagDefinition definition) {
        super(definition);
    }

    /**
     * Parses the input string into a boolean value.
     * Accepted truthy values: "true", "yes", "on", "1", "enable", "enabled" (case-insensitive).
     * Accepted falsy values: "false", "no", "off", "0", "disable", "disabled" (case-insensitive).
     *
     * @param input the raw string input
     * @return the parsed boolean value
     * @throws InvalidFlagValueException if the input is not a recognized boolean representation
     */
    @NotNull
    @Override
    public Boolean parseValue(@NotNull String input) {
        String normalized = input.trim().toLowerCase();
        if (TRUE_VALUES.contains(normalized)) {
            return Boolean.TRUE;
        }
        if (FALSE_VALUES.contains(normalized)) {
            return Boolean.FALSE;
        }
        throw new InvalidFlagValueException(getId(),
                "'" + input + "' is not a valid boolean. Use: true/false, yes/no, on/off, 1/0, enable/disable");
    }

    @NotNull
    @Override
    public String serializeValue(@NotNull Boolean value) {
        return value.toString();
    }

    @Override
    public boolean isValid(@NotNull Boolean value) {
        return true; // All boolean values are valid
    }

    /**
     * Returns "Enabled" for true, "Disabled" for false.
     *
     * @param value the boolean value
     * @return "Enabled" or "Disabled"
     */
    @NotNull
    @Override
    public String getDisplayValue(@NotNull Boolean value) {
        return value ? "Enabled" : "Disabled";
    }
}
