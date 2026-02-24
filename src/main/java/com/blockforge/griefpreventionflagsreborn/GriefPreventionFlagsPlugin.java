package com.blockforge.griefpreventionflagsreborn;

import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.GPFRApi;
import com.blockforge.griefpreventionflagsreborn.commands.ClaimFlagsCommand;
import com.blockforge.griefpreventionflagsreborn.commands.ClaimFlagsTabCompleter;
import com.blockforge.griefpreventionflagsreborn.config.ConfigManager;
import com.blockforge.griefpreventionflagsreborn.config.MessagesManager;
import com.blockforge.griefpreventionflagsreborn.flags.FlagManagerImpl;
import com.blockforge.griefpreventionflagsreborn.flags.FlagRegistryImpl;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.flags.categories.BlockInteractionFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.CommandFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.ContentFlags121;
import com.blockforge.griefpreventionflagsreborn.flags.categories.EconomyFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.EnvironmentFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.MobFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.MovementFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.PvPFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.SafetyFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.TechnicalFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.VehicleEntityFlags;
import com.blockforge.griefpreventionflagsreborn.flags.categories.WorldModificationFlags;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import com.blockforge.griefpreventionflagsreborn.hooks.PlaceholderAPIHook;
import com.blockforge.griefpreventionflagsreborn.listeners.BlockInteractionListener;
import com.blockforge.griefpreventionflagsreborn.listeners.CommandListener;
import com.blockforge.griefpreventionflagsreborn.listeners.ContentListener121;
import com.blockforge.griefpreventionflagsreborn.listeners.EnvironmentListener;
import com.blockforge.griefpreventionflagsreborn.gui.FlagGUIListener;
import com.blockforge.griefpreventionflagsreborn.listeners.MobListener;
import com.blockforge.griefpreventionflagsreborn.listeners.MovementListener;
import com.blockforge.griefpreventionflagsreborn.listeners.PvPListener;
import com.blockforge.griefpreventionflagsreborn.listeners.SafetyListener;
import com.blockforge.griefpreventionflagsreborn.listeners.VehicleEntityListener;
import com.blockforge.griefpreventionflagsreborn.listeners.WorldModificationListener;
import com.blockforge.griefpreventionflagsreborn.schedule.ScheduleManager;
import com.blockforge.griefpreventionflagsreborn.storage.DatabaseManager;
import com.blockforge.griefpreventionflagsreborn.storage.FlagStorageManager;
import com.blockforge.griefpreventionflagsreborn.storage.ScheduleStorageManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;

/**
 * GriefPreventionFlagsReborn - A comprehensive claim flags plugin with 130+ flags
 * and an API for custom flags.
 *
 * @author BlockForge
 */
public class GriefPreventionFlagsPlugin extends JavaPlugin {

    private static GriefPreventionFlagsPlugin instance;

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DatabaseManager databaseManager;
    private FlagStorageManager flagStorageManager;
    private ScheduleStorageManager scheduleStorageManager;
    private GriefPreventionHook gpHook;
    private FlagRegistry flagRegistry;
    private FlagManagerImpl flagManager;
    private ScheduleManager scheduleManager;

    /**
     * Returns the singleton instance of the plugin.
     *
     * @return the plugin instance
     */
    public static GriefPreventionFlagsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        // 1. Load configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 2. Load messages
        messagesManager = new MessagesManager(this);
        messagesManager.loadMessages();

