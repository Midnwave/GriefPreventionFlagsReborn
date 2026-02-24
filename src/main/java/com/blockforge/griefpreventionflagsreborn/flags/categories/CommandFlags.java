package com.blockforge.griefpreventionflagsreborn.flags.categories;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.BooleanFlag;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.flags.ListFlag;
import com.blockforge.griefpreventionflagsreborn.flags.StringFlag;
import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;

/**
 * Registers all command-related flags that control which commands
 * players can execute within claims.
 */
public final class CommandFlags {

    private CommandFlags() {
        // Utility class
    }

    /**
     * Registers all command flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 75. blocked-commands
        registry.registerFlag(new ListFlag(FlagDefinition.builder()
                .id("blocked-commands")
                .displayName("Blocked Commands")
                .description("List of commands that are blocked in this area")
                .type(FlagType.STRING_LIST)
                .category(FlagCategory.COMMANDS)
                .defaultValue(Collections.emptyList())
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.COMMAND_BLOCK)
                .adminOnly(false)
                .build()));

        // 76. allowed-commands
        registry.registerFlag(new ListFlag(FlagDefinition.builder()
                .id("allowed-commands")
                .displayName("Allowed Commands")
                .description("List of commands that are allowed in this area (all others are blocked)")
                .type(FlagType.STRING_LIST)
                .category(FlagCategory.COMMANDS)
                .defaultValue(Collections.emptyList())
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.REPEATING_COMMAND_BLOCK)
                .adminOnly(false)
                .build()));

        // 77. no-command-use
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-command-use")
                .displayName("No Command Use")
                .description("Prevents all command usage in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.COMMANDS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BARRIER)
                .adminOnly(false)
                .build()));

        // 78. blocked-command-message
        registry.registerFlag(new StringFlag(FlagDefinition.builder()
                .id("blocked-command-message")
                .displayName("Blocked Command Message")
                .description("Custom message shown when a command is blocked in this area")
                .type(FlagType.STRING)
                .category(FlagCategory.COMMANDS)
                .defaultValue("")
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_SIGN)
                .adminOnly(false)
                .build()));
    }
}
