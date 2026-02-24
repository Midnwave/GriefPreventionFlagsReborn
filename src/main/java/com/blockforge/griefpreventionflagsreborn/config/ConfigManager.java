package com.blockforge.griefpreventionflagsreborn.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages the plugin's {@code config.yml} file, providing typed accessor methods
 * for all configuration values.
 * <p>
 * Configuration values are read from the Bukkit {@link FileConfiguration} on each
 * getter call, so changes made via {@link #loadConfig()} are immediately reflected.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    /**
     * Creates a new ConfigManager.
     *
     * @param plugin the owning plugin instance
     */
    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads or reloads the config.yml file. If the file does not exist,
     * the default configuration embedded in the plugin JAR is saved first.
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    /**
     * Returns the database file name from the configuration.
     *
     * @return the database file name (default: "data.db")
     */
    @NotNull
    public String getDatabaseFile() {
        return config.getString("database.file", "data.db");
    }

    /**
     * Returns whether the flag value cache is enabled.
     *
     * @return true if caching is enabled (default: true)
     */
    public boolean isCacheEnabled() {
        return config.getBoolean("cache.enabled", true);
    }

    /**
     * Returns the cache time-to-live in seconds.
     *
     * @return the cache TTL in seconds (default: 30)
     */
    public int getCacheTTL() {
        return config.getInt("cache.ttl-seconds", 30);
    }

    /**
     * Returns the maximum number of entries in the flag value cache.
     *
     * @return the cache max size (default: 10000)
     */
    public int getCacheMaxSize() {
        return config.getInt("cache.max-size", 10000);
    }

    /**
     * Returns the MiniMessage-formatted GUI title string.
     *
     * @return the GUI title (default: gradient "Claim Flags")
     */
    @NotNull
    public String getGuiTitle() {
        return config.getString("gui.title", "<gradient:#7B2FF7:#FF5733>Claim Flags</gradient>");
    }

    /**
     * Returns the number of flag entries to display per GUI page.
     *
     * @return flags per page (default: 28)
     */
    public int getFlagsPerPage() {
        return config.getInt("gui.flags-per-page", 28);
    }

    /**
     * Returns whether bStats metrics are enabled.
     *
     * @return true if metrics are enabled (default: true)
     */
    public boolean isMetricsEnabled() {
        return config.getBoolean("metrics", true);
    }

    /**
     * Returns whether debug mode is enabled.
     * When enabled, additional diagnostic information is logged.
     *
     * @return true if debug mode is enabled (default: false)
     */
    public boolean isDebugMode() {
        return config.getBoolean("debug", false);
    }

    /**
     * Returns the server-level default flag values defined in config.yml.
     * <p>
     * The {@code server-defaults} section maps flag IDs to their default values.
     * These are applied at the SERVER scope during plugin startup.
     *
     * @return an unmodifiable map of flag ID to default value string, never null
     */
    @NotNull
    public Map<String, String> getServerDefaults() {
        ConfigurationSection section = config.getConfigurationSection("server-defaults");
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, String> defaults = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null) {
                defaults.put(key, String.valueOf(value));
            }
        }
        return Collections.unmodifiableMap(defaults);
    }
}
