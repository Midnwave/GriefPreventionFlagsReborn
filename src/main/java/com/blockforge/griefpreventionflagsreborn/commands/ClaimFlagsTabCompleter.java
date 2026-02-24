package com.blockforge.griefpreventionflagsreborn.commands;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.AbstractFlag;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tab completer for the {@code /claimflags} command (aliases: {@code /cf}, {@code /gpfr}).
 * <p>
 * Provides intelligent, context-aware suggestions filtered by:
 * <ul>
 *   <li>The current subcommand and argument position</li>
 *   <li>The sender's permissions</li>
 *   <li>The flag's type (boolean, material list, entity type list, etc.)</li>
 *   <li>What the player has typed so far (case-insensitive prefix matching)</li>
 * </ul>
 */
public class ClaimFlagsTabCompleter implements TabCompleter {

    /** Maximum number of Material suggestions to prevent flooding the tab list. */
    private static final int MAX_MATERIAL_SUGGESTIONS = 50;

    private final GriefPreventionFlagsPlugin plugin;

    /**
     * Constructs a new ClaimFlagsTabCompleter.
     *
     * @param plugin the owning plugin instance
     */
    public ClaimFlagsTabCompleter(@NotNull GriefPreventionFlagsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartsWith(getSubcommandSuggestions(sender), args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        return switch (subcommand) {
            case "set" -> completeSet(sender, args);
            case "unset" -> completeUnset(sender, args);
            case "add" -> completeAdd(sender, args);
            case "remove" -> completeRemove(sender, args);
            case "list" -> completeList(args);
            case "schedule" -> completeSchedule(sender, args);
            case "server" -> completeServer(sender, args);
            case "world" -> completeWorld(sender, args);
            case "admin" -> completeAdmin(sender, args);
            default -> Collections.emptyList();
        };
    }

    // -------------------------------------------------------------------------
    // Top-level subcommand suggestions
    // -------------------------------------------------------------------------

    /**
     * Returns the list of available subcommands filtered by the sender's permissions.
     *
     * @param sender the command sender
     * @return the list of available subcommand names
     */
    private List<String> getSubcommandSuggestions(@NotNull CommandSender sender) {
        List<String> suggestions = new ArrayList<>();

        if (sender.hasPermission("gpfr.gui")) {
            suggestions.add("info");
        }
        if (sender.hasPermission("gpfr.use")) {
            suggestions.add("set");
            suggestions.add("unset");
            suggestions.add("add");
            suggestions.add("remove");
            suggestions.add("list");
        }
        if (sender.hasPermission("gpfr.admin")) {
            suggestions.add("schedule");
            suggestions.add("admin");
        }
        if (sender.hasPermission("gpfr.scope.server")) {
            suggestions.add("server");
        }
        if (sender.hasPermission("gpfr.scope.world")) {
            suggestions.add("world");
        }
        if (sender.hasPermission("gpfr.admin.reload")) {
            suggestions.add("reload");
        }

        return suggestions;
    }

    // -------------------------------------------------------------------------
    // SET completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code set} subcommand.
     * <pre>
     * /claimflags set [flag] [value...]
     *                  ^(2)    ^(3+)
     * </pre>
     */
    private List<String> completeSet(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            // Suggest all flag IDs
            return filterStartsWith(getAllFlagIds(), args[1]);
        }
        if (args.length >= 3) {
            // Suggest values based on flag type
            String flagId = args[1].toLowerCase(Locale.ROOT);
            return suggestFlagValues(flagId, args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // UNSET completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code unset} subcommand by suggesting flag IDs that are
     * currently set in the player's current claim.
     * <pre>
     * /claimflags unset [flag]
     *                    ^(2)
     * </pre>
     */
    private List<String> completeUnset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            List<String> setFlags = getSetFlagIdsInCurrentClaim(sender);
            if (setFlags.isEmpty()) {
                // Fall back to all flag IDs if we cannot determine what is set
                return filterStartsWith(getAllFlagIds(), args[1]);
            }
            return filterStartsWith(setFlags, args[1]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // ADD completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code add} subcommand.
     * <pre>
     * /claimflags add [flag] [value]
     *                  ^(2)   ^(3)
     * </pre>
     * Only suggests list-type flag IDs for the flag argument.
     */
    private List<String> completeAdd(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            return filterStartsWith(getListTypeFlagIds(), args[1]);
        }
        if (args.length == 3) {
            String flagId = args[1].toLowerCase(Locale.ROOT);
            return suggestFlagValues(flagId, args[2]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // REMOVE completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code remove} subcommand.
     * <pre>
     * /claimflags remove [flag] [value]
     *                     ^(2)   ^(3)
     * </pre>
     * Only suggests list-type flag IDs for the flag argument.
     */
    private List<String> completeRemove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            return filterStartsWith(getListTypeFlagIds(), args[1]);
        }
        if (args.length == 3) {
            String flagId = args[1].toLowerCase(Locale.ROOT);
            return suggestFlagValues(flagId, args[2]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // LIST completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code list} subcommand by suggesting category names.
     * <pre>
     * /claimflags list [category]
     *                   ^(2)
     * </pre>
     */
    private List<String> completeList(@NotNull String[] args) {
        if (args.length == 2) {
            return filterStartsWith(getCategoryNames(), args[1]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // SCHEDULE completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code schedule} subcommand.
     * <pre>
     * /claimflags schedule [flag] [cron] [value]
     *                       ^(2)   ^(3)   ^(4)
     * </pre>
     */
    private List<String> completeSchedule(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            return filterStartsWith(getAllFlagIds(), args[1]);
        }
        if (args.length == 3) {
            // Suggest common cron patterns as hints
            return filterStartsWith(List.of(
                    "0_0_*_*_*", "0_*/6_*_*_*", "0_8_*_*_*", "0_20_*_*_*"
            ), args[2]);
        }
        if (args.length >= 4) {
            String flagId = args[1].toLowerCase(Locale.ROOT);
            return suggestFlagValues(flagId, args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // SERVER completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code server} subcommand tree.
     * <pre>
     * /claimflags server [set|unset|list] [flag] [value...]
     *                     ^(2)             ^(3)   ^(4+)
     * </pre>
     */
    private List<String> completeServer(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            return filterStartsWith(List.of("set", "unset", "list"), args[1]);
        }

        if (args.length < 3) {
            return Collections.emptyList();
        }

        String action = args[1].toLowerCase(Locale.ROOT);

        return switch (action) {
            case "set" -> {
                if (args.length == 3) {
                    yield filterStartsWith(getAllFlagIds(), args[2]);
                }
                if (args.length >= 4) {
                    String flagId = args[2].toLowerCase(Locale.ROOT);
                    yield suggestFlagValues(flagId, args[args.length - 1]);
                }
                yield Collections.emptyList();
            }
            case "unset" -> {
                if (args.length == 3) {
                    yield filterStartsWith(getSetFlagIdsForScope(FlagScope.SERVER, "server"), args[2]);
                }
                yield Collections.emptyList();
            }
            case "list" -> {
                if (args.length == 3) {
                    yield filterStartsWith(getCategoryNames(), args[2]);
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    // -------------------------------------------------------------------------
    // WORLD completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code world} subcommand tree.
     * <pre>
     * /claimflags world [worldName|set|unset|list] ...
     *                    ^(2)
     * </pre>
     * If args[1] is a recognized action, assumes the player's current world.
     * Otherwise, treats args[1] as a world name and expects the action at args[2].
     */
    private List<String> completeWorld(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            // Suggest both world names and actions
            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(getWorldNames());
            suggestions.add("set");
            suggestions.add("unset");
            suggestions.add("list");
            return filterStartsWith(suggestions, args[1]);
        }

        // Determine if args[1] is an action or a world name
        String second = args[1].toLowerCase(Locale.ROOT);
        boolean isAction = second.equals("set") || second.equals("unset") || second.equals("list");

        if (isAction) {
            // /claimflags world <action> [flag] [value...]
            return completeWorldAction(sender, second, args, 2);
        } else {
            // /claimflags world <worldName> [action] [flag] [value...]
            if (args.length == 3) {
                return filterStartsWith(List.of("set", "unset", "list"), args[2]);
            }
            if (args.length >= 4) {
                String action = args[2].toLowerCase(Locale.ROOT);
                return completeWorldAction(sender, action, args, 3);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Completes arguments for a world action (set, unset, list) starting at the given offset.
     *
     * @param sender     the command sender
     * @param action     the action ("set", "unset", or "list")
     * @param args       the full command arguments
     * @param flagOffset the index where the flag argument starts
     * @return the completion suggestions
     */
    private List<String> completeWorldAction(@NotNull CommandSender sender, @NotNull String action,
                                             @NotNull String[] args, int flagOffset) {
        return switch (action) {
            case "set" -> {
                if (args.length == flagOffset + 1) {
                    yield filterStartsWith(getAllFlagIds(), args[flagOffset]);
                }
                if (args.length >= flagOffset + 2) {
                    String flagId = args[flagOffset].toLowerCase(Locale.ROOT);
                    yield suggestFlagValues(flagId, args[args.length - 1]);
                }
                yield Collections.emptyList();
            }
            case "unset" -> {
                if (args.length == flagOffset + 1) {
                    // Try to determine world name for scoped unset suggestions
                    String worldName = resolveWorldNameFromArgs(sender, args);
                    if (worldName != null) {
                        yield filterStartsWith(getSetFlagIdsForScope(FlagScope.WORLD, worldName), args[flagOffset]);
                    }
                    yield filterStartsWith(getAllFlagIds(), args[flagOffset]);
                }
                yield Collections.emptyList();
            }
            case "list" -> {
                if (args.length == flagOffset + 1) {
                    yield filterStartsWith(getCategoryNames(), args[flagOffset]);
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    /**
     * Resolves the world name from the command arguments for world-scoped tab completion.
     *
     * @param sender the command sender
     * @param args   the full command arguments
     * @return the world name, or null if it cannot be determined
     */
    @Nullable
    private String resolveWorldNameFromArgs(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length >= 2) {
            String second = args[1].toLowerCase(Locale.ROOT);
            boolean isAction = second.equals("set") || second.equals("unset") || second.equals("list");
            if (!isAction) {
                // args[1] is the world name
                return args[1];
            }
        }
        // Fall back to sender's world if player
        if (sender instanceof Player player) {
            return player.getWorld().getName();
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // ADMIN completion
    // -------------------------------------------------------------------------

    /**
     * Completes the {@code admin} subcommand tree.
     * <pre>
     * /claimflags admin [reset|debug|reload] ...
     *                    ^(2)
     * </pre>
     */
    private List<String> completeAdmin(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            List<String> adminActions = new ArrayList<>();
            if (sender.hasPermission("gpfr.admin.reset")) {
                adminActions.add("reset");
            }
            if (sender.hasPermission("gpfr.admin.debug")) {
                adminActions.add("debug");
            }
            if (sender.hasPermission("gpfr.admin.reload")) {
                adminActions.add("reload");
            }
            return filterStartsWith(adminActions, args[1]);
        }

        if (args.length < 3) {
            return Collections.emptyList();
        }

        String adminAction = args[1].toLowerCase(Locale.ROOT);

        return switch (adminAction) {
            case "reset" -> completeAdminReset(args);
            case "debug" -> completeAdminDebug(args);
            default -> Collections.emptyList();
        };
    }

    /**
     * Completes the {@code admin reset} subcommand.
     * <pre>
     * /claimflags admin reset [scope] [scopeId]
     *                          ^(3)    ^(4)
     * </pre>
     */
    private List<String> completeAdminReset(@NotNull String[] args) {
        if (args.length == 3) {
            return filterStartsWith(List.of("server", "world", "claim", "subclaim"), args[2]);
        }
        if (args.length == 4) {
            String scopeInput = args[2].toLowerCase(Locale.ROOT);
            return switch (scopeInput) {
                case "world" -> filterStartsWith(getWorldNames(), args[3]);
                case "server" -> filterStartsWith(List.of("server"), args[3]);
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    /**
     * Completes the {@code admin debug} subcommand by suggesting flag IDs.
     * <pre>
     * /claimflags admin debug [flag]
     *                          ^(3)
     * </pre>
     */
    private List<String> completeAdminDebug(@NotNull String[] args) {
        if (args.length == 3) {
            return filterStartsWith(getAllFlagIds(), args[2]);
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Flag value suggestions
    // -------------------------------------------------------------------------

    /**
     * Suggests values appropriate for the given flag's type.
     *
     * @param flagId  the flag identifier
     * @param partial what the player has typed so far for this argument
     * @return a list of matching suggestions
     */
    private List<String> suggestFlagValues(@NotNull String flagId, @NotNull String partial) {
        FlagRegistry registry = getRegistry();
        AbstractFlag<?> flag = registry.getFlag(flagId);

        if (flag == null) {
            return Collections.emptyList();
        }

        FlagDefinition definition = flag.getDefinition();
        FlagType type = definition.getType();

        return switch (type) {
            case BOOLEAN -> filterStartsWith(List.of("true", "false"), partial);
            case INTEGER -> filterStartsWith(List.of("0", "1", "5", "10"), partial);
            case DOUBLE -> filterStartsWith(List.of("0.0", "0.5", "1.0", "1.5"), partial);
            case STRING -> {
                Set<String> allowedValues = definition.getAllowedEnumValues();
                if (allowedValues != null && !allowedValues.isEmpty()) {
                    yield filterStartsWith(new ArrayList<>(allowedValues), partial);
                }
                yield Collections.emptyList();
            }
            case STRING_LIST -> {
                Set<String> allowedValues = definition.getAllowedEnumValues();
                if (allowedValues != null && !allowedValues.isEmpty()) {
                    yield filterStartsWith(new ArrayList<>(allowedValues), partial);
                }
                yield Collections.emptyList();
            }
            case MATERIAL_LIST -> suggestMaterials(partial);
            case ENTITY_TYPE_LIST -> suggestEntityTypes(partial);
        };
    }

    /**
     * Suggests Material names matching the partial input.
     * Results are limited to {@value #MAX_MATERIAL_SUGGESTIONS} to prevent tab-list flooding.
     *
     * @param partial the partial input
     * @return matching Material names (limited)
     */
    private List<String> suggestMaterials(@NotNull String partial) {
        String upperPartial = partial.toUpperCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().startsWith(upperPartial)) {
                matches.add(material.name());
                if (matches.size() >= MAX_MATERIAL_SUGGESTIONS) {
                    break;
                }
            }
        }

        return matches;
    }

    /**
     * Suggests EntityType names matching the partial input.
     *
     * @param partial the partial input
     * @return matching EntityType names
     */
    private List<String> suggestEntityTypes(@NotNull String partial) {
        String upperPartial = partial.toUpperCase(Locale.ROOT);
        return Arrays.stream(EntityType.values())
                .map(EntityType::name)
                .filter(name -> name.startsWith(upperPartial))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Flag ID helpers
    // -------------------------------------------------------------------------

    /**
     * Returns all registered flag IDs.
     *
     * @return a list of all flag IDs
     */
    private List<String> getAllFlagIds() {
        try {
            return getRegistry().getAllFlags().stream()
                    .map(AbstractFlag::getId)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns only the IDs of list-type flags (STRING_LIST, MATERIAL_LIST, ENTITY_TYPE_LIST).
     *
     * @return a list of list-type flag IDs
     */
    private List<String> getListTypeFlagIds() {
        try {
            return getRegistry().getAllFlags().stream()
                    .filter(flag -> {
                        FlagType type = flag.getDefinition().getType();
                        return type == FlagType.STRING_LIST
                                || type == FlagType.MATERIAL_LIST
                                || type == FlagType.ENTITY_TYPE_LIST;
                    })
                    .map(AbstractFlag::getId)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the flag IDs that are currently set in the player's current claim.
     * Falls back to an empty list if the sender is not a player or is not in a claim.
     *
     * @param sender the command sender
     * @return a list of flag IDs set in the current claim
     */
    private List<String> getSetFlagIdsInCurrentClaim(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        try {
            GriefPreventionHook gpHook = getGpHook();
            String claimId = gpHook.getClaimId(player.getLocation());
            if (claimId == null) {
                return Collections.emptyList();
            }

            return getSetFlagIdsForScope(FlagScope.CLAIM, claimId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the flag IDs that are set at a specific scope.
     *
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @return a list of flag IDs set at the given scope
     */
    private List<String> getSetFlagIdsForScope(@NotNull FlagScope scope, @NotNull String scopeId) {
        try {
            FlagManager manager = getFlagManager();
            Map<String, Object> flags = manager.getAllFlagsForScope(scope, scopeId);
            return new ArrayList<>(flags.keySet());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // -------------------------------------------------------------------------
    // Category and world name helpers
    // -------------------------------------------------------------------------

    /**
     * Returns all category names suitable for tab completion (both enum names and display names).
     *
     * @return a list of category name strings
     */
    private List<String> getCategoryNames() {
        List<String> names = new ArrayList<>();
        for (FlagCategory category : FlagCategory.values()) {
            names.add(category.name().toLowerCase(Locale.ROOT));
            // Also add display name if it differs from enum name and has no spaces
            String displayName = category.getDisplayName();
            if (!displayName.contains(" ")) {
                names.add(displayName.toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    /**
     * Returns a list of all loaded world names.
     *
     * @return the world names
     */
    private List<String> getWorldNames() {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Filter utility
    // -------------------------------------------------------------------------

    /**
     * Filters a list of suggestions to only those that start with the given input
     * (case-insensitive).
     *
     * @param suggestions the full list of possible suggestions
     * @param input       the partial input typed by the player
     * @return a filtered list of matching suggestions
     */
    private List<String> filterStartsWith(@NotNull List<String> suggestions, @NotNull String input) {
        if (input.isEmpty()) {
            return new ArrayList<>(suggestions);
        }
        String lowerInput = input.toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lowerInput))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Plugin manager accessors
    // -------------------------------------------------------------------------

    private FlagRegistry getRegistry() {
        return plugin.getFlagRegistry();
    }

    private FlagManager getFlagManager() {
        return plugin.getFlagManager();
    }

    private GriefPreventionHook getGpHook() {
        return plugin.getGpHook();
    }
}
