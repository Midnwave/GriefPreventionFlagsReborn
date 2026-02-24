package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import com.blockforge.griefpreventionflagsreborn.util.AdventureCompat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces movement-related flags that control player movement,
 * teleportation, entry/exit behavior, messages, and speed within
 * flagged areas.
 * <p>
 * Handles 12 flags: no-enter, no-exit, entry-message, exit-message,
 * command-on-entry, command-on-exit, walk-speed, no-flight, no-elytra,
 * no-enderpearl, no-chorus-fruit, and no-riptide.
 */
public final class MovementListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new MovementListener.
     *
     * @param plugin the owning plugin instance
     */
    public MovementListener(GriefPreventionFlagsPlugin plugin) {
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
     * Sends a rate-limited custom message to the player.
     *
     * @param player  the player to notify
     * @param flagId  the flag for cooldown tracking
     * @param message the MiniMessage formatted message to send
     */
    private void sendCustomMessage(Player player, String flagId, String message) {
        String key = player.getUniqueId() + ":" + flagId;
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(key);
        if (last == null || now - last > 2000) {
            messageCooldowns.put(key, now);
            AdventureCompat.sendMessage(player, message);
        }
    }

    /**
     * Checks whether two locations are in different block positions
     * (ignoring head rotation changes).
     *
     * @param from the origin location
     * @param to   the destination location
     * @return true if the block coordinates differ
     */
    private boolean hasBlockChanged(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    /**
     * Gets the claim ID at a location, or null if not in a claim.
     *
     * @param location the location to check
     * @return the claim ID string, or null
     */
    private String getClaimIdAt(Location location) {
        return gpHook.getClaimId(location);
    }

    // ---------------------------------------------------------------
    //  PlayerMoveEvent - entry/exit, messages, commands, walk speed
    // ---------------------------------------------------------------

    /**
     * Handles player movement across claim boundaries, enforcing
     * no-enter, no-exit, entry/exit messages, entry/exit commands,
     * and walk speed modifications.
     *
     * @param event the player move event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Only process when the player actually changes block position
        if (!hasBlockChanged(from, to)) return;

        Player player = event.getPlayer();
        if (from.getWorld() == null || to.getWorld() == null) return;

        String fromClaimId = getClaimIdAt(from);
        String toClaimId = getClaimIdAt(to);

        // Determine if the player is crossing a claim boundary
        boolean crossingBoundary = !java.util.Objects.equals(fromClaimId, toClaimId);

        // ---- no-enter: prevent entering a claim ----
        if (toClaimId != null) {
            if (shouldBlock(player, "no-enter", to)) {
                // Only block if actually entering the claim (crossing boundary into it)
                if (crossingBoundary) {
                    event.setTo(from.clone());
                    sendBlockedMessage(player, "no-enter");
                    return;
                }
            }
        }

        // ---- no-exit: prevent leaving a claim ----
        if (fromClaimId != null) {
            if (shouldBlock(player, "no-exit", from)) {
                // Only block if actually leaving the claim
                if (crossingBoundary) {
                    event.setTo(from.clone());
                    sendBlockedMessage(player, "no-exit");
                    return;
                }
            }
        }

        // ---- Boundary crossing actions (messages and commands) ----
        if (crossingBoundary) {
            // exit-message: show when leaving a claim
            if (fromClaimId != null) {
                String exitMessage = flagManager.getFlagValue("exit-message", from);
                if (exitMessage != null && !exitMessage.isEmpty()) {
                    sendCustomMessage(player, "exit-message", exitMessage);
                }
            }

            // command-on-exit: execute when leaving a claim
            if (fromClaimId != null) {
                String exitCommand = flagManager.getFlagValue("command-on-exit", from);
                if (exitCommand != null && !exitCommand.isEmpty()) {
                    String command = exitCommand.replace("<player>", player.getName());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.performCommand(command));
                }
            }

            // entry-message: show when entering a claim
            if (toClaimId != null) {
                String entryMessage = flagManager.getFlagValue("entry-message", to);
                if (entryMessage != null && !entryMessage.isEmpty()) {
                    sendCustomMessage(player, "entry-message", entryMessage);
                }
            }

            // command-on-entry: execute when entering a claim
            if (toClaimId != null) {
                String entryCommand = flagManager.getFlagValue("command-on-entry", to);
                if (entryCommand != null && !entryCommand.isEmpty()) {
                    String command = entryCommand.replace("<player>", player.getName());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.performCommand(command));
                }
            }
        }

        // ---- walk-speed: modify player walk speed ----
        Object walkSpeedValue = flagManager.getFlagValue("walk-speed", to);
        if (walkSpeedValue instanceof Number walkSpeed) {
            float speed = walkSpeed.floatValue();
            if (speed >= 0.0f && speed <= 1.0f) {
                player.setWalkSpeed(speed);
            } else if (speed < 0) {
                // Reset to default Minecraft walk speed (0.2)
                player.setWalkSpeed(0.2f);
            }
        }
    }

    // ---------------------------------------------------------------
    //  PlayerToggleFlightEvent - no-flight flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from toggling flight when the no-flight flag
     * is enabled at their location.
     *
     * @param event the player toggle flight event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) return; // Only block enabling flight

        Player player = event.getPlayer();
        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) return;

        if (shouldBlock(player, "no-flight", location)) {
            event.setCancelled(true);
            player.setFlying(false);
            sendBlockedMessage(player, "no-flight");
        }
    }

    // ---------------------------------------------------------------
    //  EntityToggleGlideEvent - no-elytra flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from using elytra gliding when the no-elytra
     * flag is enabled at their location.
     *
     * @param event the entity toggle glide event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.isGliding()) return; // Only block enabling glide

        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) return;

        if (shouldBlock(player, "no-elytra", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-elytra");
        }
    }

    // ---------------------------------------------------------------
    //  PlayerTeleportEvent - ender pearl / chorus fruit flags
    // ---------------------------------------------------------------

    /**
     * Prevents ender pearl and chorus fruit teleportation when the
     * appropriate flags are enabled.
     *
     * @param event the player teleport event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();

        // Check no-enderpearl at the origin location
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            if (shouldBlock(player, "no-enderpearl", from)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-enderpearl");
                return;
            }
            // Also check at destination
            if (to.getWorld() != null && shouldBlock(player, "no-enderpearl", to)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-enderpearl");
                return;
            }
        }

        // Check no-chorus-fruit at the origin location
        if (cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            if (shouldBlock(player, "no-chorus-fruit", from)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-chorus-fruit");
                return;
            }
            if (to.getWorld() != null && shouldBlock(player, "no-chorus-fruit", to)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-chorus-fruit");
            }
        }
    }

    // ---------------------------------------------------------------
    //  PlayerRiptideEvent - no-riptide flag
    // ---------------------------------------------------------------

    /**
     * Prevents riptide trident use when the no-riptide flag is enabled.
     * Note: PlayerRiptideEvent is not cancellable in all Bukkit versions,
     * so we counteract it by teleporting the player back.
     *
     * @param event the player riptide event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) return;

        if (shouldBlock(player, "no-riptide", location)) {
            // PlayerRiptideEvent is not cancellable, so teleport back
            final Location savedLoc = location.clone();
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(savedLoc);
                player.setVelocity(player.getVelocity().multiply(0));
            });
            sendBlockedMessage(player, "no-riptide");
        }
    }
}
