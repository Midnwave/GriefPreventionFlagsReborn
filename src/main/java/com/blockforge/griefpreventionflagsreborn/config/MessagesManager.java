package com.blockforge.griefpreventionflagsreborn.config;

import com.blockforge.griefpreventionflagsreborn.util.AdventureCompat;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Manages the plugin's {@code messages.yml} file, providing methods to retrieve
 * MiniMessage-formatted message strings with placeholder support.
 * <p>
 * Messages support the {@code <prefix>} tag which is automatically replaced with
 * the configured prefix from the messages file. Arbitrary placeholders can be
 * substituted using key-value pairs passed to the {@link #getMessage(String, String...)}
 * method.
 */
public final class MessagesManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private FileConfiguration messagesConfig;

    /**
     * Creates a new MessagesManager.
     *
     * @param plugin the owning plugin instance
     */
    public MessagesManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Loads or reloads the messages.yml file. If the file does not exist in the
     * plugin's data folder, the default messages.yml from the JAR is saved first.
     */
    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from JAR to ensure all keys exist
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            messagesConfig.setDefaults(defaultConfig);
        }

        logger.info("Messages loaded from messages.yml");
    }

    /**
     * Retrieves a raw MiniMessage-formatted message string for the given key.
     * The {@code <prefix>} placeholder is automatically replaced with the
     * configured prefix.
     *
     * @param key the message key (e.g., "flag-set", "no-permission")
     * @return the MiniMessage string with prefix substituted, or the key itself if not found
     */
    @NotNull
    public String getMessage(@NotNull String key) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            logger.warning("Missing message key in messages.yml: " + key);
            return key;
        }
        // Replace <prefix> with the actual prefix value
        String prefix = getPrefix();
        return message.replace("<prefix>", prefix);
    }

    /**
     * Retrieves a MiniMessage-formatted message string with placeholder substitution.
     * Placeholders are provided as alternating key-value pairs, where each key is
     * wrapped in angle brackets for matching.
     * <p>
     * Example usage:
     * <pre>
     *   getMessage("flag-set", "flag", "no-pvp", "value", "true", "scope", "Claim #42")
     * </pre>
     * This replaces {@code <flag>} with "no-pvp", {@code <value>} with "true",
     * and {@code <scope>} with "Claim #42".
     *
     * @param key          the message key
     * @param placeholders alternating key-value pairs (must be even length)
     * @return the formatted MiniMessage string with all placeholders replaced
     */
    @NotNull
    public String getMessage(@NotNull String key, @NotNull String... placeholders) {
        String message = getMessage(key);

        if (placeholders.length % 2 != 0) {
            logger.warning("Odd number of placeholder arguments for message key: " + key +
                           ". Placeholders must be key-value pairs.");
            return message;
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            String placeholder = "<" + placeholders[i] + ">";
            String value = placeholders[i + 1];
            message = message.replace(placeholder, value);
        }

        return message;
    }

    /**
     * Sends a formatted message to a {@link CommandSender} using the MiniMessage
     * compatibility layer. The message is looked up by key and placeholders are
     * substituted before formatting and sending.
     *
     * @param sender       the recipient of the message
     * @param key          the message key
     * @param placeholders alternating key-value pairs for placeholder substitution
     */
    public void sendMessage(@NotNull CommandSender sender, @NotNull String key, @NotNull String... placeholders) {
        String message = getMessage(key, placeholders);
        AdventureCompat.sendMessage(sender, message);
    }

    /**
     * Returns the configured message prefix in MiniMessage format.
     *
     * @return the prefix string, or a default if not configured
     */
    @NotNull
    public String getPrefix() {
        String prefix = messagesConfig.getString("prefix");
        if (prefix == null) {
            return "<gradient:#7B2FF7:#FF5733><bold>GPFR</bold></gradient> <dark_gray>|</dark_gray> ";
        }
        return prefix;
    }
}
