package com.blockforge.griefpreventionflagsreborn.gui;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.api.exceptions.InvalidFlagValueException;
import com.blockforge.griefpreventionflagsreborn.flags.AbstractFlag;
import com.blockforge.griefpreventionflagsreborn.flags.EntityTypeListFlag;
import com.blockforge.griefpreventionflagsreborn.flags.ListFlag;
import com.blockforge.griefpreventionflagsreborn.flags.MaterialListFlag;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A secondary 3-row (27-slot) GUI for editing non-boolean flag values.
 * <p>
 * This GUI provides different editing interfaces depending on the flag's type:
 * <ul>
 *   <li><b>List types</b> (STRING_LIST, MATERIAL_LIST, ENTITY_TYPE_LIST): Displays current
 *       list entries as individual items that can be clicked to remove, plus an "Add" button
 *       that prompts for chat input.</li>
 *   <li><b>String/Number types:</b> Displays current value information and an "Edit" button
 *       that prompts for chat input.</li>
 * </ul>
 * <p>
 * Chat input collection is managed through a static {@link #pendingInputs} map keyed by
 * player UUID. The {@link FlagGUIListener} checks this map when processing chat events.
 */
public class FlagDetailGUI implements InventoryHolder {

    // Row 3 action slots
    private static final int SLOT_CONFIRM = 18;
    private static final int SLOT_CANCEL = 22;
    private static final int SLOT_CLEAR = 26;

    // Row 2 special slots for list types
    private static final int SLOT_ADD_ENTRY = 17;

    // Row 2 special slot for string/number types
    private static final int SLOT_EDIT_VALUE = 13;

    /**
     * Enumeration of the types of chat input that can be pending.
     */
    public enum InputType {
        /** Setting a scalar value (string, number). */
        SET_VALUE,
        /** Adding an entry to a list flag. */
        ADD_TO_LIST
    }

    /**
     * Holds the state for a pending chat input from a player. When a player clicks
     * "Edit" or "Add" in the detail GUI, a PendingInput is stored in the static map
     * until the player types a response in chat.
     */
    public static class PendingInput {
        private final String flagId;
        private final FlagScope scope;
        private final String scopeId;
        private final InputType inputType;
        private final Consumer<String> callback;
        private final FlagDetailGUI sourceGUI;

        /**
         * Creates a new PendingInput.
         *
         * @param flagId    the flag being edited
         * @param scope     the scope being edited
         * @param scopeId   the scope identifier
         * @param inputType the type of input expected
         * @param callback  the callback to invoke with the player's input
         * @param sourceGUI the detail GUI to reopen after input
         */
        public PendingInput(@NotNull String flagId, @NotNull FlagScope scope, @NotNull String scopeId,
                            @NotNull InputType inputType, @NotNull Consumer<String> callback,
                            @NotNull FlagDetailGUI sourceGUI) {
            this.flagId = flagId;
            this.scope = scope;
            this.scopeId = scopeId;
            this.inputType = inputType;
            this.callback = callback;
            this.sourceGUI = sourceGUI;
        }

        @NotNull
        public String getFlagId() {
            return flagId;
        }

        @NotNull
        public FlagScope getScope() {
            return scope;
        }

        @NotNull
        public String getScopeId() {
            return scopeId;
        }

        @NotNull
        public InputType getInputType() {
            return inputType;
        }

        @NotNull
        public Consumer<String> getCallback() {
            return callback;
        }

        @NotNull
        public FlagDetailGUI getSourceGUI() {
            return sourceGUI;
        }
    }

    /**
     * Static map tracking players who have a pending chat input for flag editing.
     * Keyed by player UUID. The {@link FlagGUIListener} reads and removes entries
     * from this map when processing chat events.
     */
    private static final Map<UUID, PendingInput> pendingInputs = new HashMap<>();

    private final GriefPreventionFlagsPlugin plugin;
    private final Player player;
    private final AbstractFlag<?> flag;
    private final FlagScope editScope;
    private final String editScopeId;
    @Nullable
    private final FlagGUI parentGUI;
    private Inventory inventory;

    /**
     * Creates a new FlagDetailGUI for editing a specific flag.
     *
     * @param plugin      the plugin instance
     * @param player      the player editing the flag
     * @param flag        the flag being edited
     * @param editScope   the scope at which to edit
     * @param editScopeId the scope identifier
     * @param parentGUI   the parent FlagGUI to return to after editing, or null
     */
    public FlagDetailGUI(@NotNull GriefPreventionFlagsPlugin plugin, @NotNull Player player,
                         @NotNull AbstractFlag<?> flag, @NotNull FlagScope editScope,
                         @NotNull String editScopeId, @Nullable FlagGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.flag = flag;
        this.editScope = editScope;
        this.editScopeId = editScopeId;
        this.parentGUI = parentGUI;
    }

    /**
     * Opens the detail editing GUI for the player.
     */
    public void open() {
        String title = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Edit: "
                + ChatColor.RESET + ChatColor.WHITE + flag.getDefinition().getDisplayName();
        inventory = Bukkit.createInventory(this, 27, title);
        rebuildInventory();
        player.openInventory(inventory);
    }

    /**
     * Clears and repopulates the inventory contents.
     */
    public void rebuildInventory() {
        if (inventory == null) {
            return;
        }
        inventory.clear();

        // Fill background
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, createFillerPane());
        }

        buildInfoRow();
        buildEditRow();
        buildActionRow();
    }

    /**
     * Builds row 1 (slots 0-8): flag information display.
     */
    private void buildInfoRow() {
        FlagManager flagManager = plugin.getFlagManager();
        String flagId = flag.getId();

        // Info item in the center of row 1 (slot 4)
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + flag.getDefinition().getDescription());
        infoLore.add("");
        infoLore.add(ChatColor.WHITE + "Type: " + ChatColor.YELLOW + flag.getType().name());
        infoLore.add(ChatColor.WHITE + "Category: " + ChatColor.YELLOW
                + flag.getCategory().getDisplayName());
        infoLore.add("");

        Object rawValue = flagManager.getRawFlagValue(flagId, editScope, editScopeId);
        if (rawValue != null) {
            String displayValue = getDisplayValue(rawValue);
            infoLore.add(ChatColor.WHITE + "Current value: " + ChatColor.GREEN + displayValue);
        } else {
            infoLore.add(ChatColor.WHITE + "Current value: " + ChatColor.GRAY + "Not set");
            Object defaultValue = flag.getDefinition().getDefaultValue();
            if (defaultValue != null) {
                infoLore.add(ChatColor.GRAY + "(Default: " + getDisplayValue(defaultValue) + ")");
            }
        }

        // Allowed enum values hint
        if (flag.getDefinition().getAllowedEnumValues() != null) {
            infoLore.add("");
            infoLore.add(ChatColor.DARK_GRAY + "Allowed values:");
            for (String allowed : flag.getDefinition().getAllowedEnumValues()) {
                infoLore.add(ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + allowed);
            }
        }

        ItemStack infoItem = createItem(Material.PAPER,
                ChatColor.GOLD + "" + ChatColor.BOLD + flag.getDefinition().getDisplayName(),
                infoLore);
        inventory.setItem(4, infoItem);
    }

    /**
     * Builds row 2 (slots 9-17): type-specific editing interface.
     * <p>
     * For list types, this shows current entries and an "Add" button.
     * For scalar types, this shows an "Edit" button.
     */
    @SuppressWarnings("unchecked")
    private void buildEditRow() {
        FlagType type = flag.getType();

        if (type == FlagType.STRING_LIST || type == FlagType.MATERIAL_LIST
                || type == FlagType.ENTITY_TYPE_LIST) {
            buildListEditRow();
        } else {
            buildScalarEditRow();
        }
    }

    /**
     * Builds the editing row for list-type flags, showing current entries and an "Add" button.
     */
    @SuppressWarnings("unchecked")
    private void buildListEditRow() {
        FlagManager flagManager = plugin.getFlagManager();
        Object rawValue = flagManager.getRawFlagValue(flag.getId(), editScope, editScopeId);

        List<String> entries = Collections.emptyList();
        if (rawValue instanceof List<?>) {
            entries = (List<String>) rawValue;
        }

        // Show up to 7 entries (slots 9-15)
        int maxEntries = Math.min(entries.size(), 7);
        for (int i = 0; i < maxEntries; i++) {
            String entry = entries.get(i);
            List<String> entryLore = new ArrayList<>();
            entryLore.add(ChatColor.GRAY + "Entry #" + (i + 1));
            entryLore.add("");
            entryLore.add(ChatColor.RED + "Click to remove");

            ItemStack entryItem = createItem(Material.PAPER,
                    ChatColor.WHITE + entry, entryLore);
            inventory.setItem(9 + i, entryItem);
        }

        // If there are more entries than can be shown
        if (entries.size() > 7) {
            List<String> overflowLore = new ArrayList<>();
            overflowLore.add(ChatColor.GRAY + "...and " + (entries.size() - 7) + " more entries");
            ItemStack overflowItem = createItem(Material.BOOK,
                    ChatColor.YELLOW + "" + entries.size() + " entries total", overflowLore);
            inventory.setItem(16, overflowItem);
        }

        // "Add" button (slot 17)
        List<String> addLore = new ArrayList<>();
        addLore.add(ChatColor.GRAY + "Click to add a new entry");
        addLore.add(ChatColor.GRAY + "You will be prompted in chat");
        ItemStack addButton = createItem(Material.LIME_DYE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Add Entry", addLore);
        inventory.setItem(SLOT_ADD_ENTRY, addButton);
    }

    /**
     * Builds the editing row for scalar (string/number) flags, showing an "Edit" button.
     */
    private void buildScalarEditRow() {
        FlagManager flagManager = plugin.getFlagManager();
        Object rawValue = flagManager.getRawFlagValue(flag.getId(), editScope, editScopeId);

        // Current value display (slot 11)
        List<String> valueLore = new ArrayList<>();
        if (rawValue != null) {
            valueLore.add(ChatColor.WHITE + "Current: " + ChatColor.GREEN + getDisplayValue(rawValue));
        } else {
            valueLore.add(ChatColor.WHITE + "Current: " + ChatColor.GRAY + "Not set");
        }

        if (flag.getType() == FlagType.INTEGER || flag.getType() == FlagType.DOUBLE) {
            valueLore.add("");
            valueLore.add(ChatColor.GRAY + "Type a number in chat to set");
        } else {
            valueLore.add("");
            valueLore.add(ChatColor.GRAY + "Type a value in chat to set");
        }

        ItemStack valueItem = createItem(Material.NAME_TAG,
                ChatColor.YELLOW + "Current Value", valueLore);
        inventory.setItem(11, valueItem);

        // "Edit" button (slot 13)
        List<String> editLore = new ArrayList<>();
        editLore.add(ChatColor.GRAY + "Click to set a new value");
        editLore.add(ChatColor.GRAY + "You will be prompted in chat");
        if (flag.getDefinition().getAllowedEnumValues() != null) {
            editLore.add("");
            editLore.add(ChatColor.DARK_GRAY + "Allowed values:");
            for (String allowed : flag.getDefinition().getAllowedEnumValues()) {
                editLore.add(ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + allowed);
            }
        }
        ItemStack editButton = createItem(Material.WRITABLE_BOOK,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Edit Value", editLore);
        inventory.setItem(SLOT_EDIT_VALUE, editButton);

        // Visual indicator of type (slot 15)
        Material typeIcon = switch (flag.getType()) {
            case INTEGER, DOUBLE -> Material.GOLD_NUGGET;
            case STRING -> Material.OAK_SIGN;
            default -> Material.PAPER;
        };
        List<String> typeLore = new ArrayList<>();
        typeLore.add(ChatColor.GRAY + "Type: " + flag.getType().name());
        ItemStack typeItem = createItem(typeIcon,
                ChatColor.AQUA + "Type: " + flag.getType().name(), typeLore);
        inventory.setItem(15, typeItem);
    }

    /**
     * Builds row 3 (slots 18-26): action buttons (Confirm/Back, Cancel, Clear/Unset).
     */
    private void buildActionRow() {
        // "Back" button (slot 18) - returns to parent GUI
        List<String> backLore = new ArrayList<>();
        backLore.add(ChatColor.GRAY + "Return to flag list");
        ItemStack backItem = createItem(Material.GREEN_WOOL,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Back", backLore);
        inventory.setItem(SLOT_CONFIRM, backItem);

        // "Cancel" button (slot 22) - closes without changes
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Close without changes");
        ItemStack cancelItem = createItem(Material.RED_WOOL,
                ChatColor.RED + "" + ChatColor.BOLD + "Cancel", cancelLore);
        inventory.setItem(SLOT_CANCEL, cancelItem);

        // "Clear/Unset" button (slot 26) - removes the flag value at this scope
        List<String> clearLore = new ArrayList<>();
        clearLore.add(ChatColor.GRAY + "Remove the value at this scope");
        clearLore.add(ChatColor.GRAY + "The flag will fall through to");
        clearLore.add(ChatColor.GRAY + "a lower scope or its default");
        ItemStack clearItem = createItem(Material.GRAY_WOOL,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "Unset", clearLore);
        inventory.setItem(SLOT_CLEAR, clearItem);
    }

    /**
     * Handles a click event within this GUI's inventory.
     *
     * @param slot  the raw slot that was clicked
     * @param event the click event
     */
    @SuppressWarnings("unchecked")
    public void handleClick(int slot, @NotNull InventoryClickEvent event) {
        FlagManager flagManager = plugin.getFlagManager();

        // Row 3 action buttons
        if (slot == SLOT_CONFIRM) {
            // Back to parent GUI
            returnToParent();
            return;
        }

        if (slot == SLOT_CANCEL) {
            player.closeInventory();
            return;
        }

        if (slot == SLOT_CLEAR) {
            flagManager.unsetFlag(flag.getId(), editScope, editScopeId);
            AdventureCompat.sendMessage(player,
                    "<gray>Unset flag <white>" + flag.getDefinition().getDisplayName()
                            + " <gray>at scope.");
            rebuildInventory();
            return;
        }

        // List type: entry removal (slots 9-15)
        FlagType type = flag.getType();
        if (isListType(type) && slot >= 9 && slot <= 15) {
            handleListEntryRemoval(slot);
            return;
        }

        // List type: add entry (slot 17)
        if (isListType(type) && slot == SLOT_ADD_ENTRY) {
            promptChatInput(InputType.ADD_TO_LIST);
            return;
        }

        // Scalar type: edit value (slot 13)
        if (!isListType(type) && slot == SLOT_EDIT_VALUE) {
            promptChatInput(InputType.SET_VALUE);
            return;
        }
    }

    /**
     * Handles clicking on a list entry to remove it.
     *
     * @param slot the clicked slot (9-15)
     */
    @SuppressWarnings("unchecked")
    private void handleListEntryRemoval(int slot) {
        FlagManager flagManager = plugin.getFlagManager();
        Object rawValue = flagManager.getRawFlagValue(flag.getId(), editScope, editScopeId);

        if (!(rawValue instanceof List<?>)) {
            return;
        }

        List<String> currentList = new ArrayList<>((List<String>) rawValue);
        int entryIndex = slot - 9;
        if (entryIndex < 0 || entryIndex >= currentList.size()) {
            return;
        }

        String removedEntry = currentList.get(entryIndex);
        currentList.remove(entryIndex);

        // Serialize and set the updated list
        String serialized = String.join(",", currentList);
        if (currentList.isEmpty()) {
            flagManager.unsetFlag(flag.getId(), editScope, editScopeId);
            AdventureCompat.sendMessage(player,
                    "<gray>Removed <white>" + removedEntry
                            + " <gray>- list is now empty, flag unset.");
        } else {
            try {
                flagManager.setFlag(flag.getId(), editScope, editScopeId,
                        serialized, player.getUniqueId());
                AdventureCompat.sendMessage(player,
                        "<gray>Removed <white>" + removedEntry + " <gray>from the list.");
            } catch (InvalidFlagValueException e) {
                AdventureCompat.sendMessage(player, "<red>Error: " + e.getMessage());
            }
        }

        rebuildInventory();
    }

    /**
     * Initiates chat input collection for the player. The GUI is closed and the player
     * is prompted to type a value in chat.
     *
     * @param inputType the type of input to collect
     */
    private void promptChatInput(@NotNull InputType inputType) {
        FlagDetailGUI self = this;

        Consumer<String> callback = (input) -> {
            // Run on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                processInput(input, inputType);
            });
        };

        PendingInput pending = new PendingInput(
                flag.getId(), editScope, editScopeId, inputType, callback, self);
        pendingInputs.put(player.getUniqueId(), pending);

        player.closeInventory();

        if (inputType == InputType.ADD_TO_LIST) {
            AdventureCompat.sendMessage(player,
                    "<gold>Type the value to add to <white>"
                            + flag.getDefinition().getDisplayName() + "<gold>:");
            AdventureCompat.sendMessage(player,
                    "<gray>Type <white>cancel <gray>to cancel.");
        } else {
            AdventureCompat.sendMessage(player,
                    "<gold>Type the new value for <white>"
                            + flag.getDefinition().getDisplayName() + "<gold>:");
            if (flag.getDefinition().getAllowedEnumValues() != null) {
                AdventureCompat.sendMessage(player,
                        "<gray>Allowed values: <white>"
                                + String.join(", ", flag.getDefinition().getAllowedEnumValues()));
            }
            AdventureCompat.sendMessage(player,
                    "<gray>Type <white>cancel <gray>to cancel.");
        }
    }

    /**
     * Processes a chat input received from the player.
     *
     * @param input     the text the player typed
     * @param inputType the type of input (SET_VALUE or ADD_TO_LIST)
     */
    @SuppressWarnings("unchecked")
    private void processInput(@NotNull String input, @NotNull InputType inputType) {
        FlagManager flagManager = plugin.getFlagManager();

        if (input.equalsIgnoreCase("cancel")) {
            AdventureCompat.sendMessage(player, "<gray>Input cancelled.");
            // Reopen detail GUI
            Bukkit.getScheduler().runTaskLater(plugin, this::open, 1L);
            return;
        }

        try {
            if (inputType == InputType.SET_VALUE) {
                flagManager.setFlag(flag.getId(), editScope, editScopeId,
                        input, player.getUniqueId());
                AdventureCompat.sendMessage(player,
                        "<gray>Set <white>" + flag.getDefinition().getDisplayName()
                                + " <gray>to <green>" + input);
            } else if (inputType == InputType.ADD_TO_LIST) {
                // Get current list, add to it
                Object rawValue = flagManager.getRawFlagValue(flag.getId(), editScope, editScopeId);
                List<String> currentList;
                if (rawValue instanceof List<?>) {
                    currentList = new ArrayList<>((List<String>) rawValue);
                } else {
                    currentList = new ArrayList<>();
                }

                // Check for duplicates
                String toAdd = input.trim();
                if (flag.getType() == FlagType.MATERIAL_LIST || flag.getType() == FlagType.ENTITY_TYPE_LIST) {
                    toAdd = toAdd.toUpperCase();
                }

                if (currentList.contains(toAdd)) {
                    AdventureCompat.sendMessage(player,
                            "<red>The value <white>" + toAdd + " <red>is already in the list.");
                } else {
                    currentList.add(toAdd);
                    String serialized = String.join(",", currentList);
                    flagManager.setFlag(flag.getId(), editScope, editScopeId,
                            serialized, player.getUniqueId());
                    AdventureCompat.sendMessage(player,
                            "<gray>Added <white>" + toAdd + " <gray>to <white>"
                                    + flag.getDefinition().getDisplayName());
                }
            }
        } catch (InvalidFlagValueException e) {
            AdventureCompat.sendMessage(player, "<red>Invalid value: " + e.getMessage());
        }

        // Reopen detail GUI after a tick
        Bukkit.getScheduler().runTaskLater(plugin, this::open, 1L);
    }

    /**
     * Returns to the parent FlagGUI. If no parent is set, simply closes the inventory.
     */
    public void returnToParent() {
        if (parentGUI != null) {
            parentGUI.open();
        } else {
            player.closeInventory();
        }
    }

    /**
     * Returns the display value for a given raw value using the flag's formatting logic.
     *
     * @param value the raw value
     * @return the formatted display string
     */
    @SuppressWarnings("unchecked")
    @NotNull
    private String getDisplayValue(@NotNull Object value) {
        try {
            return ((AbstractFlag<Object>) flag).getDisplayValue(value);
        } catch (ClassCastException e) {
            return String.valueOf(value);
        }
    }

    /**
     * Checks whether the given flag type is a list type.
     *
     * @param type the flag type
     * @return true if the type is STRING_LIST, MATERIAL_LIST, or ENTITY_TYPE_LIST
     */
    private boolean isListType(@NotNull FlagType type) {
        return type == FlagType.STRING_LIST
                || type == FlagType.MATERIAL_LIST
                || type == FlagType.ENTITY_TYPE_LIST;
    }

    /**
     * Creates an {@link ItemStack} with the given material, display name, and lore.
     *
     * @param material    the item material
     * @param displayName the display name
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
     * Creates a dark gray stained glass pane used as background filler.
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
     * Returns the player who owns this GUI.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the flag being edited.
     *
     * @return the flag
     */
    @NotNull
    public AbstractFlag<?> getFlag() {
        return flag;
    }

    /**
     * Returns the edit scope.
     *
     * @return the scope
     */
    @NotNull
    public FlagScope getEditScope() {
        return editScope;
    }

    /**
     * Returns the edit scope ID.
     *
     * @return the scope ID
     */
    @NotNull
    public String getEditScopeId() {
        return editScopeId;
    }

    /**
     * Returns the parent FlagGUI, or null if none was set.
     *
     * @return the parent GUI
     */
    @Nullable
    public FlagGUI getParentGUI() {
        return parentGUI;
    }

    /**
     * Checks whether a given player has a pending chat input.
     *
     * @param playerUUID the player's UUID
     * @return true if the player has a pending input
     */
    public static boolean hasPendingInput(@NotNull UUID playerUUID) {
        return pendingInputs.containsKey(playerUUID);
    }

    /**
     * Retrieves and removes the pending input for a player.
     *
     * @param playerUUID the player's UUID
     * @return the pending input, or null if none exists
     */
    @Nullable
    public static PendingInput consumePendingInput(@NotNull UUID playerUUID) {
        return pendingInputs.remove(playerUUID);
    }

    /**
     * Removes any pending input for a player without processing it.
     *
     * @param playerUUID the player's UUID
     */
    public static void cancelPendingInput(@NotNull UUID playerUUID) {
        pendingInputs.remove(playerUUID);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, 27,
                    ChatColor.DARK_PURPLE + "Edit Flag");
        }
        return inventory;
    }
}
