package com.blockforge.griefpreventionflagsreborn.gui;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.api.FlagValue;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import com.blockforge.griefpreventionflagsreborn.flags.AbstractFlag;
import com.blockforge.griefpreventionflagsreborn.flags.BooleanFlag;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import com.blockforge.griefpreventionflagsreborn.util.AdventureCompat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Main GUI for browsing and managing flags in a 6-row (54-slot) chest inventory.
 * <p>
 * The GUI is organized into three visual sections:
 * <ul>
 *   <li><b>Row 1 (slots 0-8):</b> Category filter tabs. Slot 0 is "All Flags" (NETHER_STAR),
 *       and slots 1-8 map to the first 8 {@link FlagCategory} values. The currently selected
 *       category is highlighted with LIME_STAINED_GLASS_PANE.</li>
 *   <li><b>Rows 2-5 (slots 9-44):</b> Flag item display. Each flag is rendered as colored wool
 *       indicating its type and state, with lore showing description, current value, and
 *       available actions.</li>
 *   <li><b>Row 6 (slots 45-53):</b> Navigation controls including previous/next page arrows,
 *       page indicator, search placeholder, and close button.</li>
 * </ul>
 * <p>
 * Implements {@link InventoryHolder} so that the listener can identify this GUI from the
 * inventory's holder reference.
 */
public class FlagGUI implements InventoryHolder {

    /** Number of flag display slots per page (rows 2-5, all 36 slots). */
    private static final int FLAGS_PER_PAGE = 36;

    /** First slot index for flag items (row 2, slot 0). */
    private static final int FLAG_SLOT_START = 9;

    /** Last slot index for flag items (row 5, slot 8). */
    private static final int FLAG_SLOT_END = 44;

    // Navigation slot constants
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_SEARCH = 47;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int SLOT_CLOSE = 51;
    private static final int SLOT_NEXT_PAGE = 53;

    // Category color mapping for stained glass panes
    private static final ChatColor[] CATEGORY_COLORS = {
            ChatColor.GREEN,         // SAFETY
            ChatColor.RED,           // PVP
            ChatColor.DARK_GREEN,    // MOBS
            ChatColor.DARK_AQUA,     // ENVIRONMENT
            ChatColor.AQUA,          // MOVEMENT
            ChatColor.GOLD,          // COMMANDS
            ChatColor.YELLOW,        // ECONOMY
            ChatColor.DARK_RED,      // TECHNICAL
            ChatColor.BLUE,          // VEHICLE_ENTITY
            ChatColor.LIGHT_PURPLE,  // BLOCK_INTERACTION
            ChatColor.DARK_PURPLE,   // WORLD_MODIFICATION
            ChatColor.WHITE          // CONTENT_121
    };

    private static final Material[] CATEGORY_GLASS_PANES = {
            Material.GREEN_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE,
            Material.WHITE_STAINED_GLASS_PANE
    };

    private final GriefPreventionFlagsPlugin plugin;
    private final Player player;
    private FlagScope editScope;
    private String editScopeId;
    @Nullable
    private FlagCategory currentCategory;
    private int currentPage;
    private List<AbstractFlag<?>> currentFlags;
    private Inventory inventory;

    /**
     * Creates a new FlagGUI for the given player. The GUI will attempt to detect the
     * claim at the player's current location. If no claim is found, the player is notified
     * and the GUI defaults to the SERVER scope.
     *
     * @param plugin the plugin instance
     * @param player the player who will view the GUI
     */
    public FlagGUI(@NotNull GriefPreventionFlagsPlugin plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.currentCategory = null;
        this.currentPage = 0;

        // Determine the claim at the player's location
        GriefPreventionHook gpHook = plugin.getGpHook();
        String claimId = gpHook.getClaimId(player.getLocation());
        if (claimId != null) {
            this.editScope = FlagScope.CLAIM;
            this.editScopeId = claimId;
        } else {
            // No claim found at player location - default to server scope
            this.editScope = FlagScope.SERVER;
            this.editScopeId = "server";
            AdventureCompat.sendMessage(player,
                    "<gray>No claim found at your location. Showing server-level flags.");
        }

        this.currentFlags = getFilteredFlags();
    }

    /**
     * Opens the flag GUI for the player, building the inventory contents and displaying it.
     */
    public void open() {
        inventory = Bukkit.createInventory(this, 54,
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Flag Manager"
                        + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + getScopeDisplayName());
        rebuildInventory();
        player.openInventory(inventory);
    }

