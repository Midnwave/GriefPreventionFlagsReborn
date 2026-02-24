package com.blockforge.griefpreventionflagsreborn.commands;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import com.blockforge.griefpreventionflagsreborn.config.ConfigManager;
import com.blockforge.griefpreventionflagsreborn.config.MessagesManager;
import com.blockforge.griefpreventionflagsreborn.flags.AbstractFlag;
import com.blockforge.griefpreventionflagsreborn.flags.EntityTypeListFlag;
import com.blockforge.griefpreventionflagsreborn.flags.ListFlag;
import com.blockforge.griefpreventionflagsreborn.flags.MaterialListFlag;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import com.blockforge.griefpreventionflagsreborn.storage.FlagStorageManager;
import com.blockforge.griefpreventionflagsreborn.storage.ScheduleStorageManager;
import com.blockforge.griefpreventionflagsreborn.util.ClaimUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Main command executor for the {@code /claimflags} command (aliases: {@code /cf}, {@code /gpfr}).
 * <p>
 * Routes all subcommands to the appropriate handler methods. Each handler validates
 * permissions, parses arguments, and delegates to the plugin's manager classes for
 * the actual flag operations. All user-facing messages are sourced from
 * {@link MessagesManager} for full localization support.
 *
 * <h3>Command Tree:</h3>
 * <pre>
 * /claimflags (no args)           -> Opens flag GUI for current claim
 * /claimflags info                -> Opens flag GUI for current claim
 * /claimflags set &lt;flag&gt; &lt;value&gt; -> Set flag in current claim
 * /claimflags unset &lt;flag&gt;        -> Remove flag from current claim
 * /claimflags add &lt;flag&gt; &lt;value&gt;  -> Add to list flag in current claim
 * /claimflags remove &lt;flag&gt; &lt;value&gt; -> Remove from list flag in current claim
 * /claimflags list [category]     -> List flags in current claim (chat)
 * /claimflags schedule &lt;flag&gt; &lt;cron&gt; &lt;value&gt; -> Schedule flag changes
 * /claimflags server set|unset|list ...
 * /claimflags world [worldName] set|unset|list ...
 * /claimflags admin reset|debug|reload
 * /claimflags reload              -> Reload configs
 * </pre>
 */
public class ClaimFlagsCommand implements CommandExecutor {

    private final GriefPreventionFlagsPlugin plugin;

    /**
     * Constructs a new ClaimFlagsCommand.
     *
     * @param plugin the owning plugin instance
     */
    public ClaimFlagsCommand(@NotNull GriefPreventionFlagsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // No args or "info" -> open GUI
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            handleGui(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "set" -> handleSet(sender, args);
            case "unset" -> handleUnset(sender, args);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args, FlagScope.CLAIM, null);
            case "schedule" -> handleSchedule(sender, args);
            case "server" -> handleServer(sender, args);
            case "world" -> handleWorld(sender, args);
            case "admin" -> handleAdmin(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                getMessages().sendMessage(sender, "unknown-subcommand",
                        "subcommand", args[0]);
            }
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // GUI
    // -------------------------------------------------------------------------

    /**
     * Opens the flag GUI for the player's current claim.
     * Requires the sender to be a player with the {@code gpfr.gui} permission.
     */
    private void handleGui(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            getMessages().sendMessage(sender, "player-only");
            return;
        }

        if (!player.hasPermission("gpfr.gui")) {
            getMessages().sendMessage(player, "no-permission");
            return;
        }

        GriefPreventionHook gpHook = getGpHook();
        if (!gpHook.isInClaim(player.getLocation())) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        // FlagGUI will be created by another agent
        new com.blockforge.griefpreventionflagsreborn.gui.FlagGUI(plugin, player).open();
    }

    // -------------------------------------------------------------------------
    // SET
    // -------------------------------------------------------------------------

    /**
     * Sets a flag value in the current claim.
     * <p>
     * Usage: {@code /claimflags set <flag> <value...>}
     *
     * @param sender the command sender (must be a player)
     * @param args   the full command arguments
     */
    private void handleSet(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            getMessages().sendMessage(sender, "player-only");
            return;
        }

        if (!player.hasPermission("gpfr.use")) {
            getMessages().sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 3) {
            getMessages().sendMessage(player, "usage-set");
            return;
        }

        String flagId = args[1].toLowerCase(Locale.ROOT);
        String value = joinArgs(args, 2);

