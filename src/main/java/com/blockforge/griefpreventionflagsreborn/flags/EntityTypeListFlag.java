package com.blockforge.griefpreventionflagsreborn.flags;

import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A flag that holds a list of EntityType names. Each entry is validated against
 * the Bukkit {@link EntityType} enum. Values are stored uppercase to match
 * EntityType enum naming conventions.
 */
public class EntityTypeListFlag extends AbstractFlag<List<String>> {

    /**
     * Constructs a new EntityTypeListFlag with the given definition.
     *
     * @param definition the flag definition
     */
    public EntityTypeListFlag(@NotNull FlagDefinition definition) {
        super(definition);
    }

    /**
     * Parses a comma-separated string into a list of validated EntityType names.
     * Each entry is trimmed, uppercased, and validated against the EntityType enum.
     *
     * @param input the raw string input (e.g. "ZOMBIE, SKELETON, CREEPER")
     * @return an unmodifiable list of validated EntityType names
     * @throws InvalidFlagValueException if any entry is not a valid EntityType name
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

        for (String entityTypeName : values) {
            if (!isValidEntityType(entityTypeName)) {
                throw new InvalidFlagValueException(getId(),
                        "'" + entityTypeName + "' is not a valid EntityType name.");
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
     * Validates that every entry in the list is a valid EntityType name.
     *
     * @param value the list to validate
     * @return true if all entries are valid EntityTypes
     */
    @Override
    public boolean isValid(@NotNull List<String> value) {
        for (String entityTypeName : value) {
            if (!isValidEntityType(entityTypeName.toUpperCase())) {
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
     * Adds EntityType names to an existing list, returning a new combined list.
     * Duplicate entries are not added. New entries are validated against the EntityType enum.
     *
     * @param existing the existing list of EntityType names
     * @param toAdd    the EntityType names to add
     * @return a new list containing all items from both lists (no duplicates)
     * @throws InvalidFlagValueException if any entry in toAdd is not a valid EntityType
     */
    @NotNull
    public List<String> addToList(@NotNull List<String> existing, @NotNull List<String> toAdd) {
        List<String> result = new ArrayList<>(existing);
        for (String item : toAdd) {
            String upper = item.toUpperCase();
            if (!isValidEntityType(upper)) {
                throw new InvalidFlagValueException(getId(),
                        "'" + upper + "' is not a valid EntityType name.");
            }
            if (!result.contains(upper)) {
                result.add(upper);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Removes EntityType names from an existing list, returning a new list without the removed items.
     *
     * @param existing the existing list of EntityType names
     * @param toRemove the EntityType names to remove
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

    /**
     * Checks whether the given name matches a valid Bukkit EntityType.
     *
     * @param name the uppercased entity type name
     * @return true if the name is a valid EntityType
     */
    private static boolean isValidEntityType(@NotNull String name) {
        try {
            EntityType.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