    /**
     * Opens the flag GUI targeting a specific scope and scope ID.
     *
     * @param scope   the scope to edit flags at
     * @param scopeId the identifier for the scope
     */
    public void open(@NotNull FlagScope scope, @NotNull String scopeId) {
        this.editScope = scope;
        this.editScopeId = scopeId;
        this.currentFlags = getFilteredFlags();
        this.currentPage = 0;
        open();
    }

    /**
     * Clears and repopulates the inventory based on the current category filter,
     * page index, and flag values.
     */
    public void rebuildInventory() {
        if (inventory == null) {
            return;
        }
        inventory.clear();
        currentFlags = getFilteredFlags();

        buildCategoryTabs();
        buildFlagItems();
        buildNavigationBar();
    }

    /**
     * Returns the filtered and sorted list of flags for the current category.
     * If no category is selected (null), all flags from the registry are returned.
     * Flags are sorted alphabetically by their display name.
     *
     * @return the filtered flag list
     */
    @NotNull
    private List<AbstractFlag<?>> getFilteredFlags() {
        FlagRegistry registry = plugin.getFlagRegistry();
        List<AbstractFlag<?>> flags;

        if (currentCategory == null) {
            flags = new ArrayList<>(registry.getAllFlags());
        } else {
            flags = new ArrayList<>(registry.getFlagsByCategory(currentCategory));
        }

        // Filter out admin-only flags for non-admin players
        if (!player.hasPermission("gpfr.admin")) {
            flags.removeIf(flag -> flag.getDefinition().isAdminOnly());
        }

        // Filter out flags that don't support the current scope
        flags.removeIf(flag -> !flag.getDefinition().isScopeAllowed(editScope));

        flags.sort(Comparator.comparing(f -> f.getDefinition().getDisplayName().toLowerCase()));
        return flags;
    }

    /**
     * Populates the category tab row (slots 0-8). Slot 0 is always "All Flags" with a
     * NETHER_STAR. Slots 1-8 correspond to the first 8 categories from {@link FlagCategory#values()}.
     * The currently selected category is highlighted with LIME_STAINED_GLASS_PANE.
     */
    private void buildCategoryTabs() {
        // Slot 0: "All Flags" tab
        boolean allSelected = (currentCategory == null);
        ItemStack allItem = createItem(
                allSelected ? Material.NETHER_STAR : Material.GRAY_STAINED_GLASS_PANE,
                (allSelected ? ChatColor.GREEN : ChatColor.GRAY) + "All Flags",
                allSelected
                        ? List.of(ChatColor.YELLOW + "Currently selected")
                        : List.of(ChatColor.GRAY + "Click to show all flags")
        );
        inventory.setItem(0, allItem);

        // Slots 1-8: category tabs
        FlagCategory[] categories = FlagCategory.values();
        for (int i = 0; i < 8 && i < categories.length; i++) {
            FlagCategory cat = categories[i];
            boolean selected = (cat == currentCategory);
            ChatColor catColor = getCategoryColor(cat);

            Material paneMaterial;
            if (selected) {
                paneMaterial = Material.LIME_STAINED_GLASS_PANE;
            } else {
                paneMaterial = getCategoryGlassPane(cat);
            }

            List<String> lore = new ArrayList<>();
            FlagRegistry registry = plugin.getFlagRegistry();
            int count = registry.getFlagsByCategory(cat).size();
            lore.add(ChatColor.GRAY + "" + count + " flags");
            if (selected) {
                lore.add(ChatColor.YELLOW + "Currently selected");
            } else {
                lore.add(ChatColor.GRAY + "Click to filter");
            }

            ItemStack tabItem = createItem(paneMaterial, catColor + cat.getDisplayName(), lore);
            inventory.setItem(i + 1, tabItem);
        }

        // If there are more than 8 categories, put remaining in slot 8 as overflow
        if (categories.length > 8) {
            // Slot 8 shows additional categories beyond the first 8
            // We check if the currently selected category is one of the overflow ones
            boolean overflowSelected = false;
            StringBuilder overflowNames = new StringBuilder();
            for (int i = 8; i < categories.length; i++) {
                if (categories[i] == currentCategory) {
                    overflowSelected = true;
                }
                if (overflowNames.length() > 0) {
                    overflowNames.append(", ");
                }
                overflowNames.append(categories[i].getDisplayName());
            }

            // Replace slot 8 with a "More..." item if there are overflow categories
            // and the slot was already set by the loop above for categories[7]
            // Only add overflow indicator if categories.length > 9 (since slots 1-8 handle indices 0-7)
            if (categories.length > 9) {
                List<String> moreLore = new ArrayList<>();
                moreLore.add(ChatColor.GRAY + "More: " + overflowNames);
                if (overflowSelected) {
                    moreLore.add(ChatColor.YELLOW + "Currently selected");
                }
                // Keep slot 8 as the 8th category (index 7) - overflow beyond 9 categories
                // is handled by cycling through with shift-click (future enhancement)
            }
        }
    }

