package com.blockforge.griefpreventionflagsreborn.util;

import com.blockforge.griefpreventionflagsreborn.hooks.PaperDetector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Compatibility layer for sending MiniMessage-formatted text to players
 * on both Paper and Spigot servers.
 * <p>
 * On Paper servers, messages are sent using the native Adventure API that Paper
 * exposes directly on {@link CommandSender}. On Spigot servers, the shaded
 * Adventure library is used to parse MiniMessage, then the resulting
 * {@link Component} is converted to legacy color codes via
 * {@link LegacyComponentSerializer} and sent using {@code sendMessage(String)}.
 */
public final class AdventureCompat {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private AdventureCompat() {
        // Utility class - no instantiation
    }

    /**
     * Sends a MiniMessage-formatted message to a {@link CommandSender}.
     * <p>
     * On Paper, the native Adventure API is used. On Spigot, the message is
     * parsed via the shaded MiniMessage library and converted to legacy format.
     *
     * @param sender      the recipient of the message
     * @param miniMessage the MiniMessage-formatted string
     */
    public static void sendMessage(@NotNull CommandSender sender, @NotNull String miniMessage) {
        Component component = deserialize(miniMessage);

        if (PaperDetector.isPaper()) {
            sendPaperMessage(sender, component);
        } else {
            sendLegacyMessage(sender, component);
        }
    }

    /**
     * Sends a MiniMessage-formatted message with tag resolvers to a {@link CommandSender}.
     * <p>
     * Tag resolvers allow dynamic placeholder replacement within MiniMessage strings
     * (e.g., {@code <player>} or {@code <value>}).
     *
     * @param sender      the recipient of the message
     * @param miniMessage the MiniMessage-formatted string
     * @param resolvers   the tag resolvers for placeholder replacement
     */
    public static void sendMessage(@NotNull CommandSender sender, @NotNull String miniMessage,
                                   @NotNull TagResolver... resolvers) {
        Component component = deserialize(miniMessage, resolvers);

        if (PaperDetector.isPaper()) {
            sendPaperMessage(sender, component);
        } else {
            sendLegacyMessage(sender, component);
        }
    }

    /**
     * Deserializes a MiniMessage-formatted string into an Adventure {@link Component}.
     *
     * @param miniMessage the MiniMessage-formatted string
     * @return the deserialized component
     */
    @NotNull
    public static Component deserialize(@NotNull String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    /**
     * Deserializes a MiniMessage-formatted string with tag resolvers into an
     * Adventure {@link Component}.
     *
     * @param miniMessage the MiniMessage-formatted string
     * @param resolvers   the tag resolvers for placeholder replacement
     * @return the deserialized component
     */
    @NotNull
    public static Component deserialize(@NotNull String miniMessage, @NotNull TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(miniMessage, resolvers);
    }

    /**
     * Converts an Adventure {@link Component} to a legacy color-coded string
     * using section sign ({@code &}) formatting with hex color support.
     *
     * @param component the component to serialize
     * @return the legacy-formatted string
     */
    @NotNull
    public static String toLegacy(@NotNull Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Sends a message using Paper's native Adventure API.
     * Paper's CommandSender natively accepts Adventure Components.
     *
     * @param sender    the recipient
     * @param component the component to send
     */
    private static void sendPaperMessage(@NotNull CommandSender sender, @NotNull Component component) {
        sender.sendMessage(component);
    }

    /**
     * Sends a message by converting the Adventure Component to a legacy string.
     * Used on Spigot servers that do not have native Adventure support.
     *
     * @param sender    the recipient
     * @param component the component to convert and send
     */
    @SuppressWarnings("deprecation")
    private static void sendLegacyMessage(@NotNull CommandSender sender, @NotNull Component component) {
        String legacy = toLegacy(component);
        sender.sendMessage(legacy);
    }
}
