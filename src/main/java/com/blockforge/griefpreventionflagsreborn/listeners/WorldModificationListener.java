package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces world modification flags that control block breaking, block placing,
 * protected materials, lighter/bucket use, and hanging entity manipulation.
 * <p>
 * Handles 10 flags: no-block-break, protected-blocks, no-block-place,
 * blocked-place-blocks, no-lighter-use, no-bucket-use, no-item-frame-break,
 * no-painting-break, no-armor-stand-manipulate, and related hanging entity flags.
 */
public final class WorldModificationListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new WorldModificationListener.
     *
     * @param plugin the owning plugin instance
     */
    public WorldModificationListener(GriefPreventionFlagsPlugin plugin) {
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

    // ---------------------------------------------------------------
    //  BlockBreakEvent - no-block-break, protected-blocks
    // ---------------------------------------------------------------

    /**
     * Prevents block breaking when no-block-break is enabled, or when
     * the block material is in the protected-blocks list.
     *
     * @param event the block break event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        // Check no-block-break
        if (shouldBlock(player, "no-block-break", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-block-break");
            return;
        }

        // Check protected-blocks material list
        if (!player.hasPermission("gpfr.bypass.protected-blocks")) {
            List<Material> protectedBlocks = flagManager.getFlagValue("protected-blocks", location);
            if (protectedBlocks != null && !protectedBlocks.isEmpty()) {
                if (protectedBlocks.contains(event.getBlock().getType())) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "protected-blocks");
                }
            }
        }
    }

    // ---------------------------------------------------------------
    //  BlockPlaceEvent - no-block-place, blocked-place-blocks
    // ---------------------------------------------------------------

    /**
     * Prevents block placement when no-block-place is enabled, or when
     * the block material is in the blocked-place-blocks list.
     *
     * @param event the block place event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        // Check no-block-place
        if (shouldBlock(player, "no-block-place", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-block-place");
            return;
        }

        // Check blocked-place-blocks material list
        if (!player.hasPermission("gpfr.bypass.blocked-place-blocks")) {
            List<Material> blockedBlocks = flagManager.getFlagValue("blocked-place-blocks", location);
            if (blockedBlocks != null && !blockedBlocks.isEmpty()) {
                if (blockedBlocks.contains(event.getBlock().getType())) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "blocked-place-blocks");
                }
            }
        }
    }

    // ---------------------------------------------------------------
    //  BlockIgniteEvent - no-lighter-use flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from using flint and steel or fire charges when
     * the no-lighter-use flag is enabled.
     *
     * @param event the block ignite event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
                && event.getCause() != BlockIgniteEvent.IgniteCause.FIREBALL) return;

        Player player = event.getPlayer();
        if (player == null) return;

        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-lighter-use", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-lighter-use");
        }
    }

    // ---------------------------------------------------------------
    //  PlayerBucketEmptyEvent / PlayerBucketFillEvent - no-bucket-use
    // ---------------------------------------------------------------

    /**
     * Prevents players from emptying buckets when the no-bucket-use flag
     * is enabled.
     *
     * @param event the player bucket empty event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-bucket-use", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-bucket-use");
        }
    }

    /**
     * Prevents players from filling buckets when the no-bucket-use flag
     * is enabled.
     *
     * @param event the player bucket fill event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-bucket-use", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-bucket-use");
        }
    }

    // ---------------------------------------------------------------
    //  HangingBreakByEntityEvent - item frame/painting break
    // ---------------------------------------------------------------

    /**
     * Prevents players from breaking item frames and paintings when the
     * appropriate flags are enabled.
     *
     * @param event the hanging break by entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        if (!(remover instanceof Player player)) return;

        Hanging hanging = event.getEntity();
        Location location = hanging.getLocation();
        if (location.getWorld() == null) return;

        // Check no-item-frame-break
        if (hanging instanceof ItemFrame) {
            if (shouldBlock(player, "no-item-frame-break", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-item-frame-break");
                return;
            }
        }

        // Check no-painting-break
        if (hanging instanceof Painting) {
            if (shouldBlock(player, "no-painting-break", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-painting-break");
            }
        }
    }

    // ---------------------------------------------------------------
    //  HangingPlaceEvent - hanging entity placement protection
    // ---------------------------------------------------------------

    /**
     * Handles hanging entity placement checks. Item frame and painting
     * placement may also be restricted by the respective break flags
     * depending on server configuration, though this primarily guards
     * placement separately.
     *
     * @param event the hanging place event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        Hanging hanging = event.getEntity();
        Location location = hanging.getLocation();
        if (location.getWorld() == null) return;

        // Item frame placement can be covered by no-item-frame-break symmetry
        if (hanging instanceof ItemFrame) {
            if (shouldBlock(player, "no-item-frame-break", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-item-frame-break");
                return;
            }
        }

        // Painting placement
        if (hanging instanceof Painting) {
            if (shouldBlock(player, "no-painting-break", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-painting-break");
            }
        }
    }

    // ---------------------------------------------------------------
    //  PlayerArmorStandManipulateEvent - no-armor-stand-manipulate
    // ---------------------------------------------------------------

    /**
     * Prevents players from manipulating armor stands when the
     * no-armor-stand-manipulate flag is enabled.
     *
     * @param event the player armor stand manipulate event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        Location location = event.getRightClicked().getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-armor-stand-manipulate", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-armor-stand-manipulate");
        }
    }
}
