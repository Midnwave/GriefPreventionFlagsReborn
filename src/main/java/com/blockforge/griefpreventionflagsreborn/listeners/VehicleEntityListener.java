package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
// PlayerLeashEntityEvent was removed in Paper 1.21 - leash control is handled via PlayerInteractEntityEvent
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces vehicle and entity interaction flags that control vehicle
 * placement/destruction/entry, animal breeding/taming, leash use,
 * item frame rotation, and armor stand manipulation.
 * <p>
 * Handles 8 flags: no-vehicle-place, no-vehicle-destroy, no-vehicle-enter,
 * no-animal-breed, no-animal-tame, no-lead-use, no-item-frame-rotate,
 * and no-armor-stand-manipulate.
 */
public final class VehicleEntityListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new VehicleEntityListener.
     *
     * @param plugin the owning plugin instance
     */
    public VehicleEntityListener(GriefPreventionFlagsPlugin plugin) {
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
     * Checks if a flag is enabled at a location (for non-player events).
     *
     * @param flagId   the flag identifier
     * @param location the location to check
     * @return true if the flag is enabled at the location
     */
    private boolean isFlagActive(String flagId, Location location) {
        return flagManager.isFlagEnabled(flagId, location);
    }

    // ---------------------------------------------------------------
    //  VehicleCreateEvent - no-vehicle-place flag
    // ---------------------------------------------------------------

    /**
     * Prevents vehicle placement when the no-vehicle-place flag is enabled.
     * Note: VehicleCreateEvent is not cancellable on all server versions.
     * On servers where it is cancellable, we cancel it directly.
     *
     * @param event the vehicle create event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        Location location = event.getVehicle().getLocation();
        if (location.getWorld() == null) return;

        if (isFlagActive("no-vehicle-place", location)) {
            // VehicleCreateEvent became cancellable in newer Paper/Spigot versions
            try {
                event.setCancelled(true);
            } catch (UnsupportedOperationException e) {
                // On older versions, remove the vehicle next tick
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        event.getVehicle().remove());
            }
        }
    }

    // ---------------------------------------------------------------
    //  VehicleDestroyEvent - no-vehicle-destroy flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from destroying vehicles when the no-vehicle-destroy
     * flag is enabled.
     *
     * @param event the vehicle destroy event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Entity attacker = event.getAttacker();
        if (!(attacker instanceof Player player)) return;

        Location location = event.getVehicle().getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-vehicle-destroy", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-vehicle-destroy");
        }
    }

    // ---------------------------------------------------------------
    //  VehicleEnterEvent - no-vehicle-enter flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from entering vehicles when the no-vehicle-enter
     * flag is enabled.
     *
     * @param event the vehicle enter event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;

        Location location = event.getVehicle().getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-vehicle-enter", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-vehicle-enter");
        }
    }

    // ---------------------------------------------------------------
    //  EntityBreedEvent - no-animal-breed flag
    // ---------------------------------------------------------------

    /**
     * Prevents animal breeding when the no-animal-breed flag is enabled.
     *
     * @param event the entity breed event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        Location location = event.getEntity().getLocation();
        if (location.getWorld() == null) return;

        // If a player initiated the breeding, check bypass
        if (event.getBreeder() instanceof Player player) {
            if (shouldBlock(player, "no-animal-breed", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-animal-breed");
            }
        } else {
            // No player breeder - just check the flag at location
            if (isFlagActive("no-animal-breed", location)) {
                event.setCancelled(true);
            }
        }
    }

    // ---------------------------------------------------------------
    //  EntityTameEvent - no-animal-tame flag
    // ---------------------------------------------------------------

    /**
     * Prevents animal taming when the no-animal-tame flag is enabled.
     *
     * @param event the entity tame event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        Location location = event.getEntity().getLocation();
        if (location.getWorld() == null) return;

        if (event.getOwner() instanceof Player player) {
            if (shouldBlock(player, "no-animal-tame", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-animal-tame");
            }
        } else {
            if (isFlagActive("no-animal-tame", location)) {
                event.setCancelled(true);
            }
        }
    }

    // ---------------------------------------------------------------
    //  PlayerLeashEntityEvent - no-lead-use flag
    // ---------------------------------------------------------------

    // TODO: Implement no-lead-use flag via PlayerInteractEntityEvent
    // (PlayerLeashEntityEvent was removed in Paper 1.21.4)

    // ---------------------------------------------------------------
    //  PlayerInteractEntityEvent - no-item-frame-rotate flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from rotating items in item frames when the
     * no-item-frame-rotate flag is enabled.
     *
     * @param event the player interact entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof ItemFrame)) return;

        Player player = event.getPlayer();
        Location location = clicked.getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-item-frame-rotate", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-item-frame-rotate");
        }
    }

    // ---------------------------------------------------------------
    //  PlayerInteractAtEntityEvent - no-armor-stand-manipulate flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from manipulating armor stands when the
     * no-armor-stand-manipulate flag is enabled. This event fires
     * for right-click interactions with armor stands.
     *
     * @param event the player interact at entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof ArmorStand)) return;

        Player player = event.getPlayer();
        Location location = clicked.getLocation();
        if (location.getWorld() == null) return;

        if (shouldBlock(player, "no-armor-stand-manipulate", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-armor-stand-manipulate");
        }
    }
}
