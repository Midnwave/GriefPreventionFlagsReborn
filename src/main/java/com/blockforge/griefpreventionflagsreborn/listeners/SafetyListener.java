package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces safety-related flags that protect players from various damage
 * sources, hunger depletion, and death penalties within flagged areas.
 * <p>
 * Handles 14 flags: no-fall-damage, no-fire-damage, no-drowning,
 * no-explosion-damage, no-void-damage, no-suffocation-damage,
 * no-contact-damage, no-lightning-damage, no-poison-damage,
 * no-wither-effect-damage, no-hunger, keep-inventory, keep-level,
 * and no-player-drops.
 */
public final class SafetyListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new SafetyListener.
     *
     * @param plugin the owning plugin instance
     */
    public SafetyListener(GriefPreventionFlagsPlugin plugin) {
        this.plugin = plugin;
        this.flagManager = plugin.getFlagManager();
        this.gpHook = plugin.getGpHook();
    }

    // ---------------------------------------------------------------
    //  Helper methods
    // ---------------------------------------------------------------

    /**
     * Checks whether the given flag should block the player's action.
     * Returns false if the player has the bypass permission or the flag
     * is not enabled at the given location.
     *
     * @param player  the player to check
     * @param flagId  the flag identifier
     * @param location the location to check
     * @return true if the action should be blocked
     */
    private boolean shouldBlock(Player player, String flagId, Location location) {
        if (player.hasPermission("gpfr.bypass." + flagId)) return false;
        return flagManager.isFlagEnabled(flagId, location);
    }

    /**
     * Sends a rate-limited blocked-action message to the player.
     * Messages are throttled to once every 2 seconds per player per flag
     * to prevent chat spam.
     *
     * @param player the player to notify
     * @param flagId the flag that blocked the action
     */
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
    //  EntityDamageEvent - damage type flags
    // ---------------------------------------------------------------

    /**
     * Listens for all entity damage events and cancels damage to players
     * when the appropriate safety flag is enabled at their location.
     *
     * @param event the entity damage event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) return;

        DamageCause cause = event.getCause();

        switch (cause) {
            case FALL -> {
                if (shouldBlock(player, "no-fall-damage", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-fall-damage");
                }
            }
            case FIRE, FIRE_TICK, LAVA, HOT_FLOOR -> {
                if (shouldBlock(player, "no-fire-damage", location)) {
                    event.setCancelled(true);
                    // Extinguish the player if on fire
                    player.setFireTicks(0);
                    sendBlockedMessage(player, "no-fire-damage");
                }
            }
            case DROWNING -> {
                if (shouldBlock(player, "no-drowning", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-drowning");
                }
            }
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> {
                if (shouldBlock(player, "no-explosion-damage", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-explosion-damage");
                }
            }
            case VOID -> {
                if (shouldBlock(player, "no-void-damage", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-void-damage");
                    // Teleport player to a safe location above the void
                    teleportToSafeLocation(player);
                }
            }
            case SUFFOCATION -> {
                if (shouldBlock(player, "no-suffocation-damage", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-suffocation-damage");
                }
            }
            case CONTACT -> {
                if (shouldBlock(player, "no-contact-damage", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-contact-damage");
                }
            }
            case LIGHTNING -> {
                if (shouldBlock(player, "no-lightning-damage", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-lightning-damage");
                }
            }
            case POISON -> {
                if (shouldBlock(player, "no-poison-damage", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-poison-damage");
                }
            }
            case WITHER -> {
                if (shouldBlock(player, "no-wither-effect-damage", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-wither-effect-damage");
                }
            }
            default -> {
                // No flag covers this damage cause
            }
        }
    }

    // ---------------------------------------------------------------
    //  FoodLevelChangeEvent - no-hunger flag
    // ---------------------------------------------------------------

    /**
     * Prevents hunger depletion when the no-hunger flag is enabled.
     *
     * @param event the food level change event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) return;

        // Only block hunger decrease, not increase (eating)
        if (event.getFoodLevel() < player.getFoodLevel()) {
            if (shouldBlock(player, "no-hunger", location)) {
                event.setCancelled(true);
            }
        }
    }

    // ---------------------------------------------------------------
    //  PlayerDeathEvent - keep-inventory, keep-level, no-player-drops
    // ---------------------------------------------------------------

    /**
     * Handles death-related safety flags: keep-inventory, keep-level,
     * and no-player-drops.
     *
     * @param event the player death event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) return;

        // keep-inventory: player keeps their inventory on death
        if (shouldBlock(player, "keep-inventory", location)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
        }

        // keep-level: player keeps their experience on death
        if (shouldBlock(player, "keep-level", location)) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }

        // no-player-drops: prevent item drops on death (without necessarily keeping them)
        if (shouldBlock(player, "no-player-drops", location)) {
            event.getDrops().clear();
        }
    }

    // ---------------------------------------------------------------
    //  Utility methods
    // ---------------------------------------------------------------

    /**
     * Teleports a player to a safe location above the void.
     * Finds the highest non-air block or uses the world spawn as a fallback.
     *
     * @param player the player to teleport
     */
    private void teleportToSafeLocation(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Try to find the highest block at the player's X/Z
        int highestY = world.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());

        if (highestY > world.getMinHeight()) {
            Location safe = new Location(world, loc.getX(), highestY + 1.0, loc.getZ(),
                    loc.getYaw(), loc.getPitch());
            player.teleport(safe);
        } else {
            // Fallback: teleport to world spawn
            player.teleport(world.getSpawnLocation());
        }
    }
}