        setFlagInClaim(player, flagId, value);
    }

    /**
     * Core logic for setting a flag within a claim scope, performing all validation
     * (claim presence, ownership/trust, flag existence, admin-only check, scope allowed).
     *
     * @param player the player executing the command
     * @param flagId the flag identifier
     * @param value  the raw value string to set
     */
    private void setFlagInClaim(@NotNull Player player, @NotNull String flagId, @NotNull String value) {
        GriefPreventionHook gpHook = getGpHook();

        if (!gpHook.isInClaim(player.getLocation())) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        if (!hasClaimPermission(player, gpHook)) {
            getMessages().sendMessage(player, "no-claim-permission");
            return;
        }

        String claimId = gpHook.getClaimId(player.getLocation());
        if (claimId == null) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        setFlagAtScope(player, flagId, value, FlagScope.CLAIM, claimId);
    }

    /**
     * Shared logic for setting a flag at any scope. Validates the flag exists, checks
     * admin-only restrictions, verifies the scope is allowed, and delegates to the
     * {@link FlagManager}.
     *
     * @param sender  the command sender
     * @param flagId  the flag identifier
     * @param value   the raw value string
     * @param scope   the scope to set at
     * @param scopeId the scope identifier
     */
    private void setFlagAtScope(@NotNull CommandSender sender, @NotNull String flagId,
                                @NotNull String value, @NotNull FlagScope scope,
                                @NotNull String scopeId) {
        FlagRegistry registry = getRegistry();
        AbstractFlag<?> flag = registry.getFlag(flagId);

        if (flag == null) {
            getMessages().sendMessage(sender, "unknown-flag", "flag", flagId);
            return;
        }

        FlagDefinition definition = flag.getDefinition();

        // Admin-only flags require elevated permission
        if (definition.isAdminOnly() && !sender.hasPermission("gpfr.flag.admin." + flagId)) {
            getMessages().sendMessage(sender, "flag-admin-only", "flag", flagId);
            return;
        }

        // Verify the flag supports this scope
        if (!definition.isScopeAllowed(scope)) {
            getMessages().sendMessage(sender, "scope-not-allowed",
                    "flag", flagId, "scope", scope.name());
            return;
        }

        try {
            FlagManager manager = getFlagManager();
            manager.setFlag(flagId, scope, scopeId, value,
                    sender instanceof Player p ? p.getUniqueId() : null);

            String scopeDescription = ClaimUtil.describeScope(scope, scopeId);
            getMessages().sendMessage(sender, "flag-set",
                    "flag", definition.getDisplayName(),
                    "value", value,
                    "scope", scopeDescription);
        } catch (InvalidFlagValueException e) {
            getMessages().sendMessage(sender, "invalid-flag-value",
                    "flag", flagId,
                    "error", e.getMessage(),
                    "expected", com.blockforge.griefpreventionflagsreborn.util.FlagValueParser
                            .getExpectedFormat(definition.getType()));
        }
    }

    // -------------------------------------------------------------------------
    // UNSET
    // -------------------------------------------------------------------------

    /**
     * Removes a flag value from the current claim.
     * <p>
     * Usage: {@code /claimflags unset <flag>}
     *
     * @param sender the command sender (must be a player)
     * @param args   the full command arguments
     */
    private void handleUnset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            getMessages().sendMessage(sender, "player-only");
            return;
        }

        if (!player.hasPermission("gpfr.use")) {
            getMessages().sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 2) {
            getMessages().sendMessage(player, "usage-unset");
            return;
        }

        String flagId = args[1].toLowerCase(Locale.ROOT);
        unsetFlagInClaim(player, flagId);
    }

    /**
     * Core logic for unsetting a flag within a claim scope.
     *
     * @param player the player executing the command
     * @param flagId the flag identifier
     */
    private void unsetFlagInClaim(@NotNull Player player, @NotNull String flagId) {
        GriefPreventionHook gpHook = getGpHook();

        if (!gpHook.isInClaim(player.getLocation())) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        if (!hasClaimPermission(player, gpHook)) {
            getMessages().sendMessage(player, "no-claim-permission");
            return;
        }

        String claimId = gpHook.getClaimId(player.getLocation());
        if (claimId == null) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        unsetFlagAtScope(player, flagId, FlagScope.CLAIM, claimId);
    }

    /**
     * Shared logic for unsetting a flag at any scope.
     *
     * @param sender  the command sender
     * @param flagId  the flag identifier
     * @param scope   the scope to unset at
     * @param scopeId the scope identifier
     */
    private void unsetFlagAtScope(@NotNull CommandSender sender, @NotNull String flagId,
                                  @NotNull FlagScope scope, @NotNull String scopeId) {
        FlagRegistry registry = getRegistry();
        AbstractFlag<?> flag = registry.getFlag(flagId);

        if (flag == null) {
            getMessages().sendMessage(sender, "unknown-flag", "flag", flagId);
            return;
        }

        FlagDefinition definition = flag.getDefinition();

        if (definition.isAdminOnly() && !sender.hasPermission("gpfr.flag.admin." + flagId)) {
            getMessages().sendMessage(sender, "flag-admin-only", "flag", flagId);
            return;
        }

        FlagManager manager = getFlagManager();
        manager.unsetFlag(flagId, scope, scopeId);

        String scopeDescription = ClaimUtil.describeScope(scope, scopeId);
        getMessages().sendMessage(sender, "flag-unset",
                "flag", definition.getDisplayName(),
                "scope", scopeDescription);
    }

    // -------------------------------------------------------------------------
    // ADD (list flags)
    // -------------------------------------------------------------------------

    /**
     * Adds a value to a list-type flag in the current claim.
     * <p>
     * Usage: {@code /claimflags add <flag> <value>}
     *
     * @param sender the command sender (must be a player)
     * @param args   the full command arguments
     */
    private void handleAdd(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            getMessages().sendMessage(sender, "player-only");
            return;
        }

        if (!player.hasPermission("gpfr.use")) {
            getMessages().sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 3) {
            getMessages().sendMessage(player, "usage-add");
            return;
        }

        String flagId = args[1].toLowerCase(Locale.ROOT);
        String valueToAdd = args[2];

        GriefPreventionHook gpHook = getGpHook();
        if (!gpHook.isInClaim(player.getLocation())) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        if (!hasClaimPermission(player, gpHook)) {
            getMessages().sendMessage(player, "no-claim-permission");
            return;
        }

        String claimId = gpHook.getClaimId(player.getLocation());
        if (claimId == null) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        addToListFlag(player, flagId, valueToAdd, FlagScope.CLAIM, claimId);
    }

    /**
     * Adds an entry to a list-type flag at the specified scope.
     *
     * @param sender     the command sender
     * @param flagId     the flag identifier (must be a list-type flag)
     * @param valueToAdd the value to add to the list
     * @param scope      the scope
     * @param scopeId    the scope identifier
     */
    @SuppressWarnings("unchecked")
    private void addToListFlag(@NotNull CommandSender sender, @NotNull String flagId,
                               @NotNull String valueToAdd, @NotNull FlagScope scope,
                               @NotNull String scopeId) {
        FlagRegistry registry = getRegistry();
        AbstractFlag<?> flag = registry.getFlag(flagId);

        if (flag == null) {
            getMessages().sendMessage(sender, "unknown-flag", "flag", flagId);
            return;
        }

        FlagDefinition definition = flag.getDefinition();
        FlagType type = definition.getType();

        // Verify this is a list-type flag
        if (type != FlagType.STRING_LIST && type != FlagType.MATERIAL_LIST && type != FlagType.ENTITY_TYPE_LIST) {
            getMessages().sendMessage(sender, "not-list-flag", "flag", flagId);
            return;
        }

        if (definition.isAdminOnly() && !sender.hasPermission("gpfr.flag.admin." + flagId)) {
            getMessages().sendMessage(sender, "flag-admin-only", "flag", flagId);
            return;
        }

        if (!definition.isScopeAllowed(scope)) {
            getMessages().sendMessage(sender, "scope-not-allowed",
                    "flag", flagId, "scope", scope.name());
            return;
        }

        try {
            FlagManager manager = getFlagManager();

            // Get the current list value (raw, may be null if not set)
            List<String> currentList;
            Object rawValue = manager.getRawFlagValue(flagId, scope, scopeId);
            if (rawValue instanceof List<?>) {
                currentList = new ArrayList<>((List<String>) rawValue);
            } else {
                currentList = new ArrayList<>();
            }

            // Use the flag's addToList method for proper validation
            List<String> updatedList;
            List<String> toAdd = List.of(valueToAdd.toUpperCase(Locale.ROOT));

            if (flag instanceof MaterialListFlag materialListFlag) {
                updatedList = materialListFlag.addToList(currentList, toAdd);
            } else if (flag instanceof EntityTypeListFlag entityTypeListFlag) {
                updatedList = entityTypeListFlag.addToList(currentList, toAdd);
            } else if (flag instanceof ListFlag listFlag) {
                updatedList = listFlag.addToList(currentList, List.of(valueToAdd));
            } else {
                getMessages().sendMessage(sender, "not-list-flag", "flag", flagId);
                return;
            }

            // Serialize the updated list back and set it
            String serialized = String.join(",", updatedList);
            manager.setFlag(flagId, scope, scopeId, serialized,
                    sender instanceof Player p ? p.getUniqueId() : null);

            String scopeDescription = ClaimUtil.describeScope(scope, scopeId);
            getMessages().sendMessage(sender, "flag-add",
                    "flag", definition.getDisplayName(),
                    "value", valueToAdd,
                    "scope", scopeDescription);
        } catch (InvalidFlagValueException e) {
            getMessages().sendMessage(sender, "invalid-flag-value",
                    "flag", flagId,
                    "error", e.getMessage(),
                    "expected", com.blockforge.griefpreventionflagsreborn.util.FlagValueParser
                            .getExpectedFormat(definition.getType()));
        }
    }

    // -------------------------------------------------------------------------
    // REMOVE (list flags)
    // -------------------------------------------------------------------------

    /**
     * Removes a value from a list-type flag in the current claim.
     * <p>
     * Usage: {@code /claimflags remove <flag> <value>}
     *
     * @param sender the command sender (must be a player)
     * @param args   the full command arguments
     */
    private void handleRemove(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            getMessages().sendMessage(sender, "player-only");
            return;
        }

        if (!player.hasPermission("gpfr.use")) {
            getMessages().sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 3) {
            getMessages().sendMessage(player, "usage-remove");
            return;
        }

        String flagId = args[1].toLowerCase(Locale.ROOT);
        String valueToRemove = args[2];

        GriefPreventionHook gpHook = getGpHook();
        if (!gpHook.isInClaim(player.getLocation())) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        if (!hasClaimPermission(player, gpHook)) {
            getMessages().sendMessage(player, "no-claim-permission");
            return;
        }

        String claimId = gpHook.getClaimId(player.getLocation());
        if (claimId == null) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        removeFromListFlag(player, flagId, valueToRemove, FlagScope.CLAIM, claimId);
    }

    /**
     * Removes an entry from a list-type flag at the specified scope.
     *
     * @param sender        the command sender
     * @param flagId        the flag identifier (must be a list-type flag)
     * @param valueToRemove the value to remove from the list
     * @param scope         the scope
     * @param scopeId       the scope identifier
     */
    @SuppressWarnings("unchecked")
    private void removeFromListFlag(@NotNull CommandSender sender, @NotNull String flagId,
                                    @NotNull String valueToRemove, @NotNull FlagScope scope,
                                    @NotNull String scopeId) {
        FlagRegistry registry = getRegistry();
        AbstractFlag<?> flag = registry.getFlag(flagId);

        if (flag == null) {
            getMessages().sendMessage(sender, "unknown-flag", "flag", flagId);
            return;
        }

        FlagDefinition definition = flag.getDefinition();
        FlagType type = definition.getType();

        if (type != FlagType.STRING_LIST && type != FlagType.MATERIAL_LIST && type != FlagType.ENTITY_TYPE_LIST) {
            getMessages().sendMessage(sender, "not-list-flag", "flag", flagId);
            return;
        }

        if (definition.isAdminOnly() && !sender.hasPermission("gpfr.flag.admin." + flagId)) {
            getMessages().sendMessage(sender, "flag-admin-only", "flag", flagId);
            return;
        }

        FlagManager manager = getFlagManager();

        List<String> currentList;
        Object rawValue = manager.getRawFlagValue(flagId, scope, scopeId);
        if (rawValue instanceof List<?>) {
            currentList = new ArrayList<>((List<String>) rawValue);
        } else {
            getMessages().sendMessage(sender, "flag-not-set",
                    "flag", definition.getDisplayName(),
                    "scope", ClaimUtil.describeScope(scope, scopeId));
            return;
        }

        List<String> updatedList;
        List<String> toRemove = List.of(valueToRemove.toUpperCase(Locale.ROOT));

        if (flag instanceof MaterialListFlag materialListFlag) {
            updatedList = materialListFlag.removeFromList(currentList, toRemove);
        } else if (flag instanceof EntityTypeListFlag entityTypeListFlag) {
            updatedList = entityTypeListFlag.removeFromList(currentList, toRemove);
        } else if (flag instanceof ListFlag listFlag) {
            updatedList = listFlag.removeFromList(currentList, List.of(valueToRemove));
        } else {
            getMessages().sendMessage(sender, "not-list-flag", "flag", flagId);
            return;
        }

        if (updatedList.isEmpty()) {
            // If the list is now empty, unset the flag entirely
            manager.unsetFlag(flagId, scope, scopeId);
        } else {
            String serialized = String.join(",", updatedList);
            manager.setFlag(flagId, scope, scopeId, serialized,
                    sender instanceof Player p ? p.getUniqueId() : null);
        }

        String scopeDescription = ClaimUtil.describeScope(scope, scopeId);
        getMessages().sendMessage(sender, "flag-remove",
                "flag", definition.getDisplayName(),
                "value", valueToRemove,
                "scope", scopeDescription);
    }

    // -------------------------------------------------------------------------
    // LIST
    // -------------------------------------------------------------------------

    /**
     * Lists all flags set at the given scope. Optionally filtered by category.
     * <p>
     * For claim scope: {@code /claimflags list [category]}
     * The scope and scopeId are determined from the player's location when called
     * for claim scope, or passed explicitly for server/world scopes.
     *
     * @param sender  the command sender
     * @param args    the full command arguments (args[1] is optional category filter)
     * @param scope   the scope to list flags for
     * @param scopeId the scope identifier, or null to resolve from player location
     */
    private void handleList(@NotNull CommandSender sender, @NotNull String[] args,
                            @NotNull FlagScope scope, String scopeId) {
        // For claim scope, we need a player
        if (scope == FlagScope.CLAIM && !(sender instanceof Player)) {
            getMessages().sendMessage(sender, "player-only");
            return;
        }

        if (!sender.hasPermission("gpfr.use")) {
            getMessages().sendMessage(sender, "no-permission");
            return;
        }

        // Resolve scopeId for claim scope
        if (scope == FlagScope.CLAIM) {
            Player player = (Player) sender;
            GriefPreventionHook gpHook = getGpHook();
            if (!gpHook.isInClaim(player.getLocation())) {
                getMessages().sendMessage(player, "not-in-claim");
                return;
            }
            scopeId = gpHook.getClaimId(player.getLocation());
            if (scopeId == null) {
                getMessages().sendMessage(player, "not-in-claim");
                return;
            }
        }

        // Determine category filter (last argument after subcommand routing)
        FlagCategory categoryFilter = null;
        // For direct "/claimflags list [category]", category is args[1]
        // For "/claimflags server list [category]", category comes from the caller
        // We determine the category arg index based on the args pattern
        int categoryArgIndex = determineCategoryArgIndex(args);
        if (categoryArgIndex >= 0 && categoryArgIndex < args.length) {
            String categoryInput = args[categoryArgIndex];
            categoryFilter = parseFlagCategory(categoryInput);
            if (categoryFilter == null) {
                getMessages().sendMessage(sender, "unknown-category", "category", categoryInput);
                return;
            }
        }

        FlagManager manager = getFlagManager();
        FlagRegistry registry = getRegistry();
        Map<String, Object> flags = manager.getAllFlagsForScope(scope, scopeId);

        if (flags.isEmpty()) {
            getMessages().sendMessage(sender, "no-flags-set",
                    "scope", ClaimUtil.describeScope(scope, scopeId));
            return;
        }

        String scopeDescription = ClaimUtil.describeScope(scope, scopeId);
        getMessages().sendMessage(sender, "flag-list-header", "scope", scopeDescription);

        for (Map.Entry<String, Object> entry : flags.entrySet()) {
            String flagId = entry.getKey();
            Object value = entry.getValue();

            AbstractFlag<?> flag = registry.getFlag(flagId);
            if (flag == null) {
                continue;
            }

            FlagDefinition definition = flag.getDefinition();

            // Apply category filter
            if (categoryFilter != null && definition.getCategory() != categoryFilter) {
                continue;
            }

            String displayValue = formatDisplayValue(flag, value);
            String categoryColor = definition.getCategory().getColorTag();

            getMessages().sendMessage(sender, "flag-list-entry",
                    "flag", definition.getDisplayName(),
                    "flag_id", flagId,
                    "value", displayValue,
                    "category_color", categoryColor,
                    "category", definition.getCategory().getDisplayName());
        }
    }

    /**
     * Determines the index in the args array where the optional category argument lives,
     * based on the subcommand structure.
     *
     * @param args the full command arguments
     * @return the index of the category argument, or -1 if not present
     */
    private int determineCategoryArgIndex(@NotNull String[] args) {
        if (args.length < 1) {
            return -1;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // /claimflags list [category] -> category at index 1
        if (sub.equals("list")) {
            return args.length >= 2 ? 1 : -1;
        }

        // /claimflags server list [category] -> category at index 2
        if (sub.equals("server") && args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            return args.length >= 3 ? 2 : -1;
        }

        // /claimflags world [worldName] list [category]
        if (sub.equals("world")) {
            // world list [category] -> args: world, list, [category]
            if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                return args.length >= 3 ? 2 : -1;
            }
            // world <worldName> list [category] -> args: world, worldName, list, [category]
            if (args.length >= 3 && args[2].equalsIgnoreCase("list")) {
                return args.length >= 4 ? 3 : -1;
            }
        }

        return -1;
    }

    // -------------------------------------------------------------------------
    // SERVER
    // -------------------------------------------------------------------------

    /**
     * Handles the {@code server} subcommand tree.
     * <p>
     * Usage:
     * <ul>
     *   <li>{@code /claimflags server set <flag> <value...>}</li>
     *   <li>{@code /claimflags server unset <flag>}</li>
     *   <li>{@code /claimflags server list [category]}</li>
     * </ul>
     *
     * @param sender the command sender (player or console)
     * @param args   the full command arguments
     */
    private void handleServer(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("gpfr.scope.server")) {
            getMessages().sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            getMessages().sendMessage(sender, "usage-server");
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);

        switch (action) {
            case "set" -> {
                if (args.length < 4) {
                    getMessages().sendMessage(sender, "usage-server-set");
                    return;
                }
                String flagId = args[2].toLowerCase(Locale.ROOT);
                String value = joinArgs(args, 3);
                setFlagAtScope(sender, flagId, value, FlagScope.SERVER, "server");
            }
            case "unset" -> {
                if (args.length < 3) {
                    getMessages().sendMessage(sender, "usage-server-unset");
                    return;
                }
                String flagId = args[2].toLowerCase(Locale.ROOT);
                unsetFlagAtScope(sender, flagId, FlagScope.SERVER, "server");
            }
            case "list" -> handleList(sender, args, FlagScope.SERVER, "server");
            default -> getMessages().sendMessage(sender, "usage-server");
        }
    }

    // -------------------------------------------------------------------------
    // WORLD
    // -------------------------------------------------------------------------

    /**
     * Handles the {@code world} subcommand tree.
     * <p>
     * Usage:
     * <ul>
     *   <li>{@code /claimflags world set <flag> <value...>} (uses player's world)</li>
     *   <li>{@code /claimflags world <worldName> set <flag> <value...>}</li>
     *   <li>{@code /claimflags world unset <flag>} (uses player's world)</li>
     *   <li>{@code /claimflags world <worldName> unset <flag>}</li>
     *   <li>{@code /claimflags world list [category]} (uses player's world)</li>
     *   <li>{@code /claimflags world <worldName> list [category]}</li>
     * </ul>
     *
     * @param sender the command sender (player or console)
     * @param args   the full command arguments
     */
    private void handleWorld(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("gpfr.scope.world")) {
            getMessages().sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            getMessages().sendMessage(sender, "usage-world");
            return;
        }

        // Determine if args[1] is an action or a world name
        String potentialAction = args[1].toLowerCase(Locale.ROOT);
        boolean isDirectAction = potentialAction.equals("set")
                || potentialAction.equals("unset")
                || potentialAction.equals("list");

        String worldName;
        int actionIndex;

        if (isDirectAction) {
            // No world name specified, use player's world
            if (!(sender instanceof Player player)) {
                getMessages().sendMessage(sender, "world-required-console");
                return;
            }
            worldName = player.getWorld().getName();
            actionIndex = 1;
        } else {
            // args[1] is the world name
            worldName = args[1];
            if (args.length < 3) {
                getMessages().sendMessage(sender, "usage-world");
                return;
            }
            actionIndex = 2;
        }

        // Validate world exists
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getMessages().sendMessage(sender, "unknown-world", "world", worldName);
            return;
        }

        String action = args[actionIndex].toLowerCase(Locale.ROOT);

        switch (action) {
            case "set" -> {
                if (args.length < actionIndex + 3) {
                    getMessages().sendMessage(sender, "usage-world-set");
                    return;
                }
                String flagId = args[actionIndex + 1].toLowerCase(Locale.ROOT);
                String value = joinArgs(args, actionIndex + 2);
                setFlagAtScope(sender, flagId, value, FlagScope.WORLD, worldName);
            }
            case "unset" -> {
                if (args.length < actionIndex + 2) {
                    getMessages().sendMessage(sender, "usage-world-unset");
                    return;
                }
                String flagId = args[actionIndex + 1].toLowerCase(Locale.ROOT);
                unsetFlagAtScope(sender, flagId, FlagScope.WORLD, worldName);
            }
            case "list" -> handleList(sender, args, FlagScope.WORLD, worldName);
            default -> getMessages().sendMessage(sender, "usage-world");
        }
    }

    // -------------------------------------------------------------------------
    // SCHEDULE
    // -------------------------------------------------------------------------

    /**
     * Schedules a flag value change on a cron expression.
     * <p>
     * Usage: {@code /claimflags schedule <flag> <cron> <value>}
     * <p>
     * The cron expression must be quoted or a single token. The schedule is stored
     * for the player's current claim.
     *
     * @param sender the command sender (must be a player)
     * @param args   the full command arguments
     */
    private void handleSchedule(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            getMessages().sendMessage(sender, "player-only");
            return;
        }

        if (!player.hasPermission("gpfr.admin")) {
            getMessages().sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 4) {
            getMessages().sendMessage(player, "usage-schedule");
            return;
        }

        String flagId = args[1].toLowerCase(Locale.ROOT);
        String cronExpr = args[2];
        String value = joinArgs(args, 3);

        FlagRegistry registry = getRegistry();
        AbstractFlag<?> flag = registry.getFlag(flagId);

        if (flag == null) {
            getMessages().sendMessage(player, "unknown-flag", "flag", flagId);
            return;
        }

        GriefPreventionHook gpHook = getGpHook();
        if (!gpHook.isInClaim(player.getLocation())) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        String claimId = gpHook.getClaimId(player.getLocation());
        if (claimId == null) {
            getMessages().sendMessage(player, "not-in-claim");
            return;
        }

        ScheduleStorageManager scheduleStorage = getScheduleStorageManager();
        int scheduleId = scheduleStorage.addSchedule(
                flagId, FlagScope.CLAIM, claimId, cronExpr, value, player.getUniqueId());

        if (scheduleId == -1) {
            getMessages().sendMessage(player, "schedule-failed", "flag", flagId);
            return;
        }

        getMessages().sendMessage(player, "schedule-created",
                "id", String.valueOf(scheduleId),
                "flag", flag.getDefinition().getDisplayName(),
                "cron", cronExpr,
                "value", value,
                "scope", ClaimUtil.describeScope(FlagScope.CLAIM, claimId));
    }

    // -------------------------------------------------------------------------
    // ADMIN
    // -------------------------------------------------------------------------

    /**
     * Routes admin subcommands.
     *
     * @param sender the command sender
     * @param args   the full command arguments
     */
    private void handleAdmin(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            getMessages().sendMessage(sender, "usage-admin");
            return;
        }

        String adminAction = args[1].toLowerCase(Locale.ROOT);

        switch (adminAction) {
            case "reset" -> handleAdminReset(sender, args);
            case "debug" -> handleAdminDebug(sender, args);
            case "reload" -> handleReload(sender);
            default -> getMessages().sendMessage(sender, "usage-admin");
        }
    }

    /**
     * Resets (clears) all flags for a given scope.
     * <p>
     * Usage: {@code /claimflags admin reset <scope> [scopeId]}
     * <p>
     * Scope can be "server", "world", or "claim". For "world" and "claim",
     * a scopeId must be provided.
     *
     * @param sender the command sender
     * @param args   the full command arguments
     */
    private void handleAdminReset(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("gpfr.admin.reset")) {
            getMessages().sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 3) {
            getMessages().sendMessage(sender, "usage-admin-reset");
            return;
        }

        String scopeInput = args[2].toUpperCase(Locale.ROOT);
        FlagScope scope;
        try {
            scope = FlagScope.valueOf(scopeInput);
        } catch (IllegalArgumentException e) {
            getMessages().sendMessage(sender, "unknown-scope", "scope", args[2]);
            return;
        }

        String scopeId;
        if (scope == FlagScope.SERVER) {
            scopeId = "server";
        } else {
            if (args.length < 4) {
                getMessages().sendMessage(sender, "usage-admin-reset-scopeid");
                return;
            }
            scopeId = args[3];
        }

        FlagStorageManager storage = getFlagStorageManager();
        storage.clearScope(scope, scopeId);

        // Also invalidate the cache for this scope
        getFlagManager().invalidateCache(scopeId);

        String scopeDescription = ClaimUtil.describeScope(scope, scopeId);
        getMessages().sendMessage(sender, "admin-reset-success", "scope", scopeDescription);
    }

    /**
     * Shows the full inheritance resolution chain for a flag at the player's current location.
     * Displays the value at each scope level (subclaim, claim, world, server, default)
     * and indicates which value is the final resolved result.
     * <p>
     * Usage: {@code /claimflags admin debug <flag> [location]}
     *
     * @param sender the command sender (must be a player)
     * @param args   the full command arguments
     */
    private void handleAdminDebug(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            getMessages().sendMessage(sender, "player-only");
            return;
        }

        if (!player.hasPermission("gpfr.admin.debug")) {
            getMessages().sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 3) {
            getMessages().sendMessage(player, "usage-admin-debug");
            return;
        }

        String flagId = args[2].toLowerCase(Locale.ROOT);

        FlagRegistry registry = getRegistry();
        AbstractFlag<?> flag = registry.getFlag(flagId);

        if (flag == null) {
            getMessages().sendMessage(player, "unknown-flag", "flag", flagId);
            return;
        }

        FlagDefinition definition = flag.getDefinition();
        FlagManager manager = getFlagManager();
        GriefPreventionHook gpHook = getGpHook();

        getMessages().sendMessage(player, "debug-header",
                "flag", definition.getDisplayName(),
                "flag_id", flagId);

        // SUBCLAIM value
        String subclaimId = gpHook.getSubclaimId(player.getLocation());
        if (subclaimId != null) {
            Object subclaimValue = manager.getRawFlagValue(flagId, FlagScope.SUBCLAIM, subclaimId);
            String display = subclaimValue != null ? formatDisplayValue(flag, subclaimValue) : "<gray>not set</gray>";
            getMessages().sendMessage(player, "debug-scope-entry",
                    "scope", "Subclaim #" + subclaimId,
                    "value", display);
        } else {
            getMessages().sendMessage(player, "debug-scope-entry",
                    "scope", "Subclaim",
                    "value", "<gray>N/A (not in subclaim)</gray>");
        }

        // CLAIM value
        String claimId = gpHook.getClaimId(player.getLocation());
        if (claimId != null) {
            Object claimValue = manager.getRawFlagValue(flagId, FlagScope.CLAIM, claimId);
            String display = claimValue != null ? formatDisplayValue(flag, claimValue) : "<gray>not set</gray>";
            getMessages().sendMessage(player, "debug-scope-entry",
                    "scope", "Claim #" + claimId,
                    "value", display);
        } else {
            getMessages().sendMessage(player, "debug-scope-entry",
                    "scope", "Claim",
                    "value", "<gray>N/A (not in claim)</gray>");
        }

        // WORLD value
        String worldName = player.getWorld().getName();
        Object worldValue = manager.getRawFlagValue(flagId, FlagScope.WORLD, worldName);
        String worldDisplay = worldValue != null ? formatDisplayValue(flag, worldValue) : "<gray>not set</gray>";
        getMessages().sendMessage(player, "debug-scope-entry",
                "scope", "World: " + worldName,
                "value", worldDisplay);

        // SERVER value
        Object serverValue = manager.getRawFlagValue(flagId, FlagScope.SERVER, "server");
        String serverDisplay = serverValue != null ? formatDisplayValue(flag, serverValue) : "<gray>not set</gray>";
        getMessages().sendMessage(player, "debug-scope-entry",
                "scope", "Server",
                "value", serverDisplay);

        // DEFAULT value
        Object defaultValue = definition.getDefaultValue();
        String defaultDisplay = defaultValue != null ? formatDisplayValue(flag, defaultValue) : "<gray>none</gray>";
        getMessages().sendMessage(player, "debug-scope-entry",
                "scope", "Default",
                "value", defaultDisplay);

        // Final resolved value
        Object resolvedValue = manager.getFlagValue(flagId, player.getLocation());
        String resolvedDisplay = resolvedValue != null ? formatDisplayValue(flag, resolvedValue) : "<gray>none</gray>";
        getMessages().sendMessage(player, "debug-resolved",
                "value", resolvedDisplay);
    }

    // -------------------------------------------------------------------------
    // RELOAD
    // -------------------------------------------------------------------------

    /**
     * Reloads the plugin configuration and messages files, and clears the flag value cache.
     * <p>
     * Usage: {@code /claimflags reload} or {@code /claimflags admin reload}
     *
     * @param sender the command sender (player or console)
     */
    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("gpfr.admin.reload")) {
            getMessages().sendMessage(sender, "no-permission");
            return;
        }

        ConfigManager configManager = getConfigManager();
        configManager.loadConfig();

        MessagesManager messagesManager = getMessages();
        messagesManager.loadMessages();

        getFlagManager().clearCache();

        messagesManager.sendMessage(sender, "reload-success");
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    /**
     * Checks whether a player has permission to modify flags in the claim at their location.
     * A player must be either the claim owner or have manager (permission) trust.
     *
     * @param player the player to check
     * @param gpHook the GriefPrevention hook
     * @return true if the player can modify flags in the claim
     */
    private boolean hasClaimPermission(@NotNull Player player, @NotNull GriefPreventionHook gpHook) {
        // Admins bypass claim ownership checks
        if (player.hasPermission("gpfr.admin")) {
            return true;
        }
        return gpHook.isClaimOwner(player.getLocation(), player.getUniqueId())
                || gpHook.hasClaimTrust(player.getLocation(), player.getUniqueId(), "PERMISSION");
    }

    /**
     * Formats a flag value for display using the flag's {@link AbstractFlag#getDisplayValue(Object)}
     * method, with type-safe casting.
     *
     * @param flag  the flag definition
     * @param value the raw value
     * @return the formatted display string
     */
    @SuppressWarnings("unchecked")
    private String formatDisplayValue(@NotNull AbstractFlag<?> flag, @NotNull Object value) {
        try {
            return ((AbstractFlag<Object>) flag).getDisplayValue(value);
        } catch (ClassCastException e) {
            return String.valueOf(value);
        }
    }

    /**
     * Joins command arguments starting from the given index with spaces.
     *
     * @param args      the arguments array
     * @param fromIndex the starting index (inclusive)
     * @return the joined string
     */
    private String joinArgs(@NotNull String[] args, int fromIndex) {
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = fromIndex; i < args.length; i++) {
            joiner.add(args[i]);
        }
        return joiner.toString();
    }

    /**
     * Parses a string input into a {@link FlagCategory} by matching against both
     * the enum name and the display name (case-insensitive).
     *
     * @param input the category name input
     * @return the matching category, or null if no match
     */
    private FlagCategory parseFlagCategory(@NotNull String input) {
        for (FlagCategory category : FlagCategory.values()) {
            if (category.name().equalsIgnoreCase(input)
                    || category.getDisplayName().equalsIgnoreCase(input)) {
                return category;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Plugin manager accessors
    // -------------------------------------------------------------------------

    private MessagesManager getMessages() {
        return plugin.getMessagesManager();
    }

    private FlagRegistry getRegistry() {
        return plugin.getFlagRegistry();
    }

    private FlagManager getFlagManager() {
        return plugin.getFlagManager();
    }

    private GriefPreventionHook getGpHook() {
        return plugin.getGpHook();
    }

    private ConfigManager getConfigManager() {
        return plugin.getConfigManager();
    }

    private FlagStorageManager getFlagStorageManager() {
        return plugin.getFlagStorageManager();
    }

    private ScheduleStorageManager getScheduleStorageManager() {
        return plugin.getScheduleStorageManager();
    }
}
