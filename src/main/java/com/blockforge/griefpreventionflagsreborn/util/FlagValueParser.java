package com.blockforge.griefpreventionflagsreborn.util;

import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.AbstractFlag;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for parsing string input into typed flag values.
 * <p>
 * Provides methods for each supported {@link FlagType}, including boolean,
 * integer, double, string lists, material lists, and entity type lists.
 * Input validation is performed where applicable (e.g., Material and EntityType lookups).
 */
public final class FlagValueParser {

    private static final Set<String> TRUE_VALUES = Set.of("true", "yes", "on", "1", "enable", "enabled");
    private static final Set<String> FALSE_VALUES = Set.of("false", "no", "off", "0", "disable", "disabled");

    private FlagValueParser() {
        // Utility class - no instantiation
    }

    /**
     * Parses a string as a boolean value.
     * Accepts: true/yes/on/1/enable/enabled for true; false/no/off/0/disable/disabled for false.
     *
     * @param input the string to parse
     * @return the parsed boolean, or null if the input is not a recognized boolean string
     */
    @Nullable
    public static Boolean parseBoolean(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String lower = input.trim().toLowerCase(Locale.ROOT);
        if (TRUE_VALUES.contains(lower)) {
            return Boolean.TRUE;
        }
        if (FALSE_VALUES.contains(lower)) {
            return Boolean.FALSE;
        }
        return null;
    }

    /**
     * Parses a string as an integer value.
     *
     * @param input the string to parse
     * @return the parsed integer, or null if the input is not a valid integer
     */
    @Nullable
    public static Integer parseInt(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a string as a double value.
     *
     * @param input the string to parse
     * @return the parsed double, or null if the input is not a valid double
     */
    @Nullable
    public static Double parseDouble(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a comma-separated string into a list of trimmed strings.
     *
     * @param input the comma-separated input
     * @return a list of trimmed, non-empty strings; never null
     */
    @NotNull
    public static List<String> parseList(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Parses a comma-separated string into a list of valid Bukkit {@link Material} names.
     * Each entry is validated against {@link Material#matchMaterial(String)}.
     * Invalid entries are silently excluded from the result.
     *
     * @param input the comma-separated material names
     * @return a list of validated material name strings (uppercase); never null
     */
    @NotNull
    public static List<String> parseMaterialList(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String entry : input.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Material material = Material.matchMaterial(trimmed);
            if (material != null) {
                result.add(material.name());
            }
        }
        return List.copyOf(result);
    }

    /**
     * Parses a comma-separated string into a list of valid Bukkit {@link EntityType} names.
     * Each entry is validated against {@link EntityType#valueOf(String)}.
     * Invalid entries are silently excluded from the result.
     *
     * @param input the comma-separated entity type names
     * @return a list of validated entity type name strings (uppercase); never null
     */
    @NotNull
    public static List<String> parseEntityTypeList(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String entry : input.split(",")) {
            String trimmed = entry.trim().toUpperCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                EntityType entityType = EntityType.valueOf(trimmed);
                result.add(entityType.name());
            } catch (IllegalArgumentException ignored) {
                // Invalid entity type, skip
            }
        }
        return List.copyOf(result);
    }

    /**
     * Parses a string input into the correct typed value based on the given flag's
     * {@link FlagType}. Delegates to the appropriate type-specific parse method.
     *
     * @param flag  the flag whose type determines the parsing strategy
     * @param input the raw string input to parse
     * @return the parsed value as the appropriate type, or null if parsing fails
     */
    @Nullable
    public static Object parseForFlag(@NotNull AbstractFlag<?> flag, @NotNull String input) {
        FlagDefinition definition = flag.getDefinition();
        FlagType type = definition.getType();

        return switch (type) {
            case BOOLEAN -> parseBoolean(input);
            case INTEGER -> parseInt(input);
            case DOUBLE -> parseDouble(input);
            case STRING -> input.trim().isEmpty() ? null : input.trim();
            case STRING_LIST -> parseList(input);
            case MATERIAL_LIST -> parseMaterialList(input);
            case ENTITY_TYPE_LIST -> parseEntityTypeList(input);
        };
    }

    /**
     * Returns a human-readable description of what input is expected for a given flag type.
     * Useful for error messages when parsing fails.
     *
     * @param type the flag type
     * @return a description of expected input format
     */
    @NotNull
    public static String getExpectedFormat(@NotNull FlagType type) {
        return switch (type) {
            case BOOLEAN -> "true/false, yes/no, on/off, 1/0, enable/disable";
            case INTEGER -> "a whole number (e.g., 42, -10)";
            case DOUBLE -> "a decimal number (e.g., 1.5, -3.14)";
            case STRING -> "any text value";
            case STRING_LIST -> "comma-separated values (e.g., value1, value2)";
            case MATERIAL_LIST -> "comma-separated material names (e.g., STONE, DIRT, OAK_LOG)";
            case ENTITY_TYPE_LIST -> "comma-separated entity types (e.g., ZOMBIE, SKELETON, CREEPER)";
        };
    }
}
