package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A flag that holds a list of string values. Values are stored as comma-separated
 * strings and parsed by splitting on commas with whitespace trimming.
 */
public class ListFlag extends AbstractFlag<List<String>> {

    /**
     * Constructs a new ListFlag with the given definition.
     *
     * @param definition the flag definition
     */
    public ListFlag(@NotNull FlagDefinition definition) {
        super(definition);
    }

    /**
     * Parses a comma-separated string into a list of trimmed, non-empty values.
     *
     * @param input the raw string input (e.g. "item1, item2, item3")
     * @return an unmodifiable list of parsed values
     */
    @NotNull
    @Override
    public List<String> parseValue(@NotNull String input) {
        if (input.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return Collections.unmodifiableList(values);
    }

    /**
     * Serializes the list to a comma-separated string.
     *
     * @param value the list of values
     * @return the comma-joined string
     */
    @NotNull
    @Override
    public String serializeValue(@NotNull List<String> value) {
        return String.join(",", value);
    }

    @Override
    public boolean isValid(@NotNull List<String> value) {
        return true; // All non-null string lists are valid by default
    }

    /**
     * Returns a formatted display string in the form "[item1, item2, ...]".
     *
     * @param value the list of values
     * @return the formatted display string
     */
    @NotNull
    @Override
    public String getDisplayValue(@NotNull List<String> value) {
        if (value.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(", ", value) + "]";
    }

    /**
     * Adds items to an existing list, returning a new combined list.
     * Duplicate items are not added.
     *
     * @param existing the existing list of values
     * @param toAdd    the items to add
     * @return a new list containing all items from both lists (no duplicates)
     */
    @NotNull
    public List<String> addToList(@NotNull List<String> existing, @NotNull List<String> toAdd) {
        List<String> result = new ArrayList<>(existing);
        for (String item : toAdd) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Removes items from an existing list, returning a new list without the removed items.
     *
     * @param existing the existing list of values
     * @param toRemove the items to remove
     * @return a new list with the specified items removed
     */
    @NotNull
    public List<String> removeFromList(@NotNull List<String> existing, @NotNull List<String> toRemove) {
        List<String> result = new ArrayList<>(existing);
        result.removeAll(toRemove);
        return Collections.unmodifiableList(result);
    }
}