    /**
     * Populates the flag display area (slots 9-44) with flag items for the current page.
     * Each flag is represented by colored wool with appropriate lore.
     */
    private void buildFlagItems() {
        int startIndex = currentPage * FLAGS_PER_PAGE;
        int endIndex = Math.min(startIndex + FLAGS_PER_PAGE, currentFlags.size());

        for (int i = startIndex; i < endIndex; i++) {
            AbstractFlag<?> flag = currentFlags.get(i);
            ItemStack item = buildFlagItem(flag);
            int slot = FLAG_SLOT_START + (i - startIndex);
            inventory.setItem(slot, item);
        }

        // Fill remaining slots with dark gray glass panes as background
        for (int slot = FLAG_SLOT_START + (endIndex - startIndex); slot <= FLAG_SLOT_END; slot++) {
            inventory.setItem(slot, createFillerPane());
        }
    }

    /**
     * Builds an {@link ItemStack} representing a single flag in the GUI.
     * <p>
     * The wool color indicates the flag's state:
     * <ul>
     *   <li>GREEN_WOOL: boolean flag that is currently enabled</li>
     *   <li>RED_WOOL: boolean flag that is currently disabled</li>
     *   <li>YELLOW_WOOL: non-boolean flag that has a value set</li>
     *   <li>GRAY_WOOL: non-boolean flag with no value set</li>
     * </ul>
     *
     * @param flag the flag to create an item for
     * @return the constructed ItemStack
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private ItemStack buildFlagItem(@NotNull AbstractFlag<?> flag) {
        FlagManager flagManager = plugin.getFlagManager();
        String flagId = flag.getId();

        // Get raw value at this scope
        Object rawValue = flagManager.getRawFlagValue(flagId, editScope, editScopeId);

        // Get resolved value (includes inheritance)
        Object resolvedValue = flagManager.getFlagValue(flagId, editScopeId);

        // Determine the source scope for inheritance display
        boolean isInherited = false;
        String inheritedFrom = null;
        if (rawValue == null && resolvedValue != null) {
            // Value is inherited - find from which scope
            isInherited = true;
            inheritedFrom = findInheritedSource(flagId);
        }

        // Determine wool color
        Material woolMaterial;
        if (flag.getType() == FlagType.BOOLEAN) {
            boolean boolValue = resolvedValue != null ? (Boolean) resolvedValue
                    : (flag.getDefinition().getDefaultValue() != null
                    ? (Boolean) flag.getDefinition().getDefaultValue() : false);
            woolMaterial = boolValue ? Material.GREEN_WOOL : Material.RED_WOOL;
        } else {
            woolMaterial = (rawValue != null) ? Material.YELLOW_WOOL : Material.GRAY_WOOL;
        }

        // Build display name with category color
        ChatColor categoryColor = getCategoryColor(flag.getCategory());
        String displayName = categoryColor + "" + ChatColor.BOLD + flag.getDefinition().getDisplayName();

        // Build lore
        List<String> lore = new ArrayList<>();

        // Description
        String description = flag.getDefinition().getDescription();
        if (description != null && !description.isEmpty()) {
            // Word-wrap description at ~40 chars per line
            for (String line : wordWrap(description, 40)) {
                lore.add(ChatColor.GRAY + line);
            }
        }

        // Flag ID
        lore.add(ChatColor.DARK_GRAY + "ID: " + flagId);

        // Blank line
        lore.add("");

        // Current value display
        if (rawValue != null) {
            String displayValue = getDisplayValue(flag, rawValue);
            lore.add(ChatColor.WHITE + "Value: " + ChatColor.GREEN + displayValue);
        } else if (resolvedValue != null) {
            String displayValue = getDisplayValue(flag, resolvedValue);
            lore.add(ChatColor.WHITE + "Value: " + ChatColor.AQUA + displayValue + ChatColor.GRAY + " (inherited)");
        } else {
            Object defaultValue = flag.getDefinition().getDefaultValue();
            if (defaultValue != null) {
                String defaultDisplay = getDisplayValue(flag, defaultValue);
                lore.add(ChatColor.WHITE + "Value: " + ChatColor.GRAY + "Not set");
                lore.add(ChatColor.GRAY + "(Default: " + defaultDisplay + ")");
            } else {
                lore.add(ChatColor.WHITE + "Value: " + ChatColor.GRAY + "Not set");
            }
        }

        // Inheritance info
        if (isInherited && inheritedFrom != null) {
            lore.add(ChatColor.DARK_AQUA + "Inherited from: " + ChatColor.WHITE + inheritedFrom);
        }

        // Blank line before actions
        lore.add("");

        // Action hints
        if (flag.getType() == FlagType.BOOLEAN) {
            lore.add(ChatColor.YELLOW + "Click to toggle");
        } else {
            lore.add(ChatColor.YELLOW + "Click to edit");
        }
        lore.add(ChatColor.GRAY + "Shift-click to unset");

        // Admin-only indicator
        if (flag.getDefinition().isAdminOnly()) {
            lore.add("");
            lore.add(ChatColor.RED + "" + ChatColor.ITALIC + "Admin only");
        }

        return createItem(woolMaterial, displayName, lore);
    }

    /**
     * Constructs the bottom navigation bar (row 6, slots 45-53).
     */
    private void buildNavigationBar() {
        int totalPages = getTotalPages();

        // Fill navigation row with dark glass pane background
        for (int slot = 45; slot <= 53; slot++) {
            inventory.setItem(slot, createNavFillerPane());
        }

        // Previous page arrow (slot 45)
        if (currentPage > 0) {
            ItemStack prevArrow = createItem(Material.ARROW,
                    ChatColor.GREEN + "Previous Page",
                    List.of(ChatColor.GRAY + "Page " + currentPage + "/" + totalPages));
            inventory.setItem(SLOT_PREV_PAGE, prevArrow);
        }

        // Search placeholder (slot 47)
        ItemStack searchItem = createItem(Material.COMPASS,
                ChatColor.YELLOW + "Search",
                List.of(ChatColor.GRAY + "Coming soon..."));
        inventory.setItem(SLOT_SEARCH, searchItem);

        // Page info (slot 49)
        ItemStack pageInfo = createItem(Material.PAPER,
                ChatColor.WHITE + "Page " + (currentPage + 1) + "/" + totalPages,
                List.of(ChatColor.GRAY + "" + currentFlags.size() + " flags total",
                        ChatColor.GRAY + "Scope: " + ChatColor.WHITE + getScopeDisplayName()));
        inventory.setItem(SLOT_PAGE_INFO, pageInfo);

        // Close button (slot 51)
        ItemStack closeItem = createItem(Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Click to close"));
        inventory.setItem(SLOT_CLOSE, closeItem);

        // Next page arrow (slot 53)
        if (currentPage < totalPages - 1) {
            ItemStack nextArrow = createItem(Material.ARROW,
                    ChatColor.GREEN + "Next Page",
                    List.of(ChatColor.GRAY + "Page " + (currentPage + 2) + "/" + totalPages));
            inventory.setItem(SLOT_NEXT_PAGE, nextArrow);
        }
    }

    /**
     * Handles a click event within this GUI's inventory.
     *
     * @param slot  the raw slot that was clicked
     * @param event the click event
     */
    public void handleClick(int slot, @NotNull InventoryClickEvent event) {
        // Category tab clicks (row 1)
        if (slot >= 0 && slot <= 8) {
            handleCategoryClick(slot);
            return;
        }

        // Flag item clicks (rows 2-5)
        if (slot >= FLAG_SLOT_START && slot <= FLAG_SLOT_END) {
            handleFlagClick(slot, event);
            return;
        }

        // Navigation bar clicks (row 6)
        if (slot == SLOT_PREV_PAGE) {
            prevPage();
            return;
        }
        if (slot == SLOT_NEXT_PAGE) {
            nextPage();
            return;
        }
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
    }

    /**
     * Handles a click on a category tab.
     *
     * @param slot the clicked slot (0-8)
     */
    private void handleCategoryClick(int slot) {
        if (slot == 0) {
            setCategory(null);
            return;
        }

        FlagCategory[] categories = FlagCategory.values();
        int categoryIndex = slot - 1;
        if (categoryIndex < categories.length) {
            setCategory(categories[categoryIndex]);
        }
    }

    /**
     * Handles a click on a flag item.
     *
     * @param slot  the clicked slot (9-44)
     * @param event the click event
     */
    @SuppressWarnings("unchecked")
    private void handleFlagClick(int slot, @NotNull InventoryClickEvent event) {
        int flagIndex = (currentPage * FLAGS_PER_PAGE) + (slot - FLAG_SLOT_START);
        if (flagIndex < 0 || flagIndex >= currentFlags.size()) {
            return; // Clicked an empty slot
        }

        AbstractFlag<?> flag = currentFlags.get(flagIndex);

        // Check permission
        if (flag.getDefinition().isAdminOnly() && !player.hasPermission("gpfr.admin")) {
            AdventureCompat.sendMessage(player,
                    "<red>You don't have permission to modify this flag.");
            return;
        }

        if (!player.hasPermission("gpfr.flag.set") && !player.hasPermission("gpfr.admin")) {
            AdventureCompat.sendMessage(player,
                    "<red>You don't have permission to modify flags.");
            return;
        }

        FlagManager flagManager = plugin.getFlagManager();

        // Shift-click: unset the flag
        if (event.isShiftClick()) {
            flagManager.unsetFlag(flag.getId(), editScope, editScopeId);
            AdventureCompat.sendMessage(player,
                    "<gray>Unset flag <white>" + flag.getDefinition().getDisplayName()
                            + " <gray>at <white>" + getScopeDisplayName());
            rebuildInventory();
            return;
        }

        // Boolean flag: toggle
        if (flag instanceof BooleanFlag) {
            Object currentRaw = flagManager.getRawFlagValue(flag.getId(), editScope, editScopeId);
            Object resolved = flagManager.getFlagValue(flag.getId(), editScopeId);
            boolean currentValue;
            if (currentRaw != null) {
                currentValue = (Boolean) currentRaw;
            } else if (resolved != null) {
                currentValue = (Boolean) resolved;
            } else {
                Object def = flag.getDefinition().getDefaultValue();
                currentValue = def != null && (Boolean) def;
            }

            String newValue = String.valueOf(!currentValue);
            try {
                flagManager.setFlag(flag.getId(), editScope, editScopeId, newValue, player.getUniqueId());
                AdventureCompat.sendMessage(player,
                        "<gray>Set <white>" + flag.getDefinition().getDisplayName()
                                + " <gray>to " + (!currentValue ? "<green>Enabled" : "<red>Disabled")
                                + " <gray>at <white>" + getScopeDisplayName());
            } catch (InvalidFlagValueException e) {
                AdventureCompat.sendMessage(player, "<red>Error: " + e.getMessage());
            }
            rebuildInventory();
            return;
        }

        // Non-boolean flag: open detail GUI
        FlagDetailGUI detailGUI = new FlagDetailGUI(plugin, player, flag, editScope, editScopeId, this);
        detailGUI.open();
    }

    /**
     * Sets the current category filter and resets to the first page.
     *
     * @param category the category to filter by, or null for all flags
     */
    public void setCategory(@Nullable FlagCategory category) {
        this.currentCategory = category;
        this.currentPage = 0;
        this.currentFlags = getFilteredFlags();
        rebuildInventory();
    }

    /**
     * Advances to the next page if available.
     */
    public void nextPage() {
        if (currentPage < getTotalPages() - 1) {
            currentPage++;
            rebuildInventory();
        }
    }

    /**
     * Returns to the previous page if available.
     */
    public void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            rebuildInventory();
        }
    }

    /**
     * Returns the total number of pages required to display all filtered flags.
     *
     * @return the total page count (minimum 1)
     */
    private int getTotalPages() {
        if (currentFlags.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) currentFlags.size() / FLAGS_PER_PAGE);
    }

    /**
     * Attempts to find the scope from which a flag value is being inherited.
     * Checks lower-priority scopes in descending priority order.
     *
     * @param flagId the flag ID to look up
     * @return a display string identifying the source scope, or null if not found
     */
    @Nullable
    private String findInheritedSource(@NotNull String flagId) {
        FlagManager flagManager = plugin.getFlagManager();

        // Check scopes below the current scope in descending priority
        for (FlagScope scope : FlagScope.getOrderedByPriorityDescending()) {
            if (scope.getPriority() >= editScope.getPriority()) {
                continue; // Skip same or higher priority scopes
            }

            String scopeId;
            switch (scope) {
                case SERVER:
                    scopeId = "server";
                    break;
                case WORLD:
                    scopeId = player.getWorld().getName();
                    break;
                default:
                    continue; // CLAIM/SUBCLAIM don't inherit from themselves
            }

            Object val = flagManager.getRawFlagValue(flagId, scope, scopeId);
            if (val != null) {
                return scope.name() + " (" + scopeId + ")";
            }
        }
        return null;
    }

    /**
     * Returns a human-readable display name for the current edit scope.
     *
     * @return the scope display name
     */
    @NotNull
    private String getScopeDisplayName() {
        return switch (editScope) {
            case SERVER -> "Server";
            case WORLD -> "World: " + editScopeId;
            case CLAIM -> "Claim #" + editScopeId;
            case SUBCLAIM -> "Subclaim #" + editScopeId;
        };
    }

    /**
     * Returns the display value for a flag's current value, using the flag's own
     * display formatting logic.
     *
     * @param flag  the flag
     * @param value the raw value object
     * @return the formatted display string
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private String getDisplayValue(@NotNull AbstractFlag<?> flag, @NotNull Object value) {
        try {
            return ((AbstractFlag<Object>) flag).getDisplayValue(value);
        } catch (ClassCastException e) {
            return String.valueOf(value);
        }
    }

    /**
     * Returns the {@link ChatColor} associated with a given flag category.
     *
     * @param category the category
     * @return the chat color for the category
     */
    @NotNull
    private ChatColor getCategoryColor(@NotNull FlagCategory category) {
        int ordinal = category.ordinal();
        if (ordinal < CATEGORY_COLORS.length) {
            return CATEGORY_COLORS[ordinal];
        }
        return ChatColor.WHITE;
    }

    /**
     * Returns the stained glass pane material for a given category.
     *
     * @param category the category
     * @return the glass pane material
     */
    @NotNull
    private Material getCategoryGlassPane(@NotNull FlagCategory category) {
        int ordinal = category.ordinal();
        if (ordinal < CATEGORY_GLASS_PANES.length) {
            return CATEGORY_GLASS_PANES[ordinal];
        }
        return Material.WHITE_STAINED_GLASS_PANE;
    }

    /**
     * Creates an {@link ItemStack} with the given material, display name, and lore.
     *
     * @param material    the item material
     * @param displayName the item display name (supports ChatColor)
     * @param lore        the lore lines
     * @return the constructed ItemStack
     */
    @NotNull
    private ItemStack createItem(@NotNull Material material, @NotNull String displayName,
                                 @NotNull List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a dark gray stained glass pane used as a filler in the flag display area.
     *
     * @return the filler pane ItemStack
     */
    @NotNull
    private ItemStack createFillerPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Creates a gray stained glass pane used as a filler in the navigation bar.
     *
     * @return the navigation filler pane ItemStack
     */
    @NotNull
    private ItemStack createNavFillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /**
     * Word-wraps a string into lines of approximately the given maximum width.
     *
     * @param text     the text to wrap
     * @param maxWidth the maximum characters per line
     * @return a list of wrapped lines
     */
    @NotNull
    private List<String> wordWrap(@NotNull String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.length() <= maxWidth) {
            lines.add(text);
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder();
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }

        return lines;
    }

    /**
     * Returns the player who owns this GUI instance.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the scope currently being edited.
     *
     * @return the edit scope
     */
    @NotNull
    public FlagScope getEditScope() {
        return editScope;
    }

    /**
     * Returns the scope ID currently being edited.
     *
     * @return the edit scope ID
     */
    @NotNull
    public String getEditScopeId() {
        return editScopeId;
    }

    /**
     * Returns the plugin instance.
     *
     * @return the plugin
     */
    @NotNull
    public GriefPreventionFlagsPlugin getPlugin() {
        return plugin;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, 54, ChatColor.DARK_PURPLE + "Flag Manager");
        }
        return inventory;
    }
}