        // 3. Open database
        databaseManager = new DatabaseManager(getDataFolder(), configManager.getDatabaseFile(), getLogger());
        try {
            databaseManager.open();
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to open database! Disabling plugin.", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 4. Initialize storage managers
        flagStorageManager = new FlagStorageManager(databaseManager, getLogger());
        scheduleStorageManager = new ScheduleStorageManager(databaseManager, getLogger());

        // 5. Set up GriefPrevention hook
        gpHook = new GriefPreventionHook(getLogger());
        if (!gpHook.setup()) {
            getLogger().severe("GriefPrevention not found or not loaded! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 6. Create flag registry and flag manager
        flagRegistry = new FlagRegistryImpl(getLogger());
        flagManager = new FlagManagerImpl((FlagRegistryImpl) flagRegistry, flagStorageManager, gpHook, getLogger());

        // 7. Initialize the public API
        GPFRApi.init(flagRegistry, flagManager);

        // 8. Register all built-in flags
        SafetyFlags.registerAll(flagRegistry);
        PvPFlags.registerAll(flagRegistry);
        MobFlags.registerAll(flagRegistry);
        EnvironmentFlags.registerAll(flagRegistry);
        MovementFlags.registerAll(flagRegistry);
        CommandFlags.registerAll(flagRegistry);
        EconomyFlags.registerAll(flagRegistry);
        TechnicalFlags.registerAll(flagRegistry);
        VehicleEntityFlags.registerAll(flagRegistry);
        BlockInteractionFlags.registerAll(flagRegistry);
        WorldModificationFlags.registerAll(flagRegistry);
        ContentFlags121.registerAll(flagRegistry);

        // 9. Load server defaults from config
        Map<String, String> serverDefaults = configManager.getServerDefaults();
        for (Map.Entry<String, String> entry : serverDefaults.entrySet()) {
            try {
                flagManager.setFlag(entry.getKey(), FlagScope.SERVER, "server", entry.getValue(), null);
            } catch (Exception e) {
                getLogger().warning("Failed to set server default for flag '" + entry.getKey() + "': " + e.getMessage());
            }
        }

        // 10. Register command executor and tab completer
        PluginCommand claimFlagsCmd = getCommand("claimflags");
        if (claimFlagsCmd != null) {
            ClaimFlagsCommand commandExecutor = new ClaimFlagsCommand(this);
            claimFlagsCmd.setExecutor(commandExecutor);
            claimFlagsCmd.setTabCompleter(new ClaimFlagsTabCompleter(this));
        }

        // 11. Register enforcement listeners
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new SafetyListener(this), this);
        pm.registerEvents(new PvPListener(this), this);
        pm.registerEvents(new MobListener(this), this);
        pm.registerEvents(new EnvironmentListener(this), this);
        pm.registerEvents(new MovementListener(this), this);
        pm.registerEvents(new CommandListener(this), this);
        pm.registerEvents(new BlockInteractionListener(this), this);
        pm.registerEvents(new WorldModificationListener(this), this);
        pm.registerEvents(new VehicleEntityListener(this), this);
        pm.registerEvents(new ContentListener121(this), this);

        // 12. Register GUI listener
        pm.registerEvents(new FlagGUIListener(this), this);

        // 13. Initialize schedule manager
        scheduleManager = new ScheduleManager(this);
        scheduleManager.start();

        // 14. Hook into PlaceholderAPI if present
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(
                    getDescription().getVersion(),
                    gpHook,
                    flagStorageManager
            ).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }

        // 15. Initialize bStats metrics
        if (configManager.isMetricsEnabled()) {
            new Metrics(this, 00000);
        }

        // 16. Log startup
        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info("GriefPreventionFlagsReborn v" + getDescription().getVersion() + " enabled with "
                + flagRegistry.getFlagCount() + " flags registered (" + elapsed + "ms)");
    }

    @Override
    public void onDisable() {
        // 1. Close database
        if (databaseManager != null) {
            databaseManager.close();
        }

        // 2. Shutdown the public API
        GPFRApi.shutdown();

        // 3. Cancel scheduled tasks
        if (scheduleManager != null) {
            scheduleManager.stop();
        }
        Bukkit.getScheduler().cancelTasks(this);

        // 4. Clear caches
        if (flagManager != null) {
            flagManager.clearCache();
        }

        // 5. Log shutdown
        getLogger().info("GriefPreventionFlagsReborn disabled.");

        instance = null;
    }

    /**
     * Returns the configuration manager.
     *
     * @return the config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Returns the messages manager.
     *
     * @return the messages manager
     */
    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    /**
     * Returns the database manager.
     *
     * @return the database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Returns the flag registry containing all registered flag definitions.
     *
     * @return the flag registry
     */
    public FlagRegistry getFlagRegistry() {
        return flagRegistry;
    }

    /**
     * Returns the flag manager for reading and writing flag values.
     *
     * @return the flag manager
     */
    public FlagManager getFlagManager() {
        return flagManager;
    }

    /**
     * Returns the GriefPrevention integration hook.
     *
     * @return the GP hook
     */
    public GriefPreventionHook getGpHook() {
        return gpHook;
    }

    /**
     * Returns the flag storage manager for direct database operations on flag values.
     *
     * @return the flag storage manager
     */
    public FlagStorageManager getFlagStorageManager() {
        return flagStorageManager;
    }

    /**
     * Returns the schedule storage manager for direct database operations on schedules.
     *
     * @return the schedule storage manager
     */
    public ScheduleStorageManager getScheduleStorageManager() {
        return scheduleStorageManager;
    }
}
