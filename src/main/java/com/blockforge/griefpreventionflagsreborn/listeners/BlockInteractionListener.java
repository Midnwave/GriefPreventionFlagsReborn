package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces block interaction flags that control player access to containers,
 * doors, buttons, levers, and other interactive blocks within flagged areas.
 * <p>
 * Handles 12 flags: no-chest-access, no-door-access, no-button-access,
 * no-lever-access, no-trapdoor-access, no-fence-gate-access, no-anvil-access,
 * no-enchanting-table, no-brewing-stand-access, no-beacon-access,
 * no-ender-chest-access, no-note-block-play, no-container-access,
 * and no-sign-edit.
 */
public final class BlockInteractionListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new BlockInteractionListener.
     *
     * @param plugin the owning plugin instance
     */
    public BlockInteractionListener(GriefPreventionFlagsPlugin plugin) {
        this.plugin = plugin;
        this.flagManager = plugin.getFlagManager();
        this.gpHook = plugin.getGpHook();
    }

    // ---------------------------------------------------------------
    //  Helper methods
    // ---------------------------------------------------------------

    private boolean shouldBlock(Player player, String flagId, Location location) {
        if (player.hasPermission("gpfr.bypass." + flagId)) return false;
        return flagManager.isFlagEnabled(flagId, location);
    }

    private void sendBlockedMessage(Player player, String flagId) {
        String key = player.getUniqueId() + ":" + flagId;
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(key);
        if (last == null || now - last > 2000) {
            messageCooldowns.put(key, now);
            plugin.getMessagesManager().sendMessage(player, "flag-blocked-action");
        }
    }

    /**
     * Checks if a material is a chest-type container.
     *
     * @param material the material to check
     * @return true if the material is a chest, trapped chest, or barrel
     */
    private boolean isChestType(Material material) {
        return material == Material.CHEST
                || material == Material.TRAPPED_CHEST
                || material == Material.BARREL;
    }

    /**
     * Checks if a material is a door using the Bukkit Tag system.
     *
     * @param material the material to check
     * @return true if the material is a door
     */
    private boolean isDoor(Material material) {
        return Tag.DOORS.isTagged(material);
    }

    /**
     * Checks if a material is a button using the Bukkit Tag system.
     *
     * @param material the material to check
     * @return true if the material is a button
     */
    private boolean isButton(Material material) {
        return Tag.BUTTONS.isTagged(material);
    }

    /**
     * Checks if a material is a trapdoor using the Bukkit Tag system.
     *
     * @param material the material to check
     * @return true if the material is a trapdoor
     */
    private boolean isTrapdoor(Material material) {
        return Tag.TRAPDOORS.isTagged(material);
    }

    /**
     * Checks if a material is a fence gate using the Bukkit Tag system.
     *
     * @param material the material to check
     * @return true if the material is a fence gate
     */
    private boolean isFenceGate(Material material) {
        return Tag.FENCE_GATES.isTagged(material);
    }

    /**
     * Checks if a material is an anvil (regular, chipped, or damaged).
     *
     * @param material the material to check
     * @return true if the material is an anvil variant
     */
    private boolean isAnvil(Material material) {
        return material == Material.ANVIL
                || material == Material.CHIPPED_ANVIL
                || material == Material.DAMAGED_ANVIL;
    }

    // ---------------------------------------------------------------
    //  PlayerInteractEvent - block interaction flags
    // ---------------------------------------------------------------

    /**
     * Handles right-click interactions with various block types,
     * checking the appropriate flag for each block category.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Location location = block.getLocation();
        if (location.getWorld() == null) return;

        Material material = block.getType();

        // Chest, trapped chest, barrel
        if (isChestType(material)) {
            if (shouldBlock(player, "no-chest-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-chest-access");
                return;
            }
        }

        // Doors
        if (isDoor(material)) {
            if (shouldBlock(player, "no-door-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-door-access");
                return;
            }
        }

        // Buttons
        if (isButton(material)) {
            if (shouldBlock(player, "no-button-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-button-access");
                return;
            }
        }

        // Levers
        if (material == Material.LEVER) {
            if (shouldBlock(player, "no-lever-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-lever-access");
                return;
            }
        }

        // Trapdoors
        if (isTrapdoor(material)) {
            if (shouldBlock(player, "no-trapdoor-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-trapdoor-access");
                return;
            }
        }

        // Fence gates
        if (isFenceGate(material)) {
            if (shouldBlock(player, "no-fence-gate-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-fence-gate-access");
                return;
            }
        }

        // Anvils
        if (isAnvil(material)) {
            if (shouldBlock(player, "no-anvil-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-anvil-access");
                return;
            }
        }

        // Enchanting table
        if (material == Material.ENCHANTING_TABLE) {
            if (shouldBlock(player, "no-enchanting-table", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-enchanting-table");
                return;
            }
        }

        // Brewing stand
        if (material == Material.BREWING_STAND) {
            if (shouldBlock(player, "no-brewing-stand-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-brewing-stand-access");
                return;
            }
        }

        // Beacon
        if (material == Material.BEACON) {
            if (shouldBlock(player, "no-beacon-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-beacon-access");
                return;
            }
        }

        // Ender chest
        if (material == Material.ENDER_CHEST) {
            if (shouldBlock(player, "no-ender-chest-access", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-ender-chest-access");
                return;
            }
        }

        // Note block
        if (material == Material.NOTE_BLOCK) {
            if (shouldBlock(player, "no-note-block-play", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-note-block-play");
            }
        }
    }

    // ---------------------------------------------------------------
    //  InventoryOpenEvent - no-container-access flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from opening container inventories when the
     * no-container-access flag is enabled.
     *
     * @param event the inventory open event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Only check for container-type inventories
        InventoryType type = event.getInventory().getType();
        if (!isContainerInventory(type)) return;

        // Try to get the location of the container
        Location location = null;
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof org.bukkit.block.Container container) {
            location = container.getLocation();
        }

        // Fallback to player location
        if (location == null) {
            location = player.getLocation();
        }
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-container-access", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-container-access");
        }
    }

    /**
     * Determines if an inventory type represents a container.
     *
     * @param type the inventory type to check
     * @return true if the inventory type is a container
     */
    private boolean isContainerInventory(InventoryType type) {
        return type == InventoryType.CHEST
                || type == InventoryType.BARREL
                || type == InventoryType.SHULKER_BOX
                || type == InventoryType.HOPPER
                || type == InventoryType.DROPPER
                || type == InventoryType.DISPENSER
                || type == InventoryType.FURNACE
                || type == InventoryType.BLAST_FURNACE
                || type == InventoryType.SMOKER
                || type == InventoryType.BREWING
                || type == InventoryType.BEACON;
    }

    // ---------------------------------------------------------------
    //  SignChangeEvent - no-sign-edit flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from editing signs when the no-sign-edit flag
     * is enabled.
     *
     * @param event the sign change event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-sign-edit", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-sign-edit");
        }
    }
}
