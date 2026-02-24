package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A flag that holds a list of Material names. Each entry is validated against
 * the Bukkit {@link Material} enum. Values are stored uppercase to match
 * Material enum naming conventions.
 */
public class MaterialListFlag extends AbstractFlag<List<String>> {

    /**
     * Constructs a new MaterialListFlag with the given definition.
     *
     * @param definition the flag definition
     */
    public MaterialListFlag(@NotNull FlagDefinition definition) {
        super(definition);
    }

    /**
     * Parses a comma-separated string into a list of validated Material names.
     * Each entry is trimmed, uppercased, and validated against the Material enum.
     *
     * @param input the raw string input (e.g. "STONE, OAK_LOG, DIAMOND_BLOCK")
     * @return an unmodifiable list of validated Material names
     * @throws InvalidFlagValueException if any entry is not a valid Material name
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
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        for (String materialName : values) {
            if (Material.matchMaterial(materialName) == null) {
                throw new InvalidFlagValueException(getId(),
                        "'" + materialName + "' is not a valid Material name.");
            }
        }

        return Collections.unmodifiableList(values);
    }

    @NotNull
    @Override
    public String serializeValue(@NotNull List<String> value) {
        return String.join(",", value);
    }

    /**
     * Validates that every entry in the list is a valid Material name.
     *
     * @param value the list to validate
     * @return true if all entries are valid Materials
     */
    @Override
    public boolean isValid(@NotNull List<String> value) {
        for (String materialName : value) {
            if (Material.matchMaterial(materialName.toUpperCase()) == null) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    @Override
    public String getDisplayValue(@NotNull List<String> value) {
        if (value.isEmpty()) {
            return "[]";
        }
        return "[" + String.join(", ", value) + "]";
    }

    /**
     * Adds Material names to an existing list, returning a new combined list.
     * Duplicate entries are not added. New entries are validated against the Material enum.
     *
     * @param existing the existing list of Material names
     * @param toAdd    the Material names to add
     * @return a new list containing all items from both lists (no duplicates)
     * @throws InvalidFlagValueException if any entry in toAdd is not a valid Material
     */
    @NotNull
    public List<String> addToList(@NotNull List<String> existing, @NotNull List<String> toAdd) {
        List<String> result = new ArrayList<>(existing);
        for (String item : toAdd) {
            String upper = item.toUpperCase();
            if (Material.matchMaterial(upper) == null) {
                throw new InvalidFlagValueException(getId(),
                        "'" + upper + "' is not a valid Material name.");
            }
            if (!result.contains(upper)) {
                result.add(upper);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Removes Material names from an existing list, returning a new list without the removed items.
     *
     * @param existing the existing list of Material names
     * @param toRemove the Material names to remove
     * @return a new list with the specified items removed
     */
    @NotNull
    public List<String> removeFromList(@NotNull List<String> existing, @NotNull List<String> toRemove) {
        List<String> upperToRemove = toRemove.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        List<String> result = new ArrayList<>(existing);
        result.removeAll(upperToRemove);
        return Collections.unmodifiableList(result);
    }
}
