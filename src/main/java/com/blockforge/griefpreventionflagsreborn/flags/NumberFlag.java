package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import org.jetbrains.annotations.NotNull;

/**
 * A flag that holds a numeric value with configurable minimum and maximum bounds.
 * Values are stored as {@link Number} (specifically {@link Double} internally)
 * to support both integer and decimal representations.
 */
public class NumberFlag extends AbstractFlag<Number> {

    private final double min;
    private final double max;

    /**
     * Constructs a NumberFlag with the given definition and bounds.
     *
     * @param definition the flag definition
     * @param min        the minimum allowed value (inclusive)
     * @param max        the maximum allowed value (inclusive)
     */
    public NumberFlag(@NotNull FlagDefinition definition, double min, double max) {
        super(definition);
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not be greater than max (" + max + ")");
        }
        this.min = min;
        this.max = max;
    }

    /**
     * Constructs a NumberFlag with no bounds (Double.MIN_VALUE to Double.MAX_VALUE).
     *
     * @param definition the flag definition
     */
    public NumberFlag(@NotNull FlagDefinition definition) {
        this(definition, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /**
     * Parses the input string as a double and validates it against the configured bounds.
     *
     * @param input the raw string input
     * @return the parsed number
     * @throws InvalidFlagValueException if the input is not a valid number or is out of range
     */
    @NotNull
    @Override
    public Number parseValue(@NotNull String input) {
        double value;
        try {
            value = Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            throw new InvalidFlagValueException(getId(),
                    "'" + input + "' is not a valid number.");
        }
        if (value < min || value > max) {
            throw new InvalidFlagValueException(getId(),
                    "Value " + value + " is out of range. Must be between " +
                            formatNumber(min) + " and " + formatNumber(max) + ".");
        }
        return value;
    }

    @NotNull
    @Override
    public String serializeValue(@NotNull Number value) {
        return value.toString();
    }

    @Override
    public boolean isValid(@NotNull Number value) {
        double d = value.doubleValue();
        return d >= min && d <= max;
    }

    /**
     * Formats the number to 2 decimal places for display.
     *
     * @param value the number value
     * @return the formatted display string
     */
    @NotNull
    @Override
    public String getDisplayValue(@NotNull Number value) {
        return formatNumber(value.doubleValue());
    }

    /**
     * Returns the minimum allowed value.
     *
     * @return the minimum bound
     */
    public double getMin() {
        return min;
    }

    /**
     * Returns the maximum allowed value.
     *
     * @return the maximum bound
     */
    public double getMax() {
        return max;
    }

    private static String formatNumber(double value) {
        return String.format("%.2f", value);
    }
}
