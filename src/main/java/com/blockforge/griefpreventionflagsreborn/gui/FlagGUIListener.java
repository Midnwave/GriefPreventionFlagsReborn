package com.blockforge.griefpreventionflagsreborn.gui;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.hooks.PaperDetector;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Central event listener for all flag GUI interactions.
 * <p>
 * This listener is registered once during plugin startup and handles:
 * <ul>
 *   <li>{@link InventoryClickEvent}: Prevents item theft and delegates clicks to
 *       the appropriate GUI handler ({@link FlagGUI} or {@link FlagDetailGUI}).</li>
 *   <li>{@link InventoryDragEvent}: Prevents item dragging in GUI inventories.</li>
 *   <li>{@link InventoryCloseEvent}: Cleans up state when a GUI is closed.</li>
 *   <li>{@link AsyncPlayerChatEvent}: Intercepts chat messages from players with
 *       pending input requests from {@link FlagDetailGUI}.</li>
 *   <li>{@link PlayerQuitEvent}: Cleans up any pending input state for disconnecting players.</li>
 * </ul>
 * <p>
 * On Paper servers, an additional listener for Paper's {@code AsyncChatEvent} is
 * registered via reflection to avoid compile-time dependencies on the Paper API.
 */
public class FlagGUIListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;

    /**
     * Creates a new FlagGUIListener.
     *
     * @param plugin the plugin instance
     */
    public FlagGUIListener(@NotNull GriefPreventionFlagsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers this listener and, if running on Paper, additionally registers
     * a Paper-specific chat event listener.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // On Paper, also register Paper's AsyncChatEvent listener
        if (PaperDetector.isPaper()) {
            try {
                registerPaperChatListener();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Failed to register Paper AsyncChatEvent listener, " +
                                "falling back to Bukkit AsyncPlayerChatEvent", e);
            }
        }
    }

    /**
     * Handles inventory click events for flag GUIs. Cancels the event to prevent
     * item movement and delegates the click to the appropriate GUI handler.
     *
     * @param event the click event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();

        // Check if the clicked inventory belongs to one of our GUIs
        if (holder instanceof FlagGUI flagGUI) {
            event.setCancelled(true);

            // Only handle clicks within the top inventory
            if (event.getRawSlot() >= event.getInventory().getSize()) {
                return; // Clicked in player's own inventory
            }

            try {
                flagGUI.handleClick(event.getRawSlot(), event);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Error handling FlagGUI click for " + player.getName(), e);
            }
            return;
        }

        if (holder instanceof FlagDetailGUI detailGUI) {
            event.setCancelled(true);

            // Only handle clicks within the top inventory
            if (event.getRawSlot() >= event.getInventory().getSize()) {
                return;
            }

            try {
                detailGUI.handleClick(event.getRawSlot(), event);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Error handling FlagDetailGUI click for " + player.getName(), e);
            }
        }
    }

    /**
     * Prevents item dragging in flag GUI inventories.
     *
     * @param event the drag event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof FlagGUI || holder instanceof FlagDetailGUI) {
            // Cancel if any of the dragged slots are in the top inventory
            int topSize = event.getInventory().getSize();
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Handles inventory close events for flag GUIs. Cleans up any pending input
     * state if the player closes the detail GUI without completing chat input
     * (only if they don't have an active pending input, since closing is expected
     * during the chat prompt flow).
     *
     * @param event the close event
     */
    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();

        // When detail GUI closes, only clean up if NOT transitioning to chat input
        if (holder instanceof FlagDetailGUI) {
            // Don't cancel pending input here - the player may be entering chat input
            // The pending input will be cleaned up by PlayerQuitEvent or consumed by chat handler
        }
    }

    /**
     * Handles Bukkit's {@link AsyncPlayerChatEvent} to intercept chat messages from
     * players with pending flag input. This is used on Spigot servers. On Paper servers,
     * the Paper-specific chat event is preferred but this serves as a fallback.
     *
     * @param event the chat event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(@NotNull AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!FlagDetailGUI.hasPendingInput(playerUUID)) {
            return;
        }

        // Consume the pending input
        FlagDetailGUI.PendingInput pending = FlagDetailGUI.consumePendingInput(playerUUID);
        if (pending == null) {
            return;
        }

        // Cancel the chat event so the input doesn't appear in chat
        event.setCancelled(true);

        String message = event.getMessage().trim();

        // Execute the callback (which schedules a task on the main thread)
        try {
            pending.getCallback().accept(message);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error processing chat input for flag " + pending.getFlagId()
                            + " from player " + player.getName(), e);
            Bukkit.getScheduler().runTask(plugin, () -> {
                com.blockforge.griefpreventionflagsreborn.util.AdventureCompat.sendMessage(player,
                        "<red>An error occurred while processing your input.");
            });
        }
    }

    /**
     * Cleans up pending input state when a player disconnects.
     *
     * @param event the quit event
     */
    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        FlagDetailGUI.cancelPendingInput(event.getPlayer().getUniqueId());
    }

    /**
     * Registers Paper's {@code AsyncChatEvent} listener via reflection to avoid
     * compile-time dependency on Paper API. This is only called when
     * {@link PaperDetector#isPaper()} returns true.
     */
    private void registerPaperChatListener() {
        try {
            // Dynamically register a listener for io.papermc.paper.event.player.AsyncChatEvent
            Class<?> asyncChatEventClass = Class.forName("io.papermc.paper.event.player.AsyncChatEvent");

            // Create a listener that wraps our chat handling logic
            Listener paperChatListener = new PaperChatEventListener(plugin);
            Bukkit.getPluginManager().registerEvents(paperChatListener, plugin);

            plugin.getLogger().info("Registered Paper AsyncChatEvent listener for GUI chat input.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("Paper AsyncChatEvent class not found despite Paper detection. " +
                    "Using Bukkit AsyncPlayerChatEvent fallback.");
        }
    }

    /**
     * Inner listener class that handles Paper's AsyncChatEvent.
     * <p>
     * This is a separate class to isolate Paper API references. It implements
     * Bukkit's {@link Listener} interface and uses Paper's event class directly.
     * The class is only loaded and instantiated when Paper is detected at runtime.
     */
    private static class PaperChatEventListener implements Listener {

        private final GriefPreventionFlagsPlugin plugin;

        PaperChatEventListener(@NotNull GriefPreventionFlagsPlugin plugin) {
            this.plugin = plugin;
        }

        /**
         * Handles Paper's AsyncChatEvent by checking for pending flag input.
         * <p>
         * This method will be compiled with Paper API on the classpath but is
         * only invoked at runtime when Paper is detected. The event handler
         * annotation is processed by Bukkit's event system.
         *
         * @param event the Paper async chat event
         */
        @EventHandler(priority = EventPriority.LOWEST)
        public void onPaperChat(@NotNull io.papermc.paper.event.player.AsyncChatEvent event) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();

            if (!FlagDetailGUI.hasPendingInput(playerUUID)) {
                return;
            }

            FlagDetailGUI.PendingInput pending = FlagDetailGUI.consumePendingInput(playerUUID);
            if (pending == null) {
                return;
            }

            event.setCancelled(true);

            // Extract plain text from the adventure Component message
            String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(event.message()).trim();

            try {
                pending.getCallback().accept(message);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Error processing Paper chat input for flag " + pending.getFlagId()
                                + " from player " + player.getName(), e);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    com.blockforge.griefpreventionflagsreborn.util.AdventureCompat.sendMessage(player,
                            "<red>An error occurred while processing your input.");
                });
            }
        }
    }
}
