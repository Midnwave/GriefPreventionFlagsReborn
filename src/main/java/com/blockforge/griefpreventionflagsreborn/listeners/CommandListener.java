package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import com.blockforge.griefpreventionflagsreborn.util.AdventureCompat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces command-related flags that control which commands players
 * can execute within flagged areas.
 * <p>
 * Handles 4 flags: no-command-use, blocked-commands, allowed-commands,
 * and blocked-command-message.
 */
public final class CommandListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new CommandListener.
     *
     * @param plugin the owning plugin instance
     */
    public CommandListener(GriefPreventionFlagsPlugin plugin) {
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
     * Sends a custom blocked command message if set, otherwise falls back
     * to the default blocked-action message.
     *
     * @param player   the player to notify
     * @param location the location to check for custom message
     */
    private void sendBlockedCommandMessage(Player player, Location location) {
        String key = player.getUniqueId() + ":blocked-commands";
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(key);
        if (last != null && now - last <= 2000) return;
        messageCooldowns.put(key, now);

        String customMessage = flagManager.getFlagValue("blocked-command-message", location);
        if (customMessage != null && !customMessage.isEmpty()) {
            AdventureCompat.sendMessage(player, customMessage);
        } else {
            plugin.getMessagesManager().sendMessage(player, "flag-blocked-action");
        }
    }

    /**
     * Extracts the base command name from a command string.
     * Strips the leading slash and returns only the first word.
     *
     * @param commandMessage the full command string (e.g., "/home set base")
     * @return the base command name (e.g., "home")
     */
    private String extractCommandName(String commandMessage) {
        // Remove leading slash
        String command = commandMessage.startsWith("/") ? commandMessage.substring(1) : commandMessage;
        // Get the first word (the command name)
        int spaceIndex = command.indexOf(' ');
        if (spaceIndex > 0) {
            command = command.substring(0, spaceIndex);
        }
        return command.toLowerCase();
    }

    // ---------------------------------------------------------------
    //  PlayerCommandPreprocessEvent - command flags
    // ---------------------------------------------------------------

    /**
     * Handles command filtering based on no-command-use, blocked-commands,
     * and allowed-commands flags.
     *
     * @param event the player command preprocess event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        if (location == null || location.getWorld() == null) return;

        String commandMessage = event.getMessage();
        String commandName = extractCommandName(commandMessage);

        // Check no-command-use - blocks all commands
        if (shouldBlock(player, "no-command-use", location)) {
            event.setCancelled(true);
            sendBlockedMessage(player, "no-command-use");
            return;
        }

        // Check blocked-commands list
        if (!player.hasPermission("gpfr.bypass.blocked-commands")) {
            List<String> blockedCommands = flagManager.getFlagValue("blocked-commands", location);
            if (blockedCommands != null && !blockedCommands.isEmpty()) {
                for (String blocked : blockedCommands) {
                    if (commandName.equalsIgnoreCase(blocked.trim())) {
                        event.setCancelled(true);
                        sendBlockedCommandMessage(player, location);
                        return;
                    }
                }
            }
        }

        // Check allowed-commands list (whitelist mode)
        if (!player.hasPermission("gpfr.bypass.allowed-commands")) {
            List<String> allowedCommands = flagManager.getFlagValue("allowed-commands", location);
            if (allowedCommands != null && !allowedCommands.isEmpty()) {
                boolean isAllowed = false;
                for (String allowed : allowedCommands) {
                    if (commandName.equalsIgnoreCase(allowed.trim())) {
                        isAllowed = true;
                        break;
                    }
                }
                if (!isAllowed) {
                    event.setCancelled(true);
                    sendBlockedCommandMessage(player, location);
                }
            }
        }
    }
}
