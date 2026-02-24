package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A flag that holds a string value. If the flag definition specifies
 * {@link FlagDefinition#getAllowedEnumValues()}, the value is validated against
 * that set of allowed strings.
 */
public class StringFlag extends AbstractFlag<String> {

    /**
     * Constructs a new StringFlag with the given definition.
     *
     * @param definition the flag definition
     */
    public StringFlag(@NotNull FlagDefinition definition) {
        super(definition);
    }

    /**
     * Parses the input string. If the flag definition has allowed enum values,
     * the input is validated against them (case-insensitive matching, stored in
     * the case it was found).
     *
     * @param input the raw string input
     * @return the parsed string value
     * @throws InvalidFlagValueException if the value is not in the allowed enum values
     */
    @NotNull
    @Override
    public String parseValue(@NotNull String input) {
        String trimmed = input.trim();
        if (!isValid(trimmed)) {
            Set<String> allowed = getDefinition().getAllowedEnumValues();
            throw new InvalidFlagValueException(getId(),
                    "'" + trimmed + "' is not an allowed value. Allowed: " + allowed);
        }
        return trimmed;
    }

    @NotNull
    @Override
    public String serializeValue(@NotNull String value) {
        return value;
    }

    /**
     * Validates the value. If the definition specifies allowed enum values,
     * the value must match one of them (case-insensitive).
     *
     * @param value the value to validate
     * @return true if valid
     */
    @Override
    public boolean isValid(@NotNull String value) {
        Set<String> allowed = getDefinition().getAllowedEnumValues();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        return allowed.stream().anyMatch(v -> v.equalsIgnoreCase(value));
    }

    @NotNull
    @Override
    public String getDisplayValue(@NotNull String value) {
        return value;
    }
}
